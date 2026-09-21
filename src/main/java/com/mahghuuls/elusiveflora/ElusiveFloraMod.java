package com.mahghuuls.elusiveflora;

import com.mahghuuls.elusiveflora.block.TileEntityStem;
import com.mahghuuls.elusiveflora.command.CommandElusiveFlora;
import com.mahghuuls.elusiveflora.config.ElusiveFloraConfig;
import com.mahghuuls.elusiveflora.registry.PlantRegistry;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import com.mahghuuls.elusiveflora.season.SeasonBridge;
import com.mahghuuls.elusiveflora.season.SeasonBridges;
import com.mahghuuls.elusiveflora.world.PlacementCheck;
import com.mahghuuls.elusiveflora.world.PlantWorldGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Entry point for Elusive Flora: rare plants placed by world generation that regrow where they
 * were found and cannot be moved or farmed.
 *
 * <p>Loads on both sides. Every decision (placement, stage changes, picking, regrowth) is made on
 * the logical server; the client only renders block states it receives.
 *
 * <p>Patchouli is the one required mod, for the journal. Serene Seasons is optional: season plants
 * bloom in their season when it is present and are always in bloom when it is absent. Dependency
 * ranges are minimum-only on purpose: the versions named are the ones this mod was built and tested
 * against, and nothing newer is excluded.
 */
@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);"
                + "required-after:patchouli@[1.0-28,);"
                + "after:sereneseasons")
public class ElusiveFloraMod {

    private static PlantRegistry registry;
    private static PlacementCheck placementCheck;

    /** The plant registry, available from pre-initialization on. */
    public static PlantRegistry registry() {
        return registry;
    }

    /** The shared placement check, available from pre-initialization on. */
    public static PlacementCheck placementCheck() {
        return placementCheck;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ElusiveFloraLog.LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
        PlantRoster roster = PlantRoster.load();
        ElusiveFloraConfig config = ElusiveFloraConfig.load(event.getSuggestedConfigurationFile(), roster);
        SeasonBridge seasons = SeasonBridges.detect();
        registry = new PlantRegistry(roster, config, seasons);
        placementCheck = new PlacementCheck(registry, seasons);
        GameRegistry.registerTileEntity(TileEntityStem.class, new ResourceLocation(Tags.MOD_ID, "stem"));
        GameRegistry.registerWorldGenerator(new PlantWorldGenerator(registry, placementCheck), 10);
        ElusiveFloraLog.LOGGER.info("{} plants registered", registry.blocks().size());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        registry.resolveBiomes();
    }

    /** The check tool exists only when the pack maker turned it on; off, the command is absent. */
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (registry.enableCheckCommand()) {
            event.registerServerCommand(new CommandElusiveFlora(registry, placementCheck));
            ElusiveFloraLog.LOGGER.info("Check command /elusiveflora registered (enableCheckCommand = true)");
        }
    }
}
