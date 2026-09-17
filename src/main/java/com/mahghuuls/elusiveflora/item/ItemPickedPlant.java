package com.mahghuuls.elusiveflora.item;

import com.mahghuuls.elusiveflora.Tags;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import net.minecraft.item.Item;

/**
 * What a pick yields: a plain item with the plant's name and no use of its own. Other mods give
 * it a purpose. Stackable to 64 like any ingredient.
 */
public final class ItemPickedPlant extends Item {

    private final PlantDefinition plant;

    public ItemPickedPlant(PlantDefinition plant) {
        this.plant = plant;
        setRegistryName(Tags.MOD_ID, plant.id());
        setTranslationKey(Tags.MOD_ID + "." + plant.id());
        setMaxStackSize(64);
    }

    public PlantDefinition plant() {
        return plant;
    }
}
