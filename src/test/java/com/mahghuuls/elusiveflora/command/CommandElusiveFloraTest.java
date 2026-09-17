package com.mahghuuls.elusiveflora.command;

import com.mahghuuls.elusiveflora.config.PlantSettings;
import com.mahghuuls.elusiveflora.roster.BiomeRule;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import com.mahghuuls.elusiveflora.world.PlacementCheck;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The report text a pack maker reads; the reason text is the check's, the framing is here. */
class CommandElusiveFloraTest {

    @Test
    void hereLineNamesThePlantAndTheVerdict() {
        assertEquals("Frostbell: can appear here", CommandElusiveFlora.lineFor("Frostbell", null));
        assertEquals("Frostbell: " + PlacementCheck.Reason.GROUND.text(),
                CommandElusiveFlora.lineFor("Frostbell", PlacementCheck.Reason.GROUND));
        assertEquals("Sporecap: " + PlacementCheck.Reason.DISABLED.text(),
                CommandElusiveFlora.lineFor("Sporecap", PlacementCheck.Reason.DISABLED));
    }

    @Test
    void installedSuffixCoversTheThreeCases() {
        assertEquals("biomes not resolved yet", CommandElusiveFlora.installedText(false, false));
        assertEquals("matches installed biomes", CommandElusiveFlora.installedText(true, true));
        assertEquals("matches no installed biome", CommandElusiveFlora.installedText(true, false));
    }

    @Test
    void listLineShowsEffectiveSettings() {
        PlantDefinition frostbell = PlantRoster.load().byId("frostbell");
        PlantSettings settings = new PlantSettings(false, 42,
                new BiomeRule(new LinkedHashSet<String>(Arrays.asList("SNOWY", "COLD")), Collections.<String>emptySet()));
        String line = CommandElusiveFlora.listLine(frostbell, settings, "matches installed biomes");
        assertEquals("Frostbell (frostbell): disabled, chunk chance 42 percent, biome types SNOWY COLD,"
                + " biome names none, matches installed biomes", line);
    }

    @Test
    void reportTextIsPlainAscii() {
        String line = CommandElusiveFlora.lineFor("Frostbell", PlacementCheck.Reason.CONDITION);
        for (char c : line.toCharArray()) {
            assertTrue(c < 128, line);
        }
    }
}
