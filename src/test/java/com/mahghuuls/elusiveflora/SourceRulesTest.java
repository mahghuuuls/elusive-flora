package com.mahghuuls.elusiveflora;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Side-safety and integration rules checked against the source tree: no client class outside
 * the client package, and no crop-growth hook anywhere (Serene Seasons would block it in winter).
 * Runs from the project directory, which is where both Gradle and the harness start tests.
 */
class SourceRulesTest {

    private static final Path MAIN = Paths.get("src", "main", "java", "com", "mahghuuls", "elusiveflora");

    @Test
    void noClientImportOutsideTheClientPackage() throws IOException {
        List<String> offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains(File_SEPARATOR + "client" + File_SEPARATOR))
                    .forEach(p -> {
                        if (read(p).contains("import net.minecraft.client.")) {
                            offenders.add(p.toString());
                        }
                    });
        }
        assertTrue(offenders.isEmpty(), "client imports outside client/: " + offenders);
    }

    @Test
    void noCropGrowthHookAnywhere() throws IOException {
        List<String> offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String text = read(p);
                if (text.contains("CropGrowEvent") || text.contains("onCropsGrowPre") || text.contains("onCropsGrowPost")) {
                    offenders.add(p.toString());
                }
            });
        }
        assertTrue(offenders.isEmpty(), "crop growth hooks: " + offenders);
    }

    /** One class may name Serene Seasons, so an API change there touches one file and nothing else loads its classes. */
    @Test
    void sereneSeasonsIsNamedOnlyInItsBridge() throws IOException {
        List<String> offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("SereneSeasonsBridge.java"))
                    .forEach(p -> {
                        // The package token, not the import keyword: a static import or a fully
                        // qualified name counts too. The mod id has no trailing dot, so it passes.
                        if (read(p).contains("sereneseasons.")) {
                            offenders.add(p.toString());
                        }
                    });
        }
        assertTrue(offenders.isEmpty(), "Serene Seasons classes named outside the bridge: " + offenders);
        assertTrue(read(MAIN.resolve("season").resolve("SereneSeasonsBridge.java")).contains("import sereneseasons."),
                "the bridge itself must exist and use the API, or this scan proves nothing");
    }

    /** Patchouli is a client-side book; only its public API, and only from the journal package. */
    @Test
    void patchouliIsUsedOnlyThroughItsApiFromTheJournalPackage() throws IOException {
        List<String> offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String text = read(p);
                boolean inJournal = p.toString().contains(File_SEPARATOR + "client" + File_SEPARATOR + "journal" + File_SEPARATOR);
                if (text.contains("vazkii.patchouli.") && (!inJournal || text.contains("vazkii.patchouli.common") || text.contains("vazkii.patchouli.client"))) {
                    offenders.add(p.toString());
                }
            });
        }
        assertTrue(offenders.isEmpty(), "Patchouli used outside its API or outside client/journal: " + offenders);
    }

    private static final String File_SEPARATOR = java.io.File.separator;

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
