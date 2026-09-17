package com.mahghuuls.elusiveflora.registry;

import com.mahghuuls.elusiveflora.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** The mod's creative tab: the picked items and, later, the journal. No plant block appears. */
public final class ModCreativeTab extends CreativeTabs {

    private final Item icon;

    ModCreativeTab(Item icon) {
        super(Tags.MOD_ID);
        this.icon = icon;
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(icon);
    }
}
