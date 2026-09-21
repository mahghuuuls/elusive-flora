package com.mahghuuls.elusiveflora.season;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import net.minecraftforge.fml.common.Loader;

/** Chooses the season bridge once at startup: Serene Seasons when installed, otherwise none. */
public final class SeasonBridges {

    private static final String SERENE_SEASONS = "sereneseasons";

    private SeasonBridges() {
    }

    public static SeasonBridge detect() {
        if (!Loader.isModLoaded(SERENE_SEASONS)) {
            return new NoSeasonBridge();
        }
        ElusiveFloraLog.LOGGER.info("Serene Seasons found; season plants follow its seasons");
        return new GuardedSeasonBridge(new SereneSeasonsBridge());
    }
}
