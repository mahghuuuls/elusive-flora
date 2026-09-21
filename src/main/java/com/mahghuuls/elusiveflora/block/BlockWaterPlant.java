package com.mahghuuls.elusiveflora.block;

import com.mahghuuls.elusiveflora.roster.GroundRule;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;

/**
 * A plant that stands in water on the sea or river bed. In 1.12.2 a block position holds one
 * block, so the plant takes the place of a water block and has to pass for water itself. This
 * class owns that whole disguise:
 * <ul>
 *   <li>its material is water, so the player swims and breathes as in water, and the water
 *       around it neither flows into it nor draws faces against it;</li>
 *   <li>its state carries the liquid {@code level} property, because the game reads that property
 *       from every water-material block it finds next to water or around a swimmer, and throws
 *       when it is missing. The value is fixed at 15, which the game treats as a full block of
 *       water that is not a source: a bucket cannot pick the plant up and it does not count
 *       toward an infinite spring, yet it feeds flowing water beside it as a source would. One
 *       side effect: flowing water directly above the plant never settles into a source. The
 *       property is not saved, never changes, and has no look;</li>
 *   <li>it refuses to be replaced, because water-material blocks are replaceable by default and a
 *       block placed against the plant would otherwise delete it silently;</li>
 *   <li>it resists explosions as water does, so a blast in the plant's block does not crater the
 *       bed the way it never could in plain water;</li>
 *   <li>when it goes, it leaves water behind where there is water beside it, and air in a place
 *       that has been drained, so it never refills what a player emptied. The water it leaves is
 *       the flowing kind, which looks around itself and settles; still water placed by hand
 *       would sit frozen beside a gap until something else woke it.</li>
 * </ul>
 */
public class BlockWaterPlant extends BlockPlantBase {

    /** Any value the game reads as "full, not a source". */
    private static final int FULL_WATER_LEVEL = 15;

    /**
     * Breaking is five times slower under water and five times slower again while floating, so a
     * stem with the land hardness would take over half a minute to clear. This keeps it near the
     * land time for a player standing on the bed.
     */
    private static final float UNDERWATER_STEM_HARDNESS = 0.2F;

    public BlockWaterPlant(PlantLifecycle lifecycle) {
        super(Material.WATER, lifecycle);
        setDefaultState(getDefaultState().withProperty(BlockLiquid.LEVEL, FULL_WATER_LEVEL));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE, BlockLiquid.LEVEL);
    }

    @Override
    public Collection<IProperty<?>> propertiesWithoutLook() {
        return Collections.<IProperty<?>>singletonList(BlockLiquid.LEVEL);
    }

    @Override
    protected boolean canStay(World world, BlockPos pos, IBlockState state) {
        return lifecycle.plant().groundRule().holdsInWater(world, pos);
    }

    @Override
    protected IBlockState stateWhenGone(World world, BlockPos pos) {
        return GroundRule.hasWaterBeside(world, pos)
                ? Blocks.FLOWING_WATER.getDefaultState()
                : super.stateWhenGone(world, pos);
    }

    @Override
    protected float stemHardness() {
        return UNDERWATER_STEM_HARDNESS;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return Blocks.WATER.getExplosionResistance(world, pos, exploder, explosion);
    }
}
