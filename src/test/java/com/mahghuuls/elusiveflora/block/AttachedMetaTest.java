package com.mahghuuls.elusiveflora.block;

import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachedMetaTest {

    /** Saved worlds hold these numbers; a changed packing would turn every placed plant around. */
    @Test
    void everyStageAndFacingSurvivesTheRoundTripInFourBits() {
        Set<Integer> used = new HashSet<Integer>();
        for (PlantStage stage : PlantStage.values()) {
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                int meta = AttachedMeta.toMeta(stage, facing);
                assertTrue(meta >= 0 && meta < 16, stage + " " + facing + " gives " + meta);
                assertTrue(used.add(meta), "meta " + meta + " used twice");
                assertEquals(stage, AttachedMeta.stageOf(meta));
                assertEquals(facing, AttachedMeta.facingOf(meta));
            }
        }
    }

    @Test
    void thePackingIsPinned() {
        assertEquals(0, AttachedMeta.toMeta(PlantStage.STEM, EnumFacing.SOUTH));
        assertEquals(2, AttachedMeta.toMeta(PlantStage.BLOOM, EnumFacing.SOUTH));
        assertEquals(2 | (2 << 2), AttachedMeta.toMeta(PlantStage.BLOOM, EnumFacing.NORTH));
        assertEquals(1 | (3 << 2), AttachedMeta.toMeta(PlantStage.DORMANT, EnumFacing.EAST));
    }
}
