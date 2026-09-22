package com.mahghuuls.elusiveflora.registry;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.roster.BiomeRule;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A biome rule checked against the live game: adapts a {@link Biome} into the registry name and
 * type names the pure {@link BiomeRule} understands, and reports once which configured names
 * resolve to nothing installed. Built after every mod has registered its biomes.
 */
public final class ResolvedBiomeRule {

    // Filled from the server thread during generation and from the client thread by the journal.
    private static final Map<Biome, Set<String>> TYPE_NAMES = new ConcurrentHashMap<Biome, Set<String>>();

    private final BiomeRule rule;
    private final boolean anyInstalled;

    private ResolvedBiomeRule(BiomeRule rule, boolean anyInstalled) {
        this.rule = rule;
        this.anyInstalled = anyInstalled;
    }

    /**
     * Resolves a rule for one plant. Unknown type names and biome names are warned about once
     * per plant and key; they stay in the rule (harmless, they match nothing) so the check tool
     * can still print them.
     */
    static ResolvedBiomeRule resolve(String plantId, BiomeRule rule) {
        boolean anyInstalled = rule.matchesAnyBiome();
        Set<String> knownTypes = new HashSet<String>();
        for (BiomeDictionary.Type type : BiomeDictionary.Type.getAll()) {
            knownTypes.add(type.getName());
        }
        for (String type : rule.typeNames()) {
            if (type.equals(BiomeRule.ANY)) {
                continue;
            }
            if (knownTypes.contains(type)) {
                anyInstalled = true;
            } else {
                ElusiveFloraLog.warnOnce("biome.type." + plantId + "." + type,
                        "Plant " + plantId + ": biome type '" + type + "' is not a known biome dictionary type; ignored");
            }
        }
        for (String name : rule.biomeNames()) {
            if (ForgeRegistries.BIOMES.containsKey(new ResourceLocation(name))) {
                anyInstalled = true;
            } else {
                ElusiveFloraLog.warnOnce("biome.name." + plantId + "." + name,
                        "Plant " + plantId + ": biome '" + name + "' is not installed; ignored");
            }
        }
        return new ResolvedBiomeRule(rule, anyInstalled);
    }

    public boolean matches(Biome biome) {
        ResourceLocation name = biome.getRegistryName();
        return rule.matches(name == null ? "" : name.toString(), typeNamesOf(biome));
    }

    /** False when nothing installed can ever satisfy the rule, so the plant never generates. */
    public boolean anyInstalled() {
        return anyInstalled;
    }

    public BiomeRule rule() {
        return rule;
    }

    private static Set<String> typeNamesOf(Biome biome) {
        Set<String> names = TYPE_NAMES.get(biome);
        if (names == null) {
            names = new HashSet<String>();
            for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
                names.add(type.getName());
            }
            names = Collections.unmodifiableSet(names);
            TYPE_NAMES.put(biome, names);
        }
        return names;
    }
}
