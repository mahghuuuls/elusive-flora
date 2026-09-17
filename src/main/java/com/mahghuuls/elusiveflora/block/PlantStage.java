package com.mahghuuls.elusiveflora.block;

import net.minecraft.util.IStringSerializable;

/**
 * The block state of a plant. {@link #STEM} is what a pick leaves behind and the only stage with
 * a tile entity; {@link #DORMANT} is a closed plant that yields nothing; {@link #BLOOM} is open
 * and yields its item. A plant with condition "always" never uses {@link #DORMANT}.
 *
 * <p>The ordinal is the block metadata, so the order here is a saved-world contract.
 */
public enum PlantStage implements IStringSerializable {
    STEM("stem"),
    DORMANT("dormant"),
    BLOOM("bloom");

    private final String name;

    PlantStage(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static PlantStage fromMeta(int meta) {
        PlantStage[] values = values();
        return meta >= 0 && meta < values.length ? values[meta] : BLOOM;
    }
}
