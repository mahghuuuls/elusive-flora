package com.mahghuuls.elusiveflora.block;

import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.season.SeasonBridge;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The one owner of "what stage should this plant be in now", of what a pick does, and of the
 * regrowth formula. Every block class delegates here; none decides a stage itself.
 *
 * <p>Rules, in order:
 * <ul>
 *   <li>A stem becomes its plant once the world's total time reaches its deadline.</li>
 *   <li>A plant is in bloom while its condition holds and dormant otherwise. A plant whose
 *       condition is always has no dormant stage.</li>
 *   <li>Breaking a plant in bloom drops its yield and leaves a stem with a fresh deadline;
 *       breaking a dormant plant leaves a stem and drops nothing; breaking a stem removes it.</li>
 *   <li>Light is emitted only in bloom.</li>
 * </ul>
 *
 * <p>Deadlines use the world's total time ({@code getTotalWorldTime}), which runs while the server
 * runs, keeps counting while a chunk is unloaded, and is not moved by the {@code /time} command
 * or by {@code doDaylightCycle}. A stem that missed its deadline while unloaded regrows on the
 * first random tick after the chunk loads.
 */
public final class PlantLifecycle {

    /**
     * What the pack multiplies regrow times by. An interface rather than a number because the
     * block package may not depend on the config package; the registry supplies the value.
     */
    public interface RegrowthScale {
        double multiplier();
    }

    private final PlantDefinition plant;
    private final Item pickedItem;
    private final SeasonBridge seasons;
    private final RegrowthScale scale;

    public PlantLifecycle(PlantDefinition plant, Item pickedItem, SeasonBridge seasons, RegrowthScale scale) {
        this.plant = plant;
        this.pickedItem = pickedItem;
        this.seasons = seasons;
        this.scale = scale;
    }

    public PlantDefinition plant() {
        return plant;
    }

    /** The stage a freshly placed plant starts in: bloom or dormant by its condition, never stem. */
    public PlantStage initialStage(World world, BlockPos pos) {
        return conditionMet(world, pos) ? PlantStage.BLOOM : PlantStage.DORMANT;
    }

    /**
     * The stage the block should show now, given its current stage and, for a stem, its
     * deadline. Pure so it can be tested without a world: the caller supplies the clock and the
     * condition's verdict.
     */
    public static PlantStage decide(PlantStage current, long regrowAt, long now, boolean conditionMet) {
        if (current == PlantStage.STEM && now < regrowAt) {
            return PlantStage.STEM;
        }
        return conditionMet ? PlantStage.BLOOM : PlantStage.DORMANT;
    }

    /**
     * The total world time at which a stem picked now regrows: now plus the plant's regrow time
     * scaled by the pack's multiplier, rounded to whole ticks.
     */
    public static long regrowAt(long now, long regrowTicks, double multiplier) {
        return now + Math.round(regrowTicks * multiplier);
    }

    /** Called from the block's random tick on the server. Applies {@link #decide} to the world. */
    public void tick(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            return;
        }
        PlantStage current = state.getValue(BlockPlantBase.STAGE);
        long regrowAt = current == PlantStage.STEM ? deadlineOf(world, pos) : 0L;
        PlantStage target = decide(current, regrowAt, world.getTotalWorldTime(), conditionMet(world, pos));
        if (target != current) {
            world.setBlockState(pos, state.withProperty(BlockPlantBase.STAGE, target), 3);
        }
    }

    /**
     * What breaking the block at this stage does. Returns true when the block should be removed
     * outright (a stem), false when it has been turned into a stem and must stay.
     *
     * <p>On the client the stem is set locally as a prediction, so the held attack key sees a
     * stem (which takes real time to break) instead of re-sending an instant break every tick
     * against a bloom plant the server has already turned into a stem. The server's own block
     * change overrides the prediction either way.
     *
     * @param drop whether the pick yields items (false in creative mode)
     */
    public boolean onBroken(World world, BlockPos pos, IBlockState state, boolean drop) {
        PlantStage current = state.getValue(BlockPlantBase.STAGE);
        if (current == PlantStage.STEM) {
            return true;
        }
        IBlockState stem = state.withProperty(BlockPlantBase.STAGE, PlantStage.STEM);
        if (world.isRemote) {
            world.setBlockState(pos, stem, 11);
            return false;
        }
        if (current == PlantStage.BLOOM && drop && pickedItem != null) {
            Block.spawnAsEntity(world, pos, new ItemStack(pickedItem, plant.yieldPerPick()));
        }
        world.setBlockState(pos, stem, 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityStem) {
            ((TileEntityStem) tile).setRegrowAt(
                    regrowAt(world.getTotalWorldTime(), plant.regrowTicks(), scale.multiplier()));
        }
        return false;
    }

    /** Light emitted at a stage: the plant's glow in bloom, nothing otherwise. */
    public int lightFor(PlantStage stage) {
        return stage == PlantStage.BLOOM ? plant.glowLight() : 0;
    }

    private boolean conditionMet(World world, BlockPos pos) {
        return plant.condition().isMet(world, pos, seasons);
    }

    private static long deadlineOf(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        // A stem without its tile entity (a corrupted save) regrows at once rather than never.
        return tile instanceof TileEntityStem ? ((TileEntityStem) tile).regrowAt() : Long.MIN_VALUE;
    }
}
