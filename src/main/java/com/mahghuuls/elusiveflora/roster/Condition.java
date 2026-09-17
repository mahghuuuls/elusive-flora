package com.mahghuuls.elusiveflora.roster;

import com.mahghuuls.elusiveflora.season.Season;
import com.mahghuuls.elusiveflora.season.SeasonBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;

/**
 * When a plant is in bloom. {@link #ALWAYS} plants have no dormant stage. The others open and
 * close on the server as the world's clock, weather, moon, or season changes.
 *
 * <p>This enum owns the exact definitions of night and the moon phases. The arithmetic is pure
 * so it can be tested without a world; {@link #isMet} applies it to a world. This is the one
 * place in the roster package that depends on the season package, because the season bridge is
 * the only way to answer a season condition.
 */
public enum Condition {
    ALWAYS(null),
    NIGHT(null),
    RAIN(null),
    FULL_MOON(null),
    NEW_MOON(null),
    SPRING(Season.SPRING),
    SUMMER(Season.SUMMER),
    AUTUMN(Season.AUTUMN),
    WINTER(Season.WINTER);

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

    private final Season season;

    Condition(Season season) {
        this.season = season;
    }

    static Condition parse(String rowId, String text) {
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RosterException(rowId, "condition", "unknown condition '" + text + "'");
        }
    }

    /** True for a season condition, which needs Serene Seasons to mean anything. */
    public boolean isSeason() {
        return season != null;
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
     * Moon phase from the world's day time, the same formula the vanilla world provider uses:
     * one phase per day, eight phases, phase 0 on the first day. Kept for tests; {@link #isMet}
     * asks the world's provider so a dimension that overrides the phase is honored.
     */
    public static int moonPhase(long dayTime) {
        return (int) (Math.floorMod(dayTime / DAY_TICKS, 8L));
    }

    /**
     * Whether this condition holds right now at a position. Server-side only: the client never
     * evaluates conditions, it renders the stage it is told.
     *
     * <ul>
     *   <li>Night: the day clock, see {@link #isNight(long)}.</li>
     *   <li>Rain: raining at the position, which needs sky access and a biome that rains; a storm
     *       is also rain.</li>
     *   <li>Full and new moon: night on the matching phase.</li>
     *   <li>Seasons: what the bridge reports; no report means the plant is in bloom.</li>
     * </ul>
     */
    public boolean isMet(World world, BlockPos pos, SeasonBridge seasons) {
        switch (this) {
            case ALWAYS:
                return true;
            case NIGHT:
                return isNight(world.getWorldTime());
            case RAIN:
                return world.isRainingAt(pos);
            case FULL_MOON:
                return isNight(world.getWorldTime()) && phaseOf(world) == PHASE_FULL;
            case NEW_MOON:
                return isNight(world.getWorldTime()) && phaseOf(world) == PHASE_NEW;
            default:
                Season current = seasons.currentSeason(world);
                return current == null || current == season;
        }
    }

    private static int phaseOf(World world) {
        return world.provider.getMoonPhase(world.getWorldTime());
    }
}
