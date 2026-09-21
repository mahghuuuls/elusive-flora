package com.mahghuuls.elusiveflora.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A plant that sits on top of a ground block. The ground rule decides which blocks qualify. */
public class BlockGroundPlant extends BlockPlantBase {

    public BlockGroundPlant(PlantLifecycle lifecycle) {
        super(Material.PLANTS, lifecycle);
    }

    @Override
    protected boolean canStay(World world, BlockPos pos, IBlockState state) {
        return lifecycle.plant().groundRule().matches(world, pos);
    }
}
