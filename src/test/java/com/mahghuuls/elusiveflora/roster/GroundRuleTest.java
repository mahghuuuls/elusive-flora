package com.mahghuuls.elusiveflora.roster;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundRuleTest {

    @Test
    void groundClauseWithMinY() {
        GroundRule rule = GroundRule.parse("skyclover", "on:grass|dirt;min_y:110");
        assertEquals(PlacementKind.GROUND, rule.kind());
        assertEquals(Arrays.asList("grass", "dirt"), rule.groundKeywords());
        assertEquals(110, rule.minY());
        assertNull(rule.sideKind());
        assertFalse(rule.nearLava());
    }

    @Test
    void groundClauseWithModifiers() {
        GroundRule rule = GroundRule.parse("tideheart", "on:sand;sea_level;near_water");
        assertTrue(rule.seaLevel());
        assertTrue(rule.nearWater());
        assertFalse(rule.nearLava());
        assertEquals(GroundRule.NO_MIN_Y, rule.minY());
    }

    @Test
    void registryNameKeywordIsAccepted() {
        GroundRule rule = GroundRule.parse("cloudfern", "on:aether_legacy:aether_grass");
        assertEquals(Arrays.asList("aether_legacy:aether_grass"), rule.groundKeywords());
    }

    @Test
    void anySolidIsAccepted() {
        GroundRule rule = GroundRule.parse("voidbloom", "on:any_solid");
        assertEquals(Arrays.asList(GroundRule.ANY_SOLID), rule.groundKeywords());
    }

    @Test
    void sideClauses() {
        assertEquals(GroundRule.SideKind.STONE, GroundRule.parse("a", "side:stone").sideKind());
        assertEquals(GroundRule.SideKind.LOG, GroundRule.parse("b", "side:log").sideKind());
        assertEquals(PlacementKind.ATTACHED, GroundRule.parse("b", "side:log").kind());
        assertThrows(RosterException.class, () -> GroundRule.parse("c", "side:ice"));
    }

    @Test
    void waterBedRanges() {
        GroundRule deep = GroundRule.parse("pearlfrond", "water_bed:8+");
        assertEquals(PlacementKind.WATER, deep.kind());
        assertEquals(8, deep.minDepth());
        assertEquals(GroundRule.UNBOUNDED, deep.maxDepth());
        assertTrue(deep.acceptsDepth(8));
        assertTrue(deep.acceptsDepth(40));
        assertFalse(deep.acceptsDepth(7));

        GroundRule shallow = GroundRule.parse("streamreed", "water_bed:1-4");
        assertEquals(1, shallow.minDepth());
        assertEquals(4, shallow.maxDepth());
        assertTrue(shallow.acceptsDepth(1));
        assertTrue(shallow.acceptsDepth(4));
        assertFalse(shallow.acceptsDepth(5));
        assertFalse(shallow.acceptsDepth(0));
    }

    @Test
    void badWaterBedRangesFail() {
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "water_bed:4-1"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "water_bed:0-3"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "water_bed:0+"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "water_bed:deep"));
    }

    @Test
    void straySeparatorsFail() {
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:grass;"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:grass|"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:|grass"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:aether_legacy:"));
    }

    @Test
    void unknownKeywordAndClauseFail() {
        RosterException keyword = assertThrows(RosterException.class,
                () -> GroundRule.parse("frostbell", "on:marble"));
        assertTrue(keyword.getMessage().contains("marble"));
        assertThrows(RosterException.class, () -> GroundRule.parse("frostbell", "on:grass;floating"));
    }

    @Test
    void missingOrDoublePlacementFails() {
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "near_lava"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:grass;side:stone"));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", ""));
        assertThrows(RosterException.class, () -> GroundRule.parse("x", "on:"));
    }
}
