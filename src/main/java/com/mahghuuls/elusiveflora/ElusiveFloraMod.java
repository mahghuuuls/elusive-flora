package com.mahghuuls.elusiveflora;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
    }
}
