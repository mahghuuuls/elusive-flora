package com.mahghuuls.elusiveflora.world;

import com.mahghuuls.elusiveflora.roster.DimensionKind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Finds the position a surface plant would occupy in a column. The Overworld and the End start
 * from the height map, which names the position just above the highest block that blocks light.
 * That position is air on bare ground. It is a thin snow layer in snowy places, and the plant
 * replaces that layer: it stands in the snow on the ground below, the way a vanilla flower stands
 * in a gap in the snow; standing on top of the layer would leave it floating above two pixels of
 * snow. Any other cover there (tall grass, a flower, thick snow) makes the column unusable. The
 * Nether has a bedrock roof, so its "surface" is any floor with air above it, found by scanning
 * down from a random height the way the Nether's own decorators do.
 *
 * <p>The returned position is always free for a plant: air, or a thin snow layer to replace.
 */
final class SurfaceFinder {

    private static final int NETHER_MIN_Y = 8;
    private static final int NETHER_MAX_Y = 120;

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
        return world.isAirBlock(top) || isThinSnow(world, top) ? top : null;
    }

    /** A one-layer snow cover, which the game itself lets any placed block replace. */
    private static boolean isThinSnow(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.SNOW_LAYER && state.getBlock().isReplaceable(world, pos);
    }
}
