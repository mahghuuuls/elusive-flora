package com.mahghuuls.elusiveflora.block;

import com.mahghuuls.elusiveflora.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
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
    private static final float STEM_HARDNESS = 1.0F;

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

    /**
     * Whether this placed plant, in this state, is still held where it stands. Asked when a
     * neighbor changes and on every tick; a plant that is not held is removed without a drop.
     */
    protected abstract boolean canStay(World world, BlockPos pos, IBlockState state);

    /** How hard a stem is to break; a placement kind where breaking is slower anyway may lower it. */
    protected float stemHardness() {
        return STEM_HARDNESS;
    }

    /**
     * State properties that exist for the game's sake and have no look of their own, so the
     * blockstate files need not list them. None, unless the placement kind carries one.
     */
    public Collection<IProperty<?>> propertiesWithoutLook() {
        return Collections.<IProperty<?>>emptyList();
    }

    /**
     * What takes the plant's place when it goes for good: a broken stem, a lost support, an
     * explosion. Air, unless the placement kind stands in something else.
     */
    protected IBlockState stateWhenGone(World world, BlockPos pos) {
        return Blocks.AIR.getDefaultState();
    }

    /**
     * The state world generation places at a position the ground rule has accepted. A placement
     * kind with more than a stage to decide (which wall to grow from) adds it here.
     */
    public IBlockState placedState(World world, BlockPos pos, PlantStage stage) {
        return getDefaultState().withProperty(STAGE, stage);
    }

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
        if (!canStay(world, pos, state)) {
            world.setBlockState(pos, stateWhenGone(world, pos), 3);
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
        return state.getValue(STAGE) == PlantStage.STEM ? stemHardness() : 0.0F;
    }

    /**
     * The pick path. A plant becomes a stem and stays: returning false keeps the block in place
     * and skips the vanilla harvest, so the lifecycle owns the drop. A stem is removed for good,
     * and drops nothing because {@link #getDrops} adds nothing.
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        boolean drop = !player.capabilities.isCreativeMode;
        if (lifecycle.onBroken(world, pos, state, drop)) {
            // What Forge's default does, with the plant's own idea of "gone" in place of air.
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, stateWhenGone(world, pos), world.isRemote ? 11 : 3);
        }
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // Drops happen in the lifecycle at pick time; explosions and other removals yield nothing.
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        world.setBlockState(pos, stateWhenGone(world, pos), 3);
        onExplosionDestroy(world, pos, explosion);
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
        return super.canPlaceBlockAt(world, pos) && lifecycle.plant().groundRule().matches(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote && !canStay(world, pos, state)) {
            world.setBlockState(pos, stateWhenGone(world, pos), 3);
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
