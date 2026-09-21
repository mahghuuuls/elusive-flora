package com.mahghuuls.elusiveflora.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A plant that grows out of the side of a cliff or a tree trunk. {@code facing} is the way the
 * plant points, away from its support, as a ladder's does; the block behind it must stay an
 * attachable face of the plant's kind or the plant is gone. What counts as attachable is the
 * ground rule's knowledge, not this class's.
 */
public class BlockAttachedPlant extends BlockPlantBase {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    /** How far the selection box reaches out from the wall, in blocks. */
    private static final double REACH = 0.5D;
    private static final AxisAlignedBB FACING_NORTH = new AxisAlignedBB(0.25D, 0.2D, 1.0D - REACH, 0.75D, 0.8D, 1.0D);
    private static final AxisAlignedBB FACING_SOUTH = new AxisAlignedBB(0.25D, 0.2D, 0.0D, 0.75D, 0.8D, REACH);
    private static final AxisAlignedBB FACING_WEST = new AxisAlignedBB(1.0D - REACH, 0.2D, 0.25D, 1.0D, 0.8D, 0.75D);
    private static final AxisAlignedBB FACING_EAST = new AxisAlignedBB(0.0D, 0.2D, 0.25D, REACH, 0.8D, 0.75D);

    public BlockAttachedPlant(PlantLifecycle lifecycle) {
        super(Material.PLANTS, lifecycle);
        setDefaultState(getDefaultState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected boolean canStay(World world, BlockPos pos, IBlockState state) {
        return lifecycle.plant().groundRule().supportsFacing(world, pos, state.getValue(FACING));
    }

    /** Points the plant away from the wall that holds it; the position must be one the ground rule accepted. */
    @Override
    public IBlockState placedState(World world, BlockPos pos, PlantStage stage) {
        EnumFacing facing = lifecycle.plant().groundRule().facingToAttach(world, pos);
        IBlockState state = super.placedState(world, pos, stage);
        return facing == null ? state : state.withProperty(FACING, facing);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(STAGE, AttachedMeta.stageOf(meta))
                .withProperty(FACING, AttachedMeta.facingOf(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return AttachedMeta.toMeta(state.getValue(STAGE), state.getValue(FACING));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case SOUTH:
                return FACING_SOUTH;
            case WEST:
                return FACING_WEST;
            case EAST:
                return FACING_EAST;
            default:
                return FACING_NORTH;
        }
    }
}
