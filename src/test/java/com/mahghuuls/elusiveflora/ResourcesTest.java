package com.mahghuuls.elusiveflora;

import com.mahghuuls.elusiveflora.roster.Condition;
import com.mahghuuls.elusiveflora.roster.PlacementKind;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            for (String stage : new String[] {"stem", "dormant", "bloom"}) {
                if (plant.placementKind() == PlacementKind.ATTACHED) {
                    // The game builds variant keys with the properties in alphabetical order.
                    // The model stands against the south side of its block, so a plant facing north
                    // needs no turn; the others turn clockwise from there, as a ladder does.
                    String[][] facings = {{"north", ""}, {"east", "\"y\": 90"}, {"south", "\"y\": 180"}, {"west", "\"y\": 270"}};
                    for (String[] facing : facings) {
                        String key = "\"facing=" + facing[0] + ",stage=" + stage + "\"";
                        String line = lineContaining(blockstate, key);
                        assertTrue(line != null, plant.id() + " lacks " + key);
                        assertEquals(facing[1].isEmpty(), !line.contains("\"y\""), plant.id() + " " + key + " turn");
                        assertTrue(line.contains(facing[1]), plant.id() + " " + key + " must turn by " + facing[1]);
                    }
                } else {
                    assertTrue(blockstate.contains("\"stage=" + stage + "\""), plant.id() + " lacks stage=" + stage);
                }
            }
            // A plant with no condition is never closed, so it has no closed look to point at.
            boolean dormantModel = blockstate.contains("_dormant\"");
            assertEquals(plant.condition() != Condition.ALWAYS, dormantModel,
                    plant.id() + " dormant model presence must follow its condition");
            assertTrue(Files.exists(ASSETS.resolve("models").resolve("item").resolve(plant.id() + ".json")), plant.id());
            assertTrue(lang.contains("tile.elusiveflora." + plant.id() + ".name=" + plant.displayName()), plant.id());
            assertTrue(lang.contains("item.elusiveflora." + plant.id() + ".name=" + plant.displayName()), plant.id());
        }
        assertTrue(lang.contains("itemGroup.elusiveflora="));
    }

    /** A missing model or texture shows in game as the purple and black checker, never as an error. */
    @Test
    void everyModelAndTextureABlockstateOrModelNamesExists() throws IOException {
        List<String> missing = new ArrayList<String>();
        for (Path file : jsonFilesUnder("blockstates")) {
            Matcher model = Pattern.compile("\"model\": \"elusiveflora:([a-z0-9_]+)\"").matcher(read(file));
            while (model.find()) {
                Path target = ASSETS.resolve("models").resolve("block").resolve(model.group(1) + ".json");
                if (!Files.exists(target)) {
                    missing.add(file.getFileName() + " -> model " + model.group(1));
                }
            }
        }
        List<Path> models = jsonFilesUnder("models/block");
        models.addAll(jsonFilesUnder("models/item"));
        for (Path file : models) {
            String text = read(file);
            Matcher texture = Pattern.compile("\"elusiveflora:((?:blocks|items)/[a-z0-9_]+)\"").matcher(text);
            while (texture.find()) {
                if (!Files.exists(ASSETS.resolve("textures").resolve(texture.group(1) + ".png"))) {
                    missing.add(file.getFileName() + " -> texture " + texture.group(1));
                }
            }
            Matcher parent = Pattern.compile("\"parent\": \"elusiveflora:block/([a-z0-9_]+)\"").matcher(text);
            while (parent.find()) {
                if (!Files.exists(ASSETS.resolve("models").resolve("block").resolve(parent.group(1) + ".json"))) {
                    missing.add(file.getFileName() + " -> parent " + parent.group(1));
                }
            }
        }
        assertTrue(missing.isEmpty(), "dangling references: " + missing);
        assertTrue(models.size() >= 60, "model files read: " + models.size());
    }

    /** The game needs square power-of-two model textures; this set is 16x16 RGBA throughout. */
    @Test
    void everyTextureIsSixteenSquareWithAlpha() throws IOException {
        List<Path> textures = new ArrayList<Path>();
        for (String folder : new String[] {"blocks", "items"}) {
            try (Stream<Path> files = Files.list(ASSETS.resolve("textures").resolve(folder))) {
                files.filter(p -> p.toString().endsWith(".png")).forEach(textures::add);
            }
        }
        assertTrue(textures.size() >= 60, "textures found: " + textures.size());
        for (Path png : textures) {
            byte[] header = Arrays.copyOf(Files.readAllBytes(png), 26);
            int width = ByteBuffer.wrap(header, 16, 4).getInt();
            int height = ByteBuffer.wrap(header, 20, 4).getInt();
            assertEquals(16, width, png + " width");
            assertEquals(16, height, png + " height");
            assertEquals(8, header[24], png + " must be 8 bits per channel");
            assertEquals(6, header[25], png + " must be RGBA (PNG color type 6)");
        }
    }

    /** A cutout plant has a clear background and hard edges: every pixel is fully there or fully not. */
    @Test
    void everyTextureHasAClearBackgroundAndNoSoftEdges() throws IOException {
        for (String folder : new String[] {"blocks", "items"}) {
            try (Stream<Path> files = Files.list(ASSETS.resolve("textures").resolve(folder))) {
                for (Path png : files.filter(p -> p.toString().endsWith(".png")).collect(Collectors.toList())) {
                    BufferedImage image = ImageIO.read(png.toFile());
                    int clear = 0;
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int alpha = image.getRGB(x, y) >>> 24;
                            assertTrue(alpha == 0 || alpha == 255, png + " has a soft pixel at " + x + "," + y);
                            if (alpha == 0) {
                                clear++;
                            }
                        }
                    }
                    assertTrue(clear > 0, png + " has no clear background");
                    assertEquals(0, image.getRGB(0, 0) >>> 24, png + " top left corner must be clear");
                }
            }
        }
    }

    /** Each plant shows its own pictures: a model that points at another plant's texture still loads. */
    @Test
    void everyPlantPointsAtItsOwnTextures() throws IOException {
        for (PlantDefinition plant : PlantRoster.load().plants()) {
            String id = plant.id();
            assertTrue(Files.exists(ASSETS.resolve("textures").resolve("items").resolve(id + ".png")), id);
            String itemModel = read(ASSETS.resolve("models").resolve("item").resolve(id + ".json"));
            assertTrue(itemModel.contains("\"elusiveflora:items/" + id + "\""), id + " item model texture");

            String blockstate = read(ASSETS.resolve("blockstates").resolve(id + ".json"));
            Matcher model = Pattern.compile("stage=([a-z]+)\": \\{ \"model\": \"elusiveflora:([a-z0-9_]+)\"").matcher(blockstate);
            int variants = 0;
            while (model.find()) {
                variants++;
                String stage = model.group(1);
                String name = model.group(2);
                if (name.startsWith("placeholder_")) {
                    continue;
                }
                boolean closedLook = stage.equals("dormant") && plant.condition() != Condition.ALWAYS;
                String expected = id + "_" + (stage.equals("stem") ? "stem" : closedLook ? "dormant" : "bloom");
                assertEquals(expected, name, id + " stage=" + stage);
                String blockModel = read(ASSETS.resolve("models").resolve("block").resolve(name + ".json"));
                assertTrue(blockModel.contains("\"elusiveflora:blocks/" + name + "\""), name + " model texture");
            }
            assertTrue(variants >= 3, id + " blockstate variants read: " + variants);
        }
    }

    private static List<Path> jsonFilesUnder(String folder) throws IOException {
        try (Stream<Path> files = Files.list(ASSETS.resolve(folder))) {
            return files.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private static String lineContaining(String text, String needle) {
        for (String line : text.split("\n")) {
            if (line.contains(needle)) {
                return line;
            }
        }
        return null;
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
