package com.mahghuuls.elusiveflora.block;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A plant that sits on top of a ground block. The ground rule decides which blocks qualify. */
public class BlockGroundPlant extends BlockPlantBase {

    public BlockGroundPlant(PlantLifecycle lifecycle) {
        super(Material.PLANTS, lifecycle);
    }

    @Override
    public boolean canStay(World world, BlockPos pos) {
        return lifecycle.plant().groundRule().matches(world, pos);
    }
}
