package com.mahghuuls.elusiveflora.roster;

import com.mahghuuls.elusiveflora.season.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionTest {

    @Test
    void nightWindowEdges() {
        assertFalse(Condition.isNight(12999));
        assertTrue(Condition.isNight(13000));
        assertTrue(Condition.isNight(18000));
        assertTrue(Condition.isNight(23000));
        assertFalse(Condition.isNight(23001));
        assertFalse(Condition.isNight(0));
    }

    @Test
    void nightUsesTheDayClockNotTheTotalTime() {
        assertTrue(Condition.isNight(24000 * 5 + 14000));
        assertFalse(Condition.isNight(24000 * 5 + 6000));
    }

    @Test
    void moonPhases() {
        assertEquals(Condition.PHASE_FULL, Condition.moonPhase(0));
        assertEquals(0, Condition.moonPhase(23999));
        assertEquals(1, Condition.moonPhase(24000));
        assertEquals(Condition.PHASE_NEW, Condition.moonPhase(24000 * 4));
        assertEquals(7, Condition.moonPhase(24000 * 7));
        assertEquals(0, Condition.moonPhase(24000 * 8));
        assertEquals(4, Condition.moonPhase(24000 * 12 + 15000));
    }

    /** A season plant is open in its own season, closed in the other three, and open when no season applies. */
    @Test
    void seasonPlantsFollowTheReportedSeason() {
        assertTrue(Condition.WINTER.isMet(null, null, world -> Season.WINTER));
        assertFalse(Condition.SPRING.isMet(null, null, world -> Season.WINTER));
        assertFalse(Condition.SUMMER.isMet(null, null, world -> Season.WINTER));
        assertFalse(Condition.AUTUMN.isMet(null, null, world -> Season.WINTER));
        assertTrue(Condition.AUTUMN.isMet(null, null, world -> Season.AUTUMN));
        for (Condition season : new Condition[] {Condition.SPRING, Condition.SUMMER, Condition.AUTUMN, Condition.WINTER}) {
            assertTrue(season.isMet(null, null, world -> null), season + " with no season must be in bloom");
        }
    }

    @Test
    void seasonsAreSeasons() {
        assertTrue(Condition.SPRING.isSeason());
        assertTrue(Condition.WINTER.isSeason());
        assertFalse(Condition.ALWAYS.isSeason());
        assertFalse(Condition.NIGHT.isSeason());
        assertFalse(Condition.FULL_MOON.isSeason());
    }
}
