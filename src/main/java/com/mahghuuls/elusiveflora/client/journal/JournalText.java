package com.mahghuuls.elusiveflora.client.journal;

import com.mahghuuls.elusiveflora.roster.Condition;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The sentences a journal page builds from plant data: the condition in plain words and the list
 * of installed biomes. Plain Java so the wording is unit-tested; the Patchouli processor only
 * gathers the inputs.
 */
public final class JournalText {

    /** Text lines a "Where it grows" page holds under its header (156 px page, 22 px header, 9 px lines). */
    public static final int LINES_PER_PAGE = 14;

    /** Characters that fit on one 116 px journal line when every glyph is 6 px wide, with a margin. */
    public static final int CHARS_PER_LINE = 19;

    /** The Patchouli line break macro. */
    public static final String BREAK = "$(br)";

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

    /**
     * How long a picked plant takes to come back, in hours and minutes of real time, as the
     * journal says it: "Grows back in 1 hour 30 minutes after a pick.".
     */
    public static String regrow(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        StringBuilder text = new StringBuilder("Grows back in ");
        if (hours > 0) {
            text.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (rest > 0 || hours == 0) {
            if (hours > 0) {
                text.append(' ');
            }
            text.append(rest).append(rest == 1 ? " minute" : " minutes");
        }
        return text.append(" after a pick.").toString();
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
                return "Open all year.";
        }
    }

    /**
     * The biome text for a list of installed biome names that already match the plant's
     * effective rules, sorted by the caller. One name per line while they all fit on the page;
     * otherwise a packed comma list that stops before the page ends and says how many are left.
     * Says so plainly when nothing installed matches, and why, when the rule names only biomes
     * from a mod that is absent ({@code anyInstalled} false). A rule that is not resolved yet
     * (before the game finished loading) gets a holding sentence.
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
        int linesOnePerName = 0;
        for (String name : names) {
            linesOnePerName += lineCount(name);
        }
        if (linesOnePerName <= LINES_PER_PAGE) {
            return String.join(BREAK, names);
        }
        // Packed: keep adding names while the text plus the closing count still fits the page.
        String text = "Found in: " + names.get(0);
        int shown = 1;
        while (shown < names.size()) {
            String longer = text + ", " + names.get(shown);
            String closing = shown + 1 < names.size() ? ", and " + (names.size() - shown - 1) + " more." : ".";
            if (lineCount(longer + closing) > LINES_PER_PAGE) {
                break;
            }
            text = longer;
            shown++;
        }
        int more = names.size() - shown;
        return more > 0 ? text + ", and " + more + " more." : text + ".";
    }

    /**
     * Lines the journal needs for a text, in the font model of this class: words wrap at spaces,
     * a word longer than a line is cut. Explicit breaks count as new lines.
     */
    public static int lineCount(String text) {
        int lines = 0;
        for (String paragraph : text.split(Pattern.quote(BREAK), -1)) {
            lines++;
            int used = 0;
            for (String word : paragraph.split(" ")) {
                int width = word.length();
                if (used > 0 && used + 1 + width > CHARS_PER_LINE) {
                    lines++;
                    used = 0;
                }
                while (width > CHARS_PER_LINE) {
                    lines++;
                    width -= CHARS_PER_LINE;
                }
                used += (used > 0 ? 1 : 0) + width;
            }
        }
        return lines;
    }
}
