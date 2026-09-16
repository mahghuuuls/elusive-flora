package com.mahghuuls.elusiveflora.roster;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which biomes a plant may appear in, as names only: biome dictionary type names and biome
 * registry names. A plant is eligible in a biome that carries any listed type or whose registry
 * name is listed. The type {@code *} means any biome.
 *
 * <p>This is the value both the roster and the config produce. Resolving the names against the
 * live registries happens once at startup, elsewhere; this class never sees a biome object.
 */
public final class BiomeRule {

    /** The type name meaning every biome. */
    public static final String ANY = "*";

    private final Set<String> typeNames;
    private final Set<String> biomeNames;

    public BiomeRule(Set<String> typeNames, Set<String> biomeNames) {
        this.typeNames = Collections.unmodifiableSet(new LinkedHashSet<String>(typeNames));
        this.biomeNames = Collections.unmodifiableSet(new LinkedHashSet<String>(biomeNames));
    }

    /**
     * Parses the two roster columns. Both are {@code |}-separated; either may be empty.
     * Type names are kept as written (upper case in the roster); registry names must contain a
     * namespace separator.
     */
    static BiomeRule parse(String rowId, String types, String names) {
        Set<String> typeSet = new LinkedHashSet<String>();
        for (String type : split(types)) {
            if (type.isEmpty()) {
                throw new RosterException(rowId, "biome_types", "empty entry in the list");
            }
            typeSet.add(type);
        }
        Set<String> nameSet = new LinkedHashSet<String>();
        for (String name : split(names)) {
            if (name.isEmpty()) {
                throw new RosterException(rowId, "biome_names", "empty entry in the list");
            }
            if (name.indexOf(':') < 0) {
                throw new RosterException(rowId, "biome_names",
                        "'" + name + "' is not a registry name (expected namespace:path)");
            }
            nameSet.add(name);
        }
        return new BiomeRule(typeSet, nameSet);
    }

    /** Biome dictionary type names, or a set containing {@link #ANY}. */
    public Set<String> typeNames() {
        return typeNames;
    }

    /** Biome registry names such as {@code aether_legacy:aether_highlands}. */
    public Set<String> biomeNames() {
        return biomeNames;
    }

    /** True when the rule matches every biome. */
    public boolean matchesAnyBiome() {
        return typeNames.contains(ANY);
    }

    /** True when neither types nor names are listed, so nothing can ever match. */
    public boolean isEmpty() {
        return typeNames.isEmpty() && biomeNames.isEmpty();
    }

    /**
     * Pure matching on names: true when {@code biomeTypes} shares a type with this rule, or
     * {@code biomeName} is listed, or the rule matches any biome. Registry resolution turns a live
     * biome into these two arguments.
     */
    public boolean matches(String biomeName, Set<String> biomeTypes) {
        if (matchesAnyBiome()) {
            return true;
        }
        if (biomeNames.contains(biomeName)) {
            return true;
        }
        for (String type : biomeTypes) {
            if (typeNames.contains(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Splits a {@code |}-separated list. An empty column is an empty list; an empty element
     * anywhere (a leading, trailing, or doubled separator) is a defect and returns an empty
     * string for the caller to reject, rather than being dropped silently.
     */
    static String[] split(String column) {
        String trimmed = column.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        String[] parts = trimmed.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    @Override
    public String toString() {
        return "BiomeRule{types=" + typeNames + ", names=" + biomeNames + "}";
    }
}
