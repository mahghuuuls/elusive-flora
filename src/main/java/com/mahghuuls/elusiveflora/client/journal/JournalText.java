package com.mahghuuls.elusiveflora.client.journal;

import com.mahghuuls.elusiveflora.roster.Condition;

import java.util.ArrayList;
import java.util.List;

/**
 * The sentences a journal page builds from plant data: the condition in plain words and the list
 * of installed biomes. Plain Java so the wording is unit-tested; the Patchouli processor only
 * gathers the inputs.
 */
public final class JournalText {

    /** Biome names shown before the list is cut short with a count. */
    public static final int MAX_BIOMES = 12;

    private JournalText() {
    }

    /**
     * A biome name as the game holds it, made readable: vanilla names run words together
     * ("ForestHills", "MushroomIsland"), so a space goes before each capital that follows a
     * lower-case letter. Names that already have spaces, as most modded ones do, are unchanged.
     */
    public static String readable(String biomeName) {
        StringBuilder text = new StringBuilder(biomeName.length() + 4);
        for (int i = 0; i < biomeName.length(); i++) {
            char c = biomeName.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(biomeName.charAt(i - 1))) {
                text.append(' ');
            }
            text.append(c);
        }
        return text.toString();
    }

    /** What a player must wait for, in the words of a field note. */
    public static String condition(Condition condition) {
        switch (condition) {
            case NIGHT:
                return "Open at night. Closed by day.";
            case RAIN:
                return "Open while it rains. Closed in clear weather.";
            case FULL_MOON:
                return "Open on the night of a full moon. Closed on every other night and by day.";
            case NEW_MOON:
                return "Open on the night of a new moon. Closed on every other night and by day.";
            case SPRING:
                return "Open in spring. Closed for the rest of the year.";
            case SUMMER:
                return "Open in summer. Closed for the rest of the year.";
            case AUTUMN:
                return "Open in autumn. Closed for the rest of the year.";
            case WINTER:
                return "Open in winter. Closed for the rest of the year.";
            default:
                return "Open all year, day and night.";
        }
    }

    /**
     * The biome sentence for a list of installed biome names that already match the plant's
     * effective rules, sorted by the caller. Says so plainly when nothing installed matches, and
     * why, when the rule names only biomes from a mod that is absent ({@code anyInstalled} false).
     * A rule that is not resolved yet (before the game finished loading) gets a holding sentence.
     */
    public static String biomes(List<String> names, boolean resolved, boolean anyInstalled) {
        if (!resolved) {
            return "Biomes are not known yet. Open the journal again once the world has loaded.";
        }
        if (names.isEmpty()) {
            return anyInstalled
                    ? "No installed biome grows it."
                    : "No installed biome grows it. The biomes it needs come from a mod that is not installed.";
        }
        List<String> shown = new ArrayList<String>(names.subList(0, Math.min(MAX_BIOMES, names.size())));
        StringBuilder text = new StringBuilder("Found in: ");
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(shown.get(i));
        }
        int more = names.size() - shown.size();
        if (more > 0) {
            text.append(", and ").append(more).append(" more");
        }
        return text.append('.').toString();
    }
}
