package com.mahghuuls.elusiveflora.client.journal;

import com.mahghuuls.elusiveflora.roster.Condition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalTextTest {

    @Test
    void everyConditionHasItsOwnPlainSentence() {
        List<String> seen = new ArrayList<String>();
        for (Condition condition : Condition.values()) {
            String text = JournalText.condition(condition);
            assertTrue(text.startsWith("Open "), condition + ": " + text);
            assertTrue(text.endsWith("."), condition + ": " + text);
            assertFalse(seen.contains(text), condition + " repeats another condition's sentence");
            seen.add(text);
        }
        assertEquals("Open all year.", JournalText.condition(Condition.ALWAYS));
        assertTrue(JournalText.condition(Condition.FULL_MOON).contains("full moon"));
    }

    @Test
    void runTogetherVanillaNamesGetSpaces() {
        assertEquals("Forest Hills", JournalText.readable("ForestHills"));
        assertEquals("Mushroom Island", JournalText.readable("MushroomIsland"));
        assertEquals("The End", JournalText.readable("The End"));
        assertEquals("Plains", JournalText.readable("Plains"));
        assertEquals("Hell", JournalText.readable("Hell"));
    }

    @Test
    void shortListsShowOneNamePerLine() {
        assertEquals("Forest$(br)Plains", JournalText.biomes(Arrays.asList("Forest", "Plains"), true, true));
        assertEquals("The End", JournalText.biomes(Collections.singletonList("The End"), true, true));
        String text = JournalText.biomes(numbered(14, "Biome "), true, true);
        assertEquals(13, text.split(Pattern.quote("$(br)")).length - 1, text);
        assertFalse(text.contains("Found in"), text);
    }

    @Test
    void listsThatDoNotFitOnePerLineArePackedAndStillFitThePage() {
        String fifteen = JournalText.biomes(numbered(15, "Biome "), true, true);
        assertTrue(fifteen.startsWith("Found in: Biome 01, Biome 02, "), fifteen);
        assertTrue(fifteen.endsWith("Biome 15."), fifteen);
        assertFalse(fifteen.contains("more"), "fifteen short names fit when packed");
        assertTrue(JournalText.lineCount(fifteen) <= JournalText.LINES_PER_PAGE, fifteen);

        String forty = JournalText.biomes(numbered(40, "Mutated Taiga Hills "), true, true);
        assertTrue(forty.matches("Found in: .*, and [0-9]+ more[.]"), forty);
        assertTrue(JournalText.lineCount(forty) <= JournalText.LINES_PER_PAGE, forty);
        assertTrue(forty.contains("Mutated Taiga Hills 05"), "the packed list holds a fair number: " + forty);
    }

    @Test
    void theLineModelWrapsAtSpacesAndCountsBreaks() {
        assertEquals(1, JournalText.lineCount("Plains"));
        assertEquals(2, JournalText.lineCount("Plains$(br)Forest"));
        assertEquals(2, JournalText.lineCount("Mutated Redwood Taiga Hills"));
        assertEquals(2, JournalText.lineCount("aaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    private static List<String> numbered(int count, String prefix) {
        List<String> names = new ArrayList<String>();
        for (int i = 1; i <= count; i++) {
            names.add(prefix + String.format("%02d", i));
        }
        return names;
    }

    @Test
    void anEmptyListSaysSoAndBlamesAMissingModOnlyWhenNothingCouldMatch() {
        assertEquals("No installed biome grows it.", JournalText.biomes(Collections.<String>emptyList(), true, true));
        assertTrue(JournalText.biomes(Collections.<String>emptyList(), true, false).contains("mod that is not installed"));
        assertTrue(JournalText.biomes(Collections.<String>emptyList(), false, false).contains("not known yet"));
    }
}
