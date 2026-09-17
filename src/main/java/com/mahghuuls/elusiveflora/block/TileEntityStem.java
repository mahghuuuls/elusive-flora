package com.mahghuuls.elusiveflora.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The one piece of data a stem carries: the world time at which it regrows. Exists only while
 * the block is in the {@link PlantStage#STEM} stage; the block creates it on the stage change
 * into stem and lets the world remove it on the change out.
 *
 * <p>Not tickable. Random ticks on the block read the deadline; nothing runs per tick.
 */
public final class TileEntityStem extends TileEntity {

    /** NBT key, a saved-world contract. */
    static final String KEY_REGROW_AT = "regrowAt";

    private long regrowAt;

    /** The world total time at which the stem becomes its plant again. */
    public long regrowAt() {
        return regrowAt;
    }

    public void setRegrowAt(long regrowAt) {
        this.regrowAt = regrowAt;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        regrowAt = compound.getLong(KEY_REGROW_AT);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(KEY_REGROW_AT, regrowAt);
        return compound;
    }

    /**
     * Forge's default for a modded tile entity refreshes on any state change, which would also
     * drop the deadline on a stem-to-stem rewrite (a facing correction on an attached plant, for
     * example). Refresh only when the block changes or the new state no longer wants a tile
     * entity: the stem-to-plant transition removes it, and a stem stays a stem with its deadline.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock() || !newState.getBlock().hasTileEntity(newState);
    }
}
