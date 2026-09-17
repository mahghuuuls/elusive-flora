package com.mahghuuls.elusiveflora.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The reason order is a contract the check tool prints; a reordering is a behavior change. */
class PlacementCheckTest {

    /** Rules that answer from fixed booleans and record which questions were asked. */
    private static final class FixedRules implements PlacementCheck.Rules {
        private final boolean[] answers;
        final List<String> asked = new ArrayList<String>();

        FixedRules(boolean enabled, boolean dimension, boolean biome, boolean ground, boolean condition) {
            answers = new boolean[] {enabled, dimension, biome, ground, condition};
        }

        public boolean enabled() { asked.add("enabled"); return answers[0]; }
        public boolean dimension() { asked.add("dimension"); return answers[1]; }
        public boolean biome() { asked.add("biome"); return answers[2]; }
        public boolean ground() { asked.add("ground"); return answers[3]; }
        public boolean condition() { asked.add("condition"); return answers[4]; }
    }

    @Test
    void everyRulePassingIsNull() {
        FixedRules rules = new FixedRules(true, true, true, true, true);
        assertNull(PlacementCheck.firstFailure(rules, true));
        assertEquals(Arrays.asList("enabled", "dimension", "biome", "ground", "condition"), rules.asked);
    }

    @Test
    void firstFailureWinsInOrderAndLaterRulesAreNotAsked() {
        assertEquals(PlacementCheck.Reason.DISABLED,
                PlacementCheck.firstFailure(new FixedRules(false, false, false, false, false), true));
        FixedRules dim = new FixedRules(true, false, false, false, false);
        assertEquals(PlacementCheck.Reason.DIMENSION, PlacementCheck.firstFailure(dim, true));
        assertEquals(Arrays.asList("enabled", "dimension"), dim.asked);
        FixedRules biome = new FixedRules(true, true, false, true, true);
        assertEquals(PlacementCheck.Reason.BIOME, PlacementCheck.firstFailure(biome, true));
        assertFalse(biome.asked.contains("ground"));
        assertEquals(PlacementCheck.Reason.GROUND,
                PlacementCheck.firstFailure(new FixedRules(true, true, true, false, true), true));
        assertEquals(PlacementCheck.Reason.CONDITION,
                PlacementCheck.firstFailure(new FixedRules(true, true, true, true, false), true));
    }

    @Test
    void generatorModeStopsBeforeCondition() {
        FixedRules rules = new FixedRules(true, true, true, true, false);
        assertNull(PlacementCheck.firstFailure(rules, false));
        assertFalse(rules.asked.contains("condition"));
    }

    @Test
    void reasonTextIsPlainAscii() {
        for (PlacementCheck.Reason reason : PlacementCheck.Reason.values()) {
            String text = reason.text();
            assertFalse(text.isEmpty());
            for (char c : text.toCharArray()) {
                assertEquals(true, c < 128, reason + " has a non-ASCII character");
            }
        }
    }
}
