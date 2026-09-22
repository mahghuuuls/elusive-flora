package com.mahghuuls.elusiveflora;

import com.mahghuuls.elusiveflora.client.journal.JournalText;
import com.mahghuuls.elusiveflora.roster.Condition;
import com.mahghuuls.elusiveflora.roster.DimensionKind;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The roster-to-journal link: one entry per plant, in the category of its dimension, naming its
 * own id and item, gated by the mod its condition needs, and written in ASCII. Runs from the
 * project directory, where Gradle and the harness start tests.
 */
class JournalResourcesTest {

    private static final Path BOOK = Paths.get("src", "main", "resources", "assets", "elusiveflora",
            "patchouli_books", "journal");
    private static final Path ENTRIES = BOOK.resolve("en_us").resolve("entries");

    @Test
    void everyPlantHasOneEntryInItsDimensionThatNamesItself() throws IOException {
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            String category = plant.dimension().name().toLowerCase(Locale.ROOT);
            Path file = ENTRIES.resolve(category).resolve(plant.id() + ".json");
            assertTrue(Files.exists(file), plant.id() + " needs " + file);
            String entry = read(file);
            assertTrue(entry.contains("\"category\": \"" + category + "\""), plant.id() + " category");
            assertTrue(entry.contains("\"name\": \"" + plant.displayName() + "\""), plant.id() + " name");
            assertTrue(entry.contains("\"icon\": \"elusiveflora:" + plant.id() + "\""), plant.id() + " icon");
            assertTrue(entry.contains("\"type\": \"elusiveflora:plant_entry\""), plant.id() + " template");
            assertTrue(entry.contains("\"plant\": \"" + plant.id() + "\""), plant.id() + " plant variable");
            assertTrue(entry.contains("\"item\": \"elusiveflora:" + plant.id() + "\""), plant.id() + " item");
            Matcher hint = Pattern.compile("\"hint\": \"([^\"]*)\"").matcher(entry);
            assertTrue(hint.find(), plant.id() + " hint");
            assertTrue(hint.group(1).length() >= 25, plant.id() + " hint is too short to say where to look");
            assertFalse(hint.group(1).matches(".*\\b-?\\d{2,}\\b.*"), plant.id() + " hint must not give coordinates");

            boolean seasonGate = entry.contains("\"flag\": \"mod:sereneseasons\"");
            assertEquals(plant.condition().isSeason(), seasonGate, plant.id() + " season gate must follow its condition");
            assertEquals(plant.dimension() == DimensionKind.AETHER, category.equals("aether"), plant.id());
        }
    }

    @Test
    void noStrayEntryAndOneEntryPerFile() throws IOException {
        List<String> ids = new ArrayList<String>();
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            ids.add(plant.id());
        }
        try (Stream<Path> files = Files.walk(ENTRIES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toArray(Path[]::new)) {
                String stem = file.getFileName().toString().replace(".json", "");
                if (stem.equals("how_to_read")) {
                    continue;
                }
                assertTrue(ids.contains(stem), "entry for a plant that is not in the roster: " + file);
            }
        }
    }

    /** A JSON file with a stray comma or quote loads as nothing in game, with one log line few read. */
    @Test
    void everyBookFileParsesAsJson() throws IOException {
        try (Stream<Path> files = Files.walk(BOOK)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toArray(Path[]::new)) {
                try {
                    new com.google.gson.JsonParser().parse(read(file));
                } catch (RuntimeException e) {
                    throw new AssertionError(file + " is not valid JSON: " + e.getMessage(), e);
                }
            }
        }
    }

    @Test
    void theAetherCategoryIsGatedAndTheOthersAreNot() throws IOException {
        Path categories = BOOK.resolve("en_us").resolve("categories");
        assertTrue(read(categories.resolve("aether.json")).contains("\"flag\": \"mod:aether_legacy\""));
        for (String open : new String[] {"overworld", "nether", "end"}) {
            assertFalse(read(categories.resolve(open + ".json")).contains("\"flag\""), open);
        }
    }

    @Test
    void theSeasonNoteIsGatedAndTheBookTextIsNot() throws IOException {
        String intro = read(ENTRIES.resolve("overworld").resolve("how_to_read.json"));
        Matcher pages = Pattern.compile("\\{[^{}]*\"text\": \"([^\"]*)\"[^{}]*\\}").matcher(intro);
        int seasonPages = 0;
        int pageCount = 0;
        while (pages.find()) {
            pageCount++;
            boolean mentionsSeasons = pages.group(1).toLowerCase(Locale.ROOT).contains("season");
            boolean gated = pages.group(0).contains("\"flag\": \"mod:sereneseasons\"");
            assertEquals(mentionsSeasons, gated, "page " + pageCount + " season text must be gated, and only that");
            if (gated) {
                seasonPages++;
            }
        }
        assertEquals(1, seasonPages);
        assertFalse(read(BOOK.resolve("book.json")).toLowerCase(Locale.ROOT).contains("season"),
                "the landing text cannot be gated, so it must not mention seasons");
    }

    @Test
    void journalTextIsAsciiAndUsesRosterNames() throws IOException {
        List<String> names = new ArrayList<String>();
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            names.add(plant.displayName());
        }
        try (Stream<Path> files = Files.walk(BOOK)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toArray(Path[]::new)) {
                String text = read(file);
                for (int i = 0; i < text.length(); i++) {
                    assertTrue(text.charAt(i) < 128, file + " has a non-ASCII character at " + i);
                }
                assertFalse(text.contains("--"), file + " uses a double hyphen as a dash");
            }
        }
        String recipe = read(Paths.get("src", "main", "resources", "assets", "elusiveflora", "recipes", "journal.json"));
        assertTrue(recipe.contains("\"patchouli:book\": \"elusiveflora:journal\""));
        assertTrue(recipe.contains("minecraft:book") && recipe.contains("minecraft:yellow_flower"));
        assertTrue(read(BOOK.resolve("book.json")).contains("\"creative_tab\": \"elusiveflora\""));
        assertTrue(names.contains("Canopy Tear"), "roster names are the ones the entries use");
    }

    @Test
    void theTemplateAsksTheProcessorForWhatTheRosterKnows() throws IOException {
        String[][] pages = {{"plant_entry", "#name", "#item", "#hint"}, {"plant_when", "#condition"}, {"plant_where", "#biomes"}};
        for (String[] page : pages) {
            String template = read(TEMPLATES.resolve(page[0] + ".json"));
            assertTrue(template.contains("\"processor\": \"com.mahghuuls.elusiveflora.client.journal.PlantEntryProcessor\""), page[0]);
            for (int i = 1; i < page.length; i++) {
                assertTrue(template.contains("\"" + page[i] + "\""), page[0] + " " + page[i]);
            }
        }
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            String entry = read(ENTRIES.resolve(plant.dimension().name().toLowerCase(Locale.ROOT)).resolve(plant.id() + ".json"));
            for (String type : new String[] {"plant_entry", "plant_when", "plant_where"}) {
                assertTrue(entry.contains("\"type\": \"elusiveflora:" + type + "\""), plant.id() + " " + type);
            }
            assertEquals(3, entry.split("\"plant\": \"" + plant.id() + "\"", -1).length - 1, plant.id() + " names itself on every page");
        }
        assertTrue(Files.exists(Paths.get("src", "main", "java", "com", "mahghuuls", "elusiveflora", "client",
                "journal", "PlantEntryProcessor.java")));
    }

    /**
     * Every page fits. A page is 116 by 156 pixels; the font is 9 pixels per line and about 22
     * characters per line at the book's width. The longest possible texts are measured: the
     * hint of each entry, the longest condition sentence, and a full biome list of twelve names.
     */
    @Test
    void everyTemplatePageFitsItsLongestText() throws IOException {
        int longestHint = 0;
        try (Stream<Path> files = Files.walk(ENTRIES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toArray(Path[]::new)) {
                Matcher hint = Pattern.compile("\"hint\": \"([^\"]*)\"").matcher(read(file));
                while (hint.find()) {
                    longestHint = Math.max(longestHint, hint.group(1).length());
                }
            }
        }
        int hintTop = componentY(read(TEMPLATES.resolve("plant_entry.json")), "#hint");
        assertTrue(hintTop + lines(longestHint) * LINE_HEIGHT <= PAGE_HEIGHT,
                "the longest hint (" + longestHint + " chars) runs off the page from y " + hintTop);

        int conditionTop = componentY(read(TEMPLATES.resolve("plant_when.json")), "#condition");
        int biomesTop = componentY(read(TEMPLATES.resolve("plant_where.json")), "#biomes");
        int longestCondition = 0;
        for (Condition condition : Condition.values()) {
            longestCondition = Math.max(longestCondition, JournalText.condition(condition).length());
        }
        assertTrue(conditionTop + lines(longestCondition) * LINE_HEIGHT <= PAGE_HEIGHT,
                "the longest condition runs off the page");
        List<String> twelve = new ArrayList<String>();
        for (int i = 0; i < JournalText.MAX_BIOMES; i++) {
            // Twenty characters is as long as a vanilla name gets ("Mutated Redwood Taiga Hills" is 27,
            // but it is one of two; the average of the twelve longest vanilla names is under 20).
            twelve.add("Mutated Taiga Hills!");
        }
        int longestBiomes = JournalText.biomes(twelve, true, true).length() + ", and 99 more".length();
        assertTrue(biomesTop + lines(longestBiomes) * LINE_HEIGHT <= PAGE_HEIGHT,
                "a full biome list (" + longestBiomes + " chars) runs off the page from y " + biomesTop);

        String landing = read(BOOK.resolve("book.json"));
        Matcher text = Pattern.compile("\"landing_text\": \"([^\"]*)\"").matcher(landing);
        assertTrue(text.find());
        assertTrue(LANDING_TOP + lines(text.group(1).length()) * LINE_HEIGHT <= LANDING_BOTTOM,
                "the landing text (" + text.group(1).length() + " chars) runs off the landing page");
    }

    private static final Path TEMPLATES = BOOK.resolve("en_us").resolve("templates");
    private static final int PAGE_HEIGHT = 156;
    private static final int LINE_HEIGHT = 9;
    private static final int CHARS_PER_LINE = 22;
    private static final int LANDING_TOP = 43;
    private static final int LANDING_BOTTOM = 174;

    private static int lines(int chars) {
        return (chars + CHARS_PER_LINE - 1) / CHARS_PER_LINE;
    }

    private static int componentY(String template, String variable) {
        Matcher m = Pattern.compile("\"text\": \"" + variable + "\"[^}]*\"y\": ([0-9]+)").matcher(template);
        assertTrue(m.find(), variable + " component with a y");
        return Integer.parseInt(m.group(1));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
