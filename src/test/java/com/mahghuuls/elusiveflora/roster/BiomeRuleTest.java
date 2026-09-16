package com.mahghuuls.elusiveflora.roster;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeRuleTest {

    private static Set<String> types(String... names) {
        return new HashSet<String>(Arrays.asList(names));
    }

    @Test
    void typeMatchIsAnyOf() {
        BiomeRule rule = BiomeRule.parse("duskwisp", "FOREST|SPOOKY", "");
        assertTrue(rule.matches("minecraft:forest", types("FOREST", "COOL")));
        assertTrue(rule.matches("spookybiomes:ghostly_forest", types("SPOOKY", "DEAD")));
        assertFalse(rule.matches("minecraft:plains", types("PLAINS")));
    }

    @Test
    void nameMatchIsExact() {
        BiomeRule rule = BiomeRule.parse("starpetal", "",
                "betterendforge:glowing_grasslands|stygian:lunar_meadow");
        assertTrue(rule.matches("stygian:lunar_meadow", types("END", "MAGICAL")));
        assertFalse(rule.matches("minecraft:sky", types("END")));
        assertFalse(rule.matches("stygian:lunar_meadows", types("END")));
    }

    @Test
    void typesAndNamesAreEither() {
        BiomeRule rule = BiomeRule.parse("x", "OCEAN", "minecraft:river");
        assertTrue(rule.matches("minecraft:river", types("RIVER")));
        assertTrue(rule.matches("biomesoplenty:coral_reef", types("OCEAN", "WATER")));
        assertFalse(rule.matches("minecraft:plains", types("PLAINS")));
    }

    @Test
    void starMatchesEverything() {
        BiomeRule rule = BiomeRule.parse("emberroot", "*", "");
        assertTrue(rule.matchesAnyBiome());
        assertTrue(rule.matches("minecraft:hell", types("NETHER")));
        assertTrue(rule.matches("anything:at_all", Collections.<String>emptySet()));
    }

    @Test
    void emptyRuleMatchesNothing() {
        BiomeRule rule = BiomeRule.parse("x", "", "");
        assertTrue(rule.isEmpty());
        assertFalse(rule.matches("minecraft:plains", types("PLAINS")));
    }

    @Test
    void biomeNameWithoutNamespaceFails() {
        assertThrows(RosterException.class, () -> BiomeRule.parse("x", "", "plains"));
    }

    @Test
    void emptyListEntriesFail() {
        assertThrows(RosterException.class, () -> BiomeRule.parse("x", "FOREST|", ""));
        assertThrows(RosterException.class, () -> BiomeRule.parse("x", "", "minecraft:plains||minecraft:forest"));
    }
}
