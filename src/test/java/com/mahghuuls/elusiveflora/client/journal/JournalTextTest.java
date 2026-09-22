package com.mahghuuls.elusiveflora.client.journal;

import com.mahghuuls.elusiveflora.roster.Condition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        assertEquals("Open all year, day and night.", JournalText.condition(Condition.ALWAYS));
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
    void shortListsAreSpelledOutInFull() {
        assertEquals("Found in: Forest, Plains.",
                JournalText.biomes(Arrays.asList("Forest", "Plains"), true, true));
        assertEquals("Found in: The End.", JournalText.biomes(Collections.singletonList("The End"), true, true));
    }

    @Test
    void longListsShowTwelveAndACount() {
        List<String> names = new ArrayList<String>();
        for (int i = 1; i <= 15; i++) {
            names.add("Biome " + String.format("%02d", i));
        }
        String text = JournalText.biomes(names, true, true);
        assertTrue(text.startsWith("Found in: Biome 01, Biome 02, "), text);
        assertTrue(text.contains("Biome 12, and 3 more."), text);
        assertFalse(text.contains("Biome 13"), text);
        List<String> twelve = names.subList(0, 12);
        assertFalse(JournalText.biomes(twelve, true, true).contains("more"), "exactly twelve needs no count");
    }

    @Test
    void anEmptyListSaysSoAndBlamesAMissingModOnlyWhenNothingCouldMatch() {
        assertEquals("No installed biome grows it.", JournalText.biomes(Collections.<String>emptyList(), true, true));
        assertTrue(JournalText.biomes(Collections.<String>emptyList(), true, false).contains("mod that is not installed"));
        assertTrue(JournalText.biomes(Collections.<String>emptyList(), false, false).contains("not known yet"));
    }
}
