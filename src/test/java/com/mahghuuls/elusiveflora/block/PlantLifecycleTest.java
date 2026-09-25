package com.mahghuuls.elusiveflora.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The stage rule as a pure function: what the block should show given clock and condition. */
class PlantLifecycleTest {

    @Test
    void stemStaysUntilItsDeadline() {
        assertEquals(PlantStage.STEM, PlantLifecycle.decide(PlantStage.STEM, 1000, 999, true));
        assertEquals(PlantStage.STEM, PlantLifecycle.decide(PlantStage.STEM, 1000, 0, false));
    }

    @Test
    void stemRegrowsIntoTheConditionStageAtTheDeadline() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.STEM, 1000, 1000, true));
        assertEquals(PlantStage.DORMANT, PlantLifecycle.decide(PlantStage.STEM, 1000, 1000, false));
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.STEM, 1000, 5000, true));
    }

    @Test
    void plantFollowsItsCondition() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.DORMANT, 0, 0, true));
        assertEquals(PlantStage.DORMANT, PlantLifecycle.decide(PlantStage.BLOOM, 0, 0, false));
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.BLOOM, 0, 0, true));
        assertEquals(PlantStage.DORMANT, PlantLifecycle.decide(PlantStage.DORMANT, 0, 0, false));
    }

    @Test
    void regrowArithmetic() {
        // base minutes times 1200 ticks times the plant factor, from the moment of picking
        assertEquals(1000 + 180 * 1200, PlantLifecycle.regrowAt(1000, 180, 1.0));
        assertEquals(1000 + 108000, PlantLifecycle.regrowAt(1000, 180, 0.5));
        assertEquals(1000 + 144000, PlantLifecycle.regrowAt(1000, 60, 2.0));
        assertEquals(1000 + 3600, PlantLifecycle.regrowAt(1000, 6, 0.5));
        assertEquals(1200, PlantLifecycle.regrowTicks(1, 1.0));
        assertEquals(120, PlantLifecycle.regrowTicks(1, 0.1));
    }

    @Test
    void stageMetadataIsStable() {
        assertEquals(0, PlantStage.STEM.ordinal());
        assertEquals(1, PlantStage.DORMANT.ordinal());
        assertEquals(2, PlantStage.BLOOM.ordinal());
        assertEquals(PlantStage.DORMANT, PlantStage.fromMeta(1));
        assertEquals(PlantStage.BLOOM, PlantStage.fromMeta(9));
        assertEquals("stem", PlantStage.STEM.getName());
    }
}
