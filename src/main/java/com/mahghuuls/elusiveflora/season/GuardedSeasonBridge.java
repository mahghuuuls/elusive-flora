package com.mahghuuls.elusiveflora.season;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps another mod's failure from reaching plant ticks. The first time the wrapped bridge
 * throws (a changed API, a broken season config), one warning is logged and the integration is
 * off until the game restarts: every later answer is "no season", so season plants stay in
 * bloom instead of crashing the server or flooding the log once per random tick.
 */
final class GuardedSeasonBridge implements SeasonBridge {

    private final SeasonBridge delegate;
    private final AtomicBoolean disabled = new AtomicBoolean();

    GuardedSeasonBridge(SeasonBridge delegate) {
        this.delegate = delegate;
    }

    @Override
    public Season currentSeason(World world) {
        if (disabled.get()) {
            return null;
        }
        try {
            return delegate.currentSeason(world);
        } catch (RuntimeException | LinkageError failure) {
            if (disabled.compareAndSet(false, true)) {
                ElusiveFloraLog.LOGGER.warn("Serene Seasons season lookup failed; season plants stay in bloom"
                        + " until the game restarts", failure);
            }
            return null;
        }
    }
}
