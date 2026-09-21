package com.mahghuuls.elusiveflora.roster;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * A world of air holding a few stub blocks, each only a material and the faces it turns solid.
 * That is all the side and water rules read. The block classes of the game cannot be loaded
 * without the game, so a rule that asks for a block itself cannot be tested this way.
 */
final class StubWorld implements InvocationHandler {

    private final Map<BlockPos, IBlockState> blocks = new HashMap<BlockPos, IBlockState>();
    private final IBlockState air = state(Material.AIR);

    void put(BlockPos pos, Material material, EnumFacing... solidFaces) {
        blocks.put(pos, state(material, solidFaces));
    }

    /** A column of one material from {@code bottom} upward, {@code height} blocks tall, no solid faces. */
    void fillUp(BlockPos bottom, int height, Material material) {
        for (int dy = 0; dy < height; dy++) {
            put(bottom.up(dy), material);
        }
    }

    IBlockAccess access() {
        return (IBlockAccess) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {IBlockAccess.class}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("getBlockState")) {
            IBlockState found = blocks.get((BlockPos) args[0]);
            return found == null ? air : found;
        }
        throw new UnsupportedOperationException(method.getName());
    }

    private static IBlockState state(final Material material, final EnumFacing... solidFaces) {
        return (IBlockState) Proxy.newProxyInstance(StubWorld.class.getClassLoader(),
                new Class<?>[] {IBlockState.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method.getName().equals("getMaterial")) {
                            return material;
                        }
                        if (method.getName().equals("isSideSolid")) {
                            for (EnumFacing face : solidFaces) {
                                if (face == args[2]) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}
