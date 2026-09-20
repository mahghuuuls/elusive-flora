package com.mahghuuls.elusiveflora.roster;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the shipped roster and the parser's failure paths. The shipped-roster assertions are
 * the test REQ-001 asks for: a changed row count or id set fails here before any build.
 */
class PlantRosterTest {

    private static final List<String> EXPECTED_IDS = Arrays.asList(
            "ledgebloom", "pearlfrond", "streamreed", "tideheart", "skyclover", "canopytear",
            "frostbell", "sporecap", "emberroot", "duskwisp", "rainthistle", "thawbud", "sunspire",
            "amberleaf", "winterthorn", "moonveil", "umbrabud", "soulwick", "ashenlotus", "voidbloom",
            "starpetal", "cloudfern");

    private static final String HEADER = "id,display_name,status,dimension,situation_rule,biome_types,"
            + "biome_names,ground,condition,rarity,chunk_chance_percent,regrow_game_days,yield_per_pick,"
            + "model,glow_light,showcase\n";

    private static final String GOOD_ROW =
            "frostbell,Frostbell,approved,overworld,snow (8),SNOWY,,on:snow|grass|dirt,always,uncommon,10,2,1,flat,0,no\n";

    @Test
    void shippedRosterHasTheTwentyTwoApprovedPlantsInOrder() {
        PlantRoster roster = PlantRoster.load();
        assertEquals(22, roster.size());
        for (int i = 0; i < EXPECTED_IDS.size(); i++) {
            assertEquals(EXPECTED_IDS.get(i), roster.plants().get(i).id(), "row " + i);
        }
    }

    @Test
    void shippedRosterValuesReadThrough() {
        PlantRoster roster = PlantRoster.load();
        PlantDefinition duskwisp = roster.byId("duskwisp");
        assertNotNull(duskwisp);
        assertEquals("Duskwisp", duskwisp.displayName());
        assertEquals(DimensionKind.OVERWORLD, duskwisp.dimension());
        assertEquals(Condition.NIGHT, duskwisp.condition());
        assertEquals(9, duskwisp.glowLight());
        assertEquals(10, duskwisp.chunkChancePercent());
        assertEquals(2 * Condition.DAY_TICKS, duskwisp.regrowTicks());
        assertEquals(PlacementKind.GROUND, duskwisp.placementKind());
        assertTrue(duskwisp.hasDormantStage());
        assertTrue(duskwisp.biomeRule().typeNames().contains("FOREST"));
        assertTrue(duskwisp.biomeRule().typeNames().contains("SPOOKY"));

        PlantDefinition ledgebloom = roster.byId("ledgebloom");
        assertEquals(PlacementKind.ATTACHED, ledgebloom.placementKind());
        assertEquals(GroundRule.SideKind.STONE, ledgebloom.groundRule().sideKind());
        assertTrue(ledgebloom.isThreeD());
        assertTrue(ledgebloom.isShowcase());

        PlantDefinition pearlfrond = roster.byId("pearlfrond");
        assertEquals(PlacementKind.WATER, pearlfrond.placementKind());
        assertEquals(8, pearlfrond.groundRule().minDepth());
        assertEquals(GroundRule.UNBOUNDED, pearlfrond.groundRule().maxDepth());
        assertEquals(10, pearlfrond.glowLight());

        PlantDefinition emberroot = roster.byId("emberroot");
        assertTrue(emberroot.biomeRule().matchesAnyBiome());
        assertTrue(emberroot.groundRule().nearLava());

        PlantDefinition cloudfern = roster.byId("cloudfern");
        assertEquals(DimensionKind.AETHER, cloudfern.dimension());
        assertEquals(Arrays.asList("aether_legacy:aether_grass"), cloudfern.groundRule().groundKeywords());
        assertTrue(cloudfern.biomeRule().biomeNames().contains("aether_legacy:aether_highlands"));
        assertFalse(cloudfern.hasDormantStage());

        PlantDefinition starpetal = roster.byId("starpetal");
        assertEquals(4, starpetal.biomeRule().biomeNames().size());
        assertTrue(starpetal.biomeRule().typeNames().isEmpty());
    }

    @Test
    void unknownGroundKeywordNamesTheRowAndColumn() {
        String row = GOOD_ROW.replace("on:snow|grass|dirt", "on:snow|marble");
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("row 'frostbell'"), e.getMessage());
        assertTrue(e.getMessage().contains("column 'ground'"), e.getMessage());
        assertTrue(e.getMessage().contains("marble"), e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "',10,2,1,', ',10,0,1,', regrow_game_days",
            "',10,2,1,', ',10,2,0,', yield_per_pick",
            "',10,2,1,', ',10,2,65,', yield_per_pick",
            "'flat,0,no', 'flat,16,no', glow_light",
            "'flat,0,no', '2d,0,no', model",
            "'flat,0,no', 'flat,0,maybe', showcase",
            "',approved,', ',draft,', status",
            "'always,uncommon', 'always,', rarity_is_free_text_so_this_is_the_control"})
    void badCellFailsNamingTheColumn(String from, String to, String column) {
        String row = GOOD_ROW.replace(from, to);
        if (column.startsWith("rarity")) {
            // Control case: an empty rarity word is allowed, proving the harness replaces text.
            assertEquals(1, PlantRoster.load(new StringReader(HEADER + row)).size());
            return;
        }
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("column '" + column + "'"), e.getMessage());
        assertTrue(e.getMessage().contains("row 'frostbell'"), e.getMessage());
    }

    @Test
    void rowWithEmptyIdIsNamedByLineNumber() {
        String row = GOOD_ROW.replace("frostbell,Frostbell", ",Frostbell");
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("row 'line 2'"), e.getMessage());
        assertTrue(e.getMessage().contains("column 'id'"), e.getMessage());
    }

    @Test
    void duplicateIdFails() {
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + GOOD_ROW + GOOD_ROW)));
        assertTrue(e.getMessage().contains("duplicate"), e.getMessage());
    }

    @Test
    void chunkChanceOutOfRangeFails() {
        String row = GOOD_ROW.replace(",10,2,1,", ",101,2,1,");
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("chunk_chance_percent"), e.getMessage());
    }

    @Test
    void unknownConditionFails() {
        String row = GOOD_ROW.replace(",always,", ",dusk,");
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("condition"), e.getMessage());
    }

    @Test
    void wrongHeaderFails() {
        String badHeader = HEADER.replace("glow_light", "glow");
        assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(badHeader + GOOD_ROW)));
    }

    @Test
    void rowWithNoBiomeRuleFails() {
        String row = GOOD_ROW.replace(",SNOWY,,", ",,,");
        RosterException e = assertThrows(RosterException.class,
                () -> PlantRoster.load(new StringReader(HEADER + row)));
        assertTrue(e.getMessage().contains("biome"), e.getMessage());
    }

    @Test
    void byteOrderMarkOnTheHeaderIsTolerated() {
        PlantRoster roster = PlantRoster.load(new StringReader(String.valueOf((char) 0xFEFF) + HEADER + GOOD_ROW));
        assertEquals(1, roster.size());
    }
}
