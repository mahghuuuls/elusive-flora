package com.mahghuuls.elusiveflora.season;

import net.minecraft.world.World;

/** The bridge used when Serene Seasons is not installed: no season ever applies. */
final class NoSeasonBridge implements SeasonBridge {

    @Override
    public Season currentSeason(World world) {
        return null;
    }
}
