package com.mahghuuls.elusiveflora.config;

import com.mahghuuls.elusiveflora.roster.PlantRoster;
import net.minecraftforge.common.config.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Value reading and clamping through Forge's Property class, which works standalone. The
 * Configuration file itself cannot be built outside a running game, so writing the defaults file
 * and overriding from a real file are checked in the development client (Campaign A card A2).
 */
class ElusiveFloraConfigTest {

    @Test
    void clampWarnsAndMoves() {
        assertEquals(100, ElusiveFloraConfig.clampInt(150, "t.a", 0, 100));
        assertEquals(0, ElusiveFloraConfig.clampInt(-3, "t.b", 0, 100));
        assertEquals(42, ElusiveFloraConfig.clampInt(42, "t.c", 0, 100));
        assertEquals(10.0, ElusiveFloraConfig.clampDouble(99.0, "t.d", 0.1, 10.0));
        assertEquals(0.1, ElusiveFloraConfig.clampDouble(0.0, "t.e", 0.1, 10.0));
        assertEquals(2.5, ElusiveFloraConfig.clampDouble(2.5, "t.f", 0.1, 10.0));
    }

    @Test
    void validValuesReadThrough() {
        assertEquals(7, ElusiveFloraConfig.intOf(new Property("k", "7", Property.Type.INTEGER), "t.g", 1));
        assertEquals(2.5, ElusiveFloraConfig.doubleOf(new Property("k", "2.5", Property.Type.DOUBLE), "t.h", 1.0));
        assertTrue(ElusiveFloraConfig.booleanOf(new Property("k", "true", Property.Type.BOOLEAN), "t.i", false));
        assertFalse(ElusiveFloraConfig.booleanOf(new Property("k", "false", Property.Type.BOOLEAN), "t.j", true));
    }

    @Test
    void malformedValuesFallBackToTheDefaultAndAreLeftInPlace() {
        Property typo = new Property("k", "seven", Property.Type.INTEGER);
        assertEquals(1, ElusiveFloraConfig.intOf(typo, "t.k", 1));
        assertEquals("seven", typo.getString(), "the typo stays for the pack maker to see");
        assertEquals(1, ElusiveFloraConfig.intOf(new Property("k", "2.5", Property.Type.INTEGER), "t.l", 1));
        assertEquals(1.0, ElusiveFloraConfig.doubleOf(new Property("k", "fast", Property.Type.DOUBLE), "t.m", 1.0));
        assertTrue(ElusiveFloraConfig.booleanOf(new Property("k", "yes", Property.Type.BOOLEAN), "t.n", true));
    }

    @Test
    void situationTextDropsTheRuleNumber() {
        assertEquals("special ground snow", ElusiveFloraConfig.situationText(PlantRoster.load().byId("frostbell")));
        assertEquals("nether biome next to lava", ElusiveFloraConfig.situationText(PlantRoster.load().byId("ashenlotus")));
    }
}
