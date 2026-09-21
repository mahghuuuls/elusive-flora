package com.mahghuuls.elusiveflora.season;

import net.minecraft.world.World;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;

/**
 * Asks Serene Seasons for the season. The only class in the mod that names a Serene Seasons
 * class, so it is loaded only when that mod is installed (see {@link SeasonBridges}).
 *
 * <p>Serene Seasons applies and shows seasons only in the dimensions its config lists. Elsewhere
 * it still counts a season internally, but the player can see none, so a plant closing for it
 * would look broken. Such a dimension therefore has no season here. Failures are not handled in this
 * class: {@link GuardedSeasonBridge} wraps it.
 */
final class SereneSeasonsBridge implements SeasonBridge {

    @Override
    public Season currentSeason(World world) {
        if (!SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension())) {
            return null;
        }
        // The two enums share their four names.
        return Season.valueOf(SeasonHelper.getSeasonState(world).getSeason().name());
    }
}
