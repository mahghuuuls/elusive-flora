package com.mahghuuls.elusiveflora.client;

import com.mahghuuls.elusiveflora.ElusiveFloraMod;
import com.mahghuuls.elusiveflora.Tags;
import com.mahghuuls.elusiveflora.item.ItemPickedPlant;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Item models for every picked item. Block models come from the blockstate files and need no
 * code. The only client-side class of this slice.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MOD_ID)
public final class ClientModels {

    private ClientModels() {
    }

    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event) {
        for (ItemPickedPlant item : ElusiveFloraMod.registry().items()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
