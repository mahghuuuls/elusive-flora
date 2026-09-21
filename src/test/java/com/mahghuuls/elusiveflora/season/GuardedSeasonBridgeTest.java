package com.mahghuuls.elusiveflora.season;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardedSeasonBridgeTest {

    /** Answers from a script, counting calls; a null world is enough because the fake ignores it. */
    private static final class ScriptedBridge implements SeasonBridge {
        private int calls;
        private boolean failing;
        private Throwable failure = new IllegalStateException("season data missing");

        @Override
        public Season currentSeason(World world) {
            calls++;
            if (failing) {
                if (failure instanceof Error) {
                    throw (Error) failure;
                }
                throw (RuntimeException) failure;
            }
            return Season.WINTER;
        }
    }

    /** Collects what the mod's logger writes at WARN, so "logged once" is measured, not assumed. */
    private static final class WarningCollector extends AbstractAppender {
        private final List<LogEvent> warnings = new ArrayList<LogEvent>();

        WarningCollector() {
            super("guarded-season-bridge-test", null, null);
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel() == Level.WARN) {
                warnings.add(event.toImmutable());
            }
        }
    }

    private final WarningCollector collector = new WarningCollector();

    @BeforeEach
    void listen() {
        collector.start();
        ((Logger) ElusiveFloraLog.LOGGER).addAppender(collector);
    }

    @AfterEach
    void stopListening() {
        ((Logger) ElusiveFloraLog.LOGGER).removeAppender(collector);
        collector.stop();
    }

    @Test
    void passesTheSeasonThroughWhileHealthy() {
        ScriptedBridge inner = new ScriptedBridge();
        SeasonBridge guarded = new GuardedSeasonBridge(inner);
        assertSame(Season.WINTER, guarded.currentSeason(null));
        assertSame(Season.WINTER, guarded.currentSeason(null));
        assertEquals(2, inner.calls);
        assertTrue(collector.warnings.isEmpty(), "a healthy integration logs nothing");
    }

    @Test
    void oneFailureTurnsTheIntegrationOffForGoodAndWarnsOnce() {
        ScriptedBridge inner = new ScriptedBridge();
        SeasonBridge guarded = new GuardedSeasonBridge(inner);
        inner.failing = true;
        assertNull(guarded.currentSeason(null));
        inner.failing = false;
        assertNull(guarded.currentSeason(null), "a recovered provider must not flip plants back mid-session");
        assertNull(guarded.currentSeason(null));
        assertEquals(1, inner.calls, "after the failure the provider is never asked again");
        assertEquals(1, collector.warnings.size(), "one warning for the session, not one per plant tick");
        LogEvent warning = collector.warnings.get(0);
        assertTrue(warning.getMessage().getFormattedMessage().contains("Serene Seasons"));
        assertNotNull(warning.getThrown(), "the cause must reach the log so a pack maker can report it");
    }

    @Test
    void aChangedApiIsAFailureToo() {
        ScriptedBridge inner = new ScriptedBridge();
        inner.failing = true;
        inner.failure = new NoSuchMethodError("getSeasonState");
        SeasonBridge guarded = new GuardedSeasonBridge(inner);
        assertNull(guarded.currentSeason(null));
        assertNull(guarded.currentSeason(null));
        assertEquals(1, inner.calls);
        assertEquals(1, collector.warnings.size());
    }
}
