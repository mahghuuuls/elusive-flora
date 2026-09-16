package com.mahghuuls.elusiveflora.roster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bundled plant roster: the single source of per-plant data, read once from
 * {@code assets/elusiveflora/plants.csv}. The file is a byte-identical copy of the design roster;
 * nothing in Java repeats a number from it.
 *
 * <p>The CSV is simple on purpose: comma-separated, a header row, no quoted fields, lists inside
 * a cell separated by {@code |} and clauses by {@code ;}. Any defect fails loading with the row
 * and column named.
 */
public final class PlantRoster {

    /** The UTF-8 byte order mark some editors prepend; stripped from the header if present. */
    private static final char BYTE_ORDER_MARK = (char) 0xFEFF;

    /** Classpath location of the bundled roster. */
    public static final String RESOURCE = "/assets/elusiveflora/plants.csv";

    private static final String[] COLUMNS = {
            "id", "display_name", "status", "dimension", "situation_rule", "biome_types",
            "biome_names", "ground", "condition", "rarity", "chunk_chance_percent",
            "regrow_game_days", "yield_per_pick", "model", "glow_light", "showcase"};

    private final List<PlantDefinition> plants;
    private final Map<String, PlantDefinition> byId;

    private PlantRoster(List<PlantDefinition> plants) {
        this.plants = Collections.unmodifiableList(new ArrayList<PlantDefinition>(plants));
        Map<String, PlantDefinition> map = new LinkedHashMap<String, PlantDefinition>();
        for (PlantDefinition plant : plants) {
            map.put(plant.id(), plant);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    /** Loads the bundled roster from the classpath. */
    public static PlantRoster load() {
        InputStream in = PlantRoster.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new RosterException("resource " + RESOURCE + " is missing from the jar");
        }
        try {
            return load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Closing a classpath stream cannot change what was already parsed.
            }
        }
    }

    /** Parses roster text from any reader. Used by tests and by {@link #load()}. */
    public static PlantRoster load(Reader reader) {
        List<String> lines = readLines(reader);
        if (lines.isEmpty()) {
            throw new RosterException("no header row");
        }
        Map<String, Integer> columns = parseHeader(lines.get(0));
        List<PlantDefinition> plants = new ArrayList<PlantDefinition>();
        Set<String> seenIds = new HashSet<String>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            PlantDefinition plant = parseRow(line, columns, i + 1);
            if (!seenIds.add(plant.id())) {
                throw new RosterException(plant.id(), "id", "duplicate id");
            }
            plants.add(plant);
        }
        if (plants.isEmpty()) {
            throw new RosterException("no plant rows");
        }
        return new PlantRoster(plants);
    }

    private static List<String> readLines(Reader reader) {
        List<String> lines = new ArrayList<String>();
        BufferedReader buffered = new BufferedReader(reader);
        try {
            String line;
            while ((line = buffered.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RosterException("cannot read: " + e.getMessage());
        }
        // A UTF-8 byte order mark, if an editor added one, would otherwise hide inside "id".
        if (!lines.isEmpty() && lines.get(0).startsWith(String.valueOf(BYTE_ORDER_MARK))) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    private static Map<String, Integer> parseHeader(String header) {
        String[] names = header.split(",", -1);
        if (!Arrays.equals(names, COLUMNS)) {
            throw new RosterException("header must be exactly " + Arrays.toString(COLUMNS)
                    + " but was " + Arrays.toString(names));
        }
        Map<String, Integer> columns = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < names.length; i++) {
            columns.put(names[i], i);
        }
        return columns;
    }

    private static PlantDefinition parseRow(String line, Map<String, Integer> columns, int lineNumber) {
        String[] cells = line.split(",", -1);
        if (cells.length != COLUMNS.length) {
            throw new RosterException("line " + lineNumber + " has " + cells.length
                    + " cells, expected " + COLUMNS.length + " (a comma inside a cell?)");
        }
        Row row = new Row(cells, columns, lineNumber);
        String id = row.id();
        String displayName = row.text("display_name");
        if (displayName.isEmpty()) {
            throw new RosterException(row.where(), "display_name", "empty");
        }
        DimensionKind dimension = DimensionKind.parse(id, row.text("dimension"));
        String situation = row.text("situation_rule");
        BiomeRule biomeRule = BiomeRule.parse(id, row.text("biome_types"), row.text("biome_names"));
        if (biomeRule.isEmpty()) {
            throw new RosterException(row.where(), "biome_types", "no biome types and no biome names");
        }
        GroundRule groundRule = GroundRule.parse(id, row.text("ground"));
        Condition condition = Condition.parse(id, row.text("condition"));
        String rarity = row.text("rarity");
        // Only approved rows ship. A draft row left in the file is a mistake, not a hidden plant.
        row.choice("status", "approved", "approved");
        int chunkChance = row.intValue("chunk_chance_percent", 0, 100);
        int regrowDays = row.intValue("regrow_game_days", 1, Integer.MAX_VALUE / (int) Condition.DAY_TICKS);
        // 64 is a stack; a pick never yields more than one stack.
        int yield = row.intValue("yield_per_pick", 1, 64);
        boolean threeD = row.choice("model", "3d", "flat");
        int glow = row.intValue("glow_light", 0, 15);
        boolean showcase = row.choice("showcase", "yes", "no");
        return new PlantDefinition(id, displayName, dimension, situation, biomeRule, groundRule,
                condition, rarity, chunkChance, regrowDays * Condition.DAY_TICKS, yield, threeD,
                glow, showcase);
    }

    /** One CSV row with typed cell access; every failure names the row and column. */
    private static final class Row {
        private final String[] cells;
        private final Map<String, Integer> columns;
        private final int lineNumber;
        private final String id;

        Row(String[] cells, Map<String, Integer> columns, int lineNumber) {
            this.cells = cells;
            this.columns = columns;
            this.lineNumber = lineNumber;
            this.id = text("id");
            if (!id.matches("[a-z][a-z0-9_]*")) {
                throw new RosterException(where(), "id",
                        "must be lower case letters, digits, and underscores");
            }
        }

        String id() {
            return id;
        }

        /** The row as an error message names it: its id when it has one, else its line number. */
        String where() {
            return id == null || id.isEmpty() ? "line " + lineNumber : id;
        }

        String text(String column) {
            return cells[columns.get(column)].trim();
        }

        int intValue(String column, int min, int max) {
            String value = text(column);
            int number;
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new RosterException(where(), column, "needs a whole number, got '" + value + "'");
            }
            if (number < min || number > max) {
                throw new RosterException(where(), column, number + " is outside " + min + " to " + max);
            }
            return number;
        }

        /** Returns true for {@code trueWord}, false for {@code falseWord}, and fails otherwise. */
        boolean choice(String column, String trueWord, String falseWord) {
            String value = text(column);
            if (value.equals(trueWord)) {
                return true;
            }
            if (value.equals(falseWord)) {
                return false;
            }
            throw new RosterException(where(), column, "must be '" + trueWord + "'"
                    + (trueWord.equals(falseWord) ? "" : " or '" + falseWord + "'")
                    + ", got '" + value + "'");
        }
    }

    /** Every plant, in roster order. */
    public List<PlantDefinition> plants() {
        return plants;
    }

    /** The plant with this id, or null. */
    public PlantDefinition byId(String id) {
        return byId.get(id);
    }

    public int size() {
        return plants.size();
    }
}
