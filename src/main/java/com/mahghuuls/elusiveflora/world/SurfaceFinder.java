package com.mahghuuls.elusiveflora.world;

import com.mahghuuls.elusiveflora.roster.DimensionKind;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Finds the position a surface plant would occupy in a column: the first air block above the
 * highest ground. The Overworld and the End start from the height map, which stops at the first
 * block that lets light through, so a snow layer, tall grass, or a flower on top is stepped over
 * (a few blocks at most) and the ground rule then judges what is below. The Nether has a bedrock
 * roof, so its "surface" is any floor with air above it, found by scanning down from a random
 * height the way the Nether's own decorators do.
 */
final class SurfaceFinder {

    private static final int NETHER_MIN_Y = 8;
    private static final int NETHER_MAX_Y = 120;

    /** How far above the height map a transparent cover (snow layer, grass) may be stepped over. */
    private static final int COVER_STEPS = 3;

    private SurfaceFinder() {
    }

    /** The candidate plant position for the column, or null when the column has no usable surface. */
    static BlockPos find(World world, DimensionKind kind, int x, int z, Random random) {
        if (kind == DimensionKind.NETHER) {
            int start = NETHER_MIN_Y + random.nextInt(NETHER_MAX_Y - NETHER_MIN_Y);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, start, z);
            while (pos.getY() > NETHER_MIN_Y) {
                if (world.isAirBlock(pos) && !world.isAirBlock(pos.down())) {
                    return pos.toImmutable();
                }
                pos.move(EnumFacing.DOWN);
            }
            return null;
        }
        BlockPos top = world.getHeight(new BlockPos(x, 0, z));
        if (top.getY() <= 0) {
            return null;
        }
        for (int step = 0; step < COVER_STEPS && !world.isAirBlock(top); step++) {
            top = top.up();
        }
        return world.isAirBlock(top) ? top : null;
    }
}
