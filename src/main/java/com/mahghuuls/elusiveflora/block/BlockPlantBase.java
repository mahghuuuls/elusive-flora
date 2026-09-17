package com.mahghuuls.elusiveflora.block;

import com.mahghuuls.elusiveflora.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * What every plant block shares: the stage property, the tile entity that exists only for a
 * stem, delegation of every stage decision and every pick to {@link PlantLifecycle}, light by
 * stage, no block item, no drops through the normal path, no bone meal, no collision.
 *
 * <p>Subclasses own only what differs by placement kind: material, extra properties, and the
 * support check.
 */
public abstract class BlockPlantBase extends Block {

    public static final PropertyEnum<PlantStage> STAGE = PropertyEnum.create("stage", PlantStage.class);

    private static final AxisAlignedBB PLANT_AABB = new AxisAlignedBB(0.3D, 0.0D, 0.3D, 0.7D, 0.6D, 0.7D);

    /** Dirt's hardness: about one and a half seconds by hand. */
    static final float STEM_HARDNESS = 1.0F;

    protected final PlantLifecycle lifecycle;

    protected BlockPlantBase(Material material, PlantLifecycle lifecycle) {
        super(material);
        this.lifecycle = lifecycle;
        setRegistryName(Tags.MOD_ID, lifecycle.plant().id());
        setTranslationKey(Tags.MOD_ID + "." + lifecycle.plant().id());
        setTickRandomly(true);
        setHardness(0.0F);
        setSoundType(SoundType.PLANT);
        setDefaultState(blockState.getBaseState().withProperty(STAGE, PlantStage.BLOOM));
    }

    public PlantLifecycle lifecycle() {
        return lifecycle;
    }

    /** Whether the plant may stand here now; the placement kind's ground rule. */
    public abstract boolean canStay(World world, BlockPos pos);

    // Stage property and metadata.

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, PlantStage.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE).ordinal();
    }

    // Tile entity only while a stem.

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(STAGE) == PlantStage.STEM;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityStem();
    }

    // Ticks, light, picking.

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) {
            return;
        }
        if (!canStay(world, pos)) {
            world.setBlockToAir(pos);
            return;
        }
        lifecycle.tick(world, pos, state);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return lifecycle.lightFor(state.getValue(STAGE));
    }

    /**
     * A plant picks instantly; a stem takes a held break like dirt. Without this, a click held
     * for a few ticks would pick the plant and then remove the stem the client still saw as an
     * instant-break block.
     */
    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return state.getValue(STAGE) == PlantStage.STEM ? STEM_HARDNESS : 0.0F;
    }

    /**
     * The pick path. Returning false keeps the block in place (now a stem) and skips the vanilla
     * harvest, so the lifecycle owns the drop; returning true lets vanilla remove a stem, which
     * drops nothing because {@link #getDrops} adds nothing.
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        boolean drop = !player.capabilities.isCreativeMode;
        if (lifecycle.onBroken(world, pos, state, drop)) {
            return super.removedByPlayer(state, world, pos, player, willHarvest);
        }
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // Drops happen in the lifecycle at pick time; explosions and other removals yield nothing.
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    // Support.

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canStay(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote && !canStay(world, pos)) {
            world.setBlockToAir(pos);
        }
    }

    // Shape and rendering.

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return PLANT_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
