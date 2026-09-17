package com.mahghuuls.elusiveflora.registry;

import com.mahghuuls.elusiveflora.ElusiveFloraMod;
import com.mahghuuls.elusiveflora.Tags;
import com.mahghuuls.elusiveflora.block.BlockPlantBase;
import com.mahghuuls.elusiveflora.item.ItemPickedPlant;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Hands the registry's blocks and items to Forge when it asks for them. Blocks get no item. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModRegistration {

    private ModRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        for (BlockPlantBase block : ElusiveFloraMod.registry().blocks()) {
            event.getRegistry().register(block);
        }
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (ItemPickedPlant item : ElusiveFloraMod.registry().items()) {
            event.getRegistry().register(item);
        }
    }
}
