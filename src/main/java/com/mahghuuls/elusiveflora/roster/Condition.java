package com.mahghuuls.elusiveflora.roster;

import java.util.Locale;

/**
 * When a plant is in bloom. {@link #ALWAYS} plants have no dormant stage. The others open and
 * close on the server as the world's clock, weather, moon, or season changes.
 *
 * <p>This enum owns the exact definitions of night and the moon phases. The world-facing check
 * lives with the block lifecycle; the arithmetic here is pure so it can be tested without a world.
 */
public enum Condition {
    ALWAYS,
    NIGHT,
    RAIN,
    FULL_MOON,
    NEW_MOON,
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    /** Minecraft day length in ticks. */
    public static final long DAY_TICKS = 24000L;

    /** First tick of the night window, inclusive. */
    public static final long NIGHT_START = 13000L;

    /** Last tick of the night window, inclusive. */
    public static final long NIGHT_END = 23000L;

    /** Moon phase index of the full moon as Minecraft counts phases. */
    public static final int PHASE_FULL = 0;

    /** Moon phase index of the new moon as Minecraft counts phases. */
    public static final int PHASE_NEW = 4;

    static Condition parse(String rowId, String text) {
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RosterException(rowId, "condition", "unknown condition '" + text + "'");
        }
    }

    /** True for a season condition, which needs Serene Seasons to mean anything. */
    public boolean isSeason() {
        return this == SPRING || this == SUMMER || this == AUTUMN || this == WINTER;
    }

    /**
     * Night is world day time 13000 to 23000 inclusive. The clock is used rather than the sky
     * brightness because rain darkens the sky and would otherwise make "night" weather-dependent.
     *
     * @param dayTime the world's day time, which may already be reduced modulo the day length or not
     */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, DAY_TICKS);
        return t >= NIGHT_START && t <= NIGHT_END;
    }

    /**
     * Moon phase from total world time, the same formula the vanilla world provider uses:
     * one phase per day, eight phases, phase 0 on the first day.
     */
    public static int moonPhase(long worldTime) {
        return (int) (Math.floorMod(worldTime / DAY_TICKS, 8L));
    }

    /**
     * The absolute world time at which a stem regrows: pick time plus the plant's regrow time
     * scaled by the pack's multiplier. Rounded to whole ticks.
     */
    public static long regrowAt(long now, long regrowTicks, double multiplier) {
        return now + Math.round(regrowTicks * multiplier);
    }
}
