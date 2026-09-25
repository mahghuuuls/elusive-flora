package com.mahghuuls.elusiveflora.config;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.roster.BiomeRule;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The pack maker's overrides, read once at startup from {@code config/elusiveflora.cfg}. The one
 * owner of precedence: a valid config value wins, otherwise the roster default; an out-of-range
 * number is clamped with one warning; an unresolvable biome name is reported once elsewhere.
 * Every option needs a restart, and its comment says so.
 */
public final class ElusiveFloraConfig {

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_PLANTS = "plants";

    private static final String RESTART = " Requires a game restart.";

    private final int regrowBaseMinutes;
    private final boolean enableCheckCommand;
    private final boolean debugLog;
    private final Map<String, PlantSettings> plants;

    private ElusiveFloraConfig(int regrowBaseMinutes, boolean enableCheckCommand, boolean debugLog,
                               Map<String, PlantSettings> plants) {
        this.regrowBaseMinutes = regrowBaseMinutes;
        this.enableCheckCommand = enableCheckCommand;
        this.debugLog = debugLog;
        this.plants = Collections.unmodifiableMap(plants);
    }

    /** Reads or creates the file, writing roster defaults and comments for every option. */
    public static ElusiveFloraConfig load(File file, PlantRoster roster) {
        Configuration config = new Configuration(file);
        config.load();

        config.setCategoryComment(CATEGORY_GENERAL, "Settings that apply to every plant.");
        int baseMinutes = clampInt(readInt(config, CATEGORY_GENERAL, "regrowBaseMinutes", 180,
                "Base time in real minutes for a picked plant to grow back (1 minute = 1200 ticks of world time)."
                        + " Each plant multiplies it by its regrowFactor. Range 1 to 100000." + RESTART),
                "regrowBaseMinutes", 1, 100000);
        boolean checkCommand = readBoolean(config, CATEGORY_GENERAL, "enableCheckCommand", false,
                "Registers the operator command /elusiveflora, which reports which plants can appear where you stand."
                        + " Off by default; it is a pack-maker tool, not gameplay." + RESTART);
        boolean debug = readBoolean(config, CATEGORY_GENERAL, "debugLog", false,
                "Logs one line for every plant world generation places, with its id and position."
                        + " Off by default; noisy in a fresh world." + RESTART);

        config.setCategoryComment(CATEGORY_PLANTS,
                "One section per plant. Biome types are Forge biome dictionary names such as FOREST or SNOWY;"
                        + " * means every biome. Biome names are registry names such as minecraft:plains."
                        + " A plant is eligible in a biome that carries any listed type or whose name is listed.");
        Map<String, PlantSettings> plants = new LinkedHashMap<String, PlantSettings>();
        for (PlantDefinition plant : roster.plants()) {
            String category = CATEGORY_PLANTS + "." + plant.id();
            config.setCategoryComment(category, plant.displayName() + ": " + situationText(plant) + ".");
            boolean enabled = readBoolean(config, category, "enabled", true,
                    "false removes " + plant.displayName() + " from world generation. Existing plants stay." + RESTART);
            int chance = clampInt(readInt(config, category, "chunkChancePercent", plant.chunkChancePercent(),
                    "Percent chance, rolled once per new chunk, that the game tries to place one "
                            + plant.displayName() + ". Example: 10 means about one attempt in ten chunks. Range 0 to 100."
                            + RESTART),
                    category + ".chunkChancePercent", 0, 100);
            String[] types = readList(config, category, "biomeTypes",
                    plant.biomeRule().typeNames().toArray(new String[0]),
                    "Biome dictionary types where " + plant.displayName() + " may appear, one per line. Example: FOREST."
                            + RESTART);
            String[] names = readList(config, category, "biomeNames",
                    plant.biomeRule().biomeNames().toArray(new String[0]),
                    "Biome registry names where " + plant.displayName() + " may appear, one per line."
                            + " Example: minecraft:plains." + RESTART);
            double factor = clampDouble(readDouble(config, category, "regrowFactor", plant.regrowFactor(),
                    "Multiplier of regrowBaseMinutes for " + plant.displayName() + ". 0.5 halves the time, 2.0 doubles it."
                            + " Range 0.1 to 10." + RESTART),
                    category + ".regrowFactor", 0.1, 10.0);
            plants.put(plant.id(), new PlantSettings(enabled, chance, new BiomeRule(toSet(types), toSet(names)), factor));
        }

        if (config.hasChanged()) {
            config.save();
        }
        return new ElusiveFloraConfig(baseMinutes, checkCommand, debug, plants);
    }

    /** Base regrow time in real minutes; every plant multiplies it by its factor. */
    public int regrowBaseMinutes() {
        return regrowBaseMinutes;
    }

    public boolean enableCheckCommand() {
        return enableCheckCommand;
    }

    public boolean debugLog() {
        return debugLog;
    }

    /** The effective settings for a plant id; every roster plant has an entry. */
    public PlantSettings plant(String id) {
        return plants.get(id);
    }

    // Reading. Forge's typed get overloads repair a malformed or out-of-range value in place and
    // save the repaired file, which hides the pack maker's typo. These go through the untyped
    // overload, which only records the default and the comment, and judge the raw value
    // themselves so a bad value warns once and stays in the file for the pack maker to see.
    // The value readers take a Property so they can be tested without a Configuration, which
    // cannot be constructed outside a running game.

    private static double readDouble(Configuration config, String category, String key, double fallback, String comment) {
        Property property = config.get(category, key, String.valueOf(fallback), comment, Property.Type.DOUBLE);
        property.setRequiresMcRestart(true);
        return doubleOf(property, category + "." + key, fallback);
    }

    private static int readInt(Configuration config, String category, String key, int fallback, String comment) {
        Property property = config.get(category, key, String.valueOf(fallback), comment, Property.Type.INTEGER);
        property.setRequiresMcRestart(true);
        return intOf(property, category + "." + key, fallback);
    }

    private static boolean readBoolean(Configuration config, String category, String key, boolean fallback, String comment) {
        Property property = config.get(category, key, String.valueOf(fallback), comment, Property.Type.BOOLEAN);
        property.setRequiresMcRestart(true);
        return booleanOf(property, category + "." + key, fallback);
    }

    private static String[] readList(Configuration config, String category, String key, String[] fallback, String comment) {
        Property property = config.get(category, key, fallback, comment);
        property.setRequiresMcRestart(true);
        return property.getStringList();
    }

    static double doubleOf(Property property, String key, double fallback) {
        if (!property.isDoubleValue()) {
            ElusiveFloraLog.warnOnce("config." + key,
                    "Config " + key + " is not a number ('" + property.getString() + "'); using " + fallback);
            return fallback;
        }
        return property.getDouble();
    }

    static int intOf(Property property, String key, int fallback) {
        if (!property.isIntValue()) {
            ElusiveFloraLog.warnOnce("config." + key,
                    "Config " + key + " is not a whole number ('" + property.getString() + "'); using " + fallback);
            return fallback;
        }
        return property.getInt();
    }

    static boolean booleanOf(Property property, String key, boolean fallback) {
        if (!property.isBooleanValue()) {
            ElusiveFloraLog.warnOnce("config." + key,
                    "Config " + key + " is not true or false ('" + property.getString() + "'); using " + fallback);
            return fallback;
        }
        return property.getBoolean();
    }

    /** Clamps into range and warns once when a value had to move. Package-private for tests. */
    static int clampInt(int value, String key, int min, int max) {
        if (value < min || value > max) {
            int clamped = Math.max(min, Math.min(max, value));
            ElusiveFloraLog.warnOnce("config.clamp." + key,
                    "Config " + key + " is " + value + ", outside " + min + " to " + max + "; using " + clamped);
            return clamped;
        }
        return value;
    }

    static double clampDouble(double value, String key, double min, double max) {
        if (value < min || value > max) {
            double clamped = Math.max(min, Math.min(max, value));
            ElusiveFloraLog.warnOnce("config.clamp." + key,
                    "Config " + key + " is " + value + ", outside " + min + " to " + max + "; using " + clamped);
            return clamped;
        }
        return value;
    }

    /** The roster's situation text without its design-note rule number, e.g. "(8)". */
    static String situationText(PlantDefinition plant) {
        return plant.situation().replaceAll("\\s*\\([^)]*\\)\\s*$", "");
    }

    private static Set<String> toSet(String[] values) {
        Set<String> set = new LinkedHashSet<String>();
        for (String value : values) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }
}
