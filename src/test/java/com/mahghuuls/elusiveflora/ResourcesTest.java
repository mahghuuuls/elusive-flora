package com.mahghuuls.elusiveflora;

import com.mahghuuls.elusiveflora.roster.Condition;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The roster-to-resources link: every plant has a blockstate with all three stages, a dormant
 * variant only when it has a condition, an item model, and both language keys. Runs from the
 * project directory, where Gradle and the harness start tests.
 */
class ResourcesTest {

    private static final Path ASSETS = Paths.get("src", "main", "resources", "assets", "elusiveflora");

    @Test
    void everyPlantHasItsBlockstateItemModelAndNames() throws IOException {
        String lang = read(ASSETS.resolve("lang").resolve("en_us.lang"));
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            String blockstate = read(ASSETS.resolve("blockstates").resolve(plant.id() + ".json"));
            assertTrue(blockstate.contains("\"stage=stem\""), plant.id());
            assertTrue(blockstate.contains("\"stage=dormant\""), plant.id());
            assertTrue(blockstate.contains("\"stage=bloom\""), plant.id());
            boolean dormantModel = blockstate.contains("placeholder_dormant");
            assertEquals(plant.condition() != Condition.ALWAYS, dormantModel,
                    plant.id() + " dormant model presence must follow its condition");
            assertTrue(Files.exists(ASSETS.resolve("models").resolve("item").resolve(plant.id() + ".json")), plant.id());
            assertTrue(lang.contains("tile.elusiveflora." + plant.id() + ".name=" + plant.displayName()), plant.id());
            assertTrue(lang.contains("item.elusiveflora." + plant.id() + ".name=" + plant.displayName()), plant.id());
        }
        assertTrue(lang.contains("itemGroup.elusiveflora="));
    }

    @Test
    void placeholderModelsAndTexturesExist() {
        for (String stage : new String[] {"bloom", "dormant", "stem"}) {
            assertTrue(Files.exists(ASSETS.resolve("models").resolve("block").resolve("placeholder_" + stage + ".json")), stage);
            assertTrue(Files.exists(ASSETS.resolve("textures").resolve("blocks").resolve("placeholder_" + stage + ".png")), stage);
        }
        assertTrue(Files.exists(ASSETS.resolve("textures").resolve("items").resolve("placeholder.png")));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
