package com.mahghuuls.elusiveflora.season;

import net.minecraft.world.World;

/**
 * The one door to Serene Seasons. Returns the season currently in force in a world, or null
 * when no season applies: the mod is absent, the dimension is excluded from seasons, or the
 * integration has been disabled after a failure.
 *
 * <p>A null answer means "always in bloom" for season plants. Nothing outside the {@code season}
 * package names a Serene Seasons class, and this package depends on nothing else in the mod.
 */
public interface SeasonBridge {

    Season currentSeason(World world);
}
