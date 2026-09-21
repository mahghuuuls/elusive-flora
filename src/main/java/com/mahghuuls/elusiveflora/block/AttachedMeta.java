package com.mahghuuls.elusiveflora.block;

import net.minecraft.util.EnumFacing;

/**
 * How an attached plant packs its two properties into the four metadata bits: the stage in the
 * low two bits, the horizontal facing in the high two. Kept apart from the block class so the
 * packing can be tested without loading Minecraft's block registry.
 */
final class AttachedMeta {

    private static final int STAGE_BITS = 2;
    private static final int STAGE_MASK = (1 << STAGE_BITS) - 1;

    private AttachedMeta() {
    }

    static int toMeta(PlantStage stage, EnumFacing facing) {
        return stage.ordinal() | (facing.getHorizontalIndex() << STAGE_BITS);
    }

    static PlantStage stageOf(int meta) {
        return PlantStage.fromMeta(meta & STAGE_MASK);
    }

    static EnumFacing facingOf(int meta) {
        return EnumFacing.byHorizontalIndex(meta >> STAGE_BITS);
    }
}
