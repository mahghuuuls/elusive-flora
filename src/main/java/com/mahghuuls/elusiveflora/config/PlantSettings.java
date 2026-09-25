package com.mahghuuls.elusiveflora.config;

import com.mahghuuls.elusiveflora.roster.BiomeRule;

/** The effective per-plant values after config: roster defaults unless the pack overrides them. */
public final class PlantSettings {

    private final boolean enabled;
    private final int chunkChancePercent;
    private final BiomeRule biomeRule;
    private final double regrowFactor;

    public PlantSettings(boolean enabled, int chunkChancePercent, BiomeRule biomeRule, double regrowFactor) {
        this.enabled = enabled;
        this.chunkChancePercent = chunkChancePercent;
        this.biomeRule = biomeRule;
        this.regrowFactor = regrowFactor;
    }

    /** The effective multiplier of the base regrow time for this plant. */
    public double regrowFactor() {
        return regrowFactor;
    }

    public boolean enabled() {
        return enabled;
    }

    public int chunkChancePercent() {
        return chunkChancePercent;
    }

    public BiomeRule biomeRule() {
        return biomeRule;
    }
}
