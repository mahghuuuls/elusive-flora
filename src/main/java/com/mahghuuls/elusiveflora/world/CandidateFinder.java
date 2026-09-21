package com.mahghuuls.elusiveflora.world;

import com.mahghuuls.elusiveflora.roster.DimensionKind;
import com.mahghuuls.elusiveflora.roster.GroundRule;
import com.mahghuuls.elusiveflora.roster.PlacementKind;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Finds the position a plant would occupy in a column, or null when the column offers none.
 * The returned position is always free for the plant: air, a thin snow layer to replace, or for a
 * water plant the still water it will stand in.
 *
 * <p>A surface plant stands at the top of the column. The Overworld and the End start from the
 * height map, which names the position just above the highest block that blocks light. That
 * position is air on bare ground. It is a thin snow layer in snowy places, and the plant
 * replaces that layer: it stands in the snow on the ground below, the way a vanilla flower stands
 * in a gap in the snow; standing on top of the layer would leave it floating above two pixels of
 * snow. Any other cover there (tall grass, a flower, thick snow) makes the column unusable. The
 * Nether has a bedrock roof, so its "surface" is any floor with air above it, found by scanning
 * down from a random height the way the Nether's own decorators do.
 *
 * <p>An attached plant hangs on a wall beside the column, so its column is searched up and down
 * from the height map: up through the open air, where a cliff may rise beside it, and down
 * through a tree's canopy, where a trunk may stand beside it. The way down passes only air,
 * leaves, and the vine material (vines, tall grass, ferns), so it stops at the first real block
 * and does not wander into caves; a pit roofed only by leaves is the one place it can go deep.
 * The position that rests on the ground is skipped, so the plant reads as growing out of the wall.
 *
 * <p>A water plant stands on the bed of its column, in the place of the still water block there.
 * A column roofed by ice has no such block at its top and is unusable.
 */
final class CandidateFinder {

    private static final int NETHER_MIN_Y = 8;
    private static final int NETHER_MAX_Y = 120;

    /** How far above the column's top a cliff face is looked for. */
    private static final int ATTACHED_ABOVE = 12;
    /** How far below the column's top, through canopy, a trunk face is looked for. */
    private static final int ATTACHED_BELOW = 24;

    private CandidateFinder() {
    }

    static BlockPos find(World world, PlantDefinition plant, int x, int z, Random random) {
        if (plant.placementKind() == PlacementKind.ATTACHED) {
            return findAttached(world, plant.groundRule(), x, z, random);
        }
        if (plant.placementKind() == PlacementKind.WATER) {
            return findWaterBed(world, x, z);
        }
        if (plant.dimension() == DimensionKind.NETHER) {
            return findNetherFloor(world, x, z, random);
        }
        BlockPos top = world.getHeight(new BlockPos(x, 0, z));
        if (top.getY() <= 0) {
            return null;
        }
        return world.isAirBlock(top) || isThinSnow(world, top) ? top : null;
    }

    private static BlockPos findNetherFloor(World world, int x, int z, Random random) {
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

    /** One of the column's hanging positions with an attachable face beside it, chosen at random. */
    private static BlockPos findAttached(World world, GroundRule rule, int x, int z, Random random) {
        BlockPos top = world.getHeight(new BlockPos(x, 0, z));
        if (top.getY() <= 0) {
            return null;
        }
        List<BlockPos> hangingSpots = new ArrayList<BlockPos>();
        for (int dy = 0; dy < ATTACHED_ABOVE; dy++) {
            collectHangingSpot(world, rule, top.up(dy), hangingSpots);
        }
        BlockPos pos = top.down();
        for (int dy = 0; dy < ATTACHED_BELOW && isOpenOrFoliage(world, pos); dy++) {
            collectHangingSpot(world, rule, pos, hangingSpots);
            pos = pos.down();
        }
        return hangingSpots.isEmpty() ? null : hangingSpots.get(random.nextInt(hangingSpots.size()));
    }

    private static void collectHangingSpot(World world, GroundRule rule, BlockPos pos, List<BlockPos> spots) {
        if (world.isAirBlock(pos) && isOpenOrFoliage(world, pos.down()) && rule.facingToAttach(world, pos) != null) {
            spots.add(pos);
        }
    }

    private static boolean isOpenOrFoliage(World world, BlockPos pos) {
        Material material = world.getBlockState(pos).getMaterial();
        return material == Material.AIR || material == Material.LEAVES || material == Material.VINE;
    }

    /**
     * The still water block resting on the column's bed. Despite its name, the game's "top solid
     * or liquid block" is the position above the highest block that stops movement, and water does
     * not, so in a lake this is the bottom of the water and on land it is the air above the ground.
     */
    private static BlockPos findWaterBed(World world, int x, int z) {
        BlockPos aboveBed = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
        IBlockState state = world.getBlockState(aboveBed);
        // The game's own water only: another mod's liquid would come back as plain water later.
        boolean gameWater = state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER;
        boolean stillWater = gameWater && state.getValue(BlockLiquid.LEVEL) == 0;
        return stillWater ? aboveBed : null;
    }

    /** A one-layer snow cover, which the game itself lets any placed block replace. */
    private static boolean isThinSnow(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.SNOW_LAYER && state.getBlock().isReplaceable(world, pos);
    }
}
