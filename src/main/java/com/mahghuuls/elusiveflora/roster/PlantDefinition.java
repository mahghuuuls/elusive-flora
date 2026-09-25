package com.mahghuuls.elusiveflora.roster;

/**
 * One plant as the roster defines it. Immutable; every field comes from one CSV row. The config
 * may override chunk chance, biome rules, and enabled state per pack, but the definition itself
 * never changes after load.
 */
public final class PlantDefinition {

    private final String id;
    private final String displayName;
    private final DimensionKind dimension;
    private final String situation;
    private final BiomeRule biomeRule;
    private final GroundRule groundRule;
    private final Condition condition;
    private final String rarity;
    private final int chunkChancePercent;
    private final double regrowFactor;
    private final int yieldPerPick;
    private final boolean showcase;
    private final boolean threeD;
    private final int glowLight;

    PlantDefinition(String id, String displayName, DimensionKind dimension, String situation,
                    BiomeRule biomeRule, GroundRule groundRule, Condition condition, String rarity,
                    int chunkChancePercent, double regrowFactor, int yieldPerPick, boolean threeD,
                    int glowLight, boolean showcase) {
        this.id = id;
        this.displayName = displayName;
        this.dimension = dimension;
        this.situation = situation;
        this.biomeRule = biomeRule;
        this.groundRule = groundRule;
        this.condition = condition;
        this.rarity = rarity;
        this.chunkChancePercent = chunkChancePercent;
        this.regrowFactor = regrowFactor;
        this.yieldPerPick = yieldPerPick;
        this.threeD = threeD;
        this.glowLight = glowLight;
        this.showcase = showcase;
    }

    /** Registry path, lower case, unique across the roster. */
    public String id() {
        return id;
    }

    /** Player-facing name. */
    public String displayName() {
        return displayName;
    }

    public DimensionKind dimension() {
        return dimension;
    }

    /** The human-readable situation text from the roster, for documentation only. */
    public String situation() {
        return situation;
    }

    public BiomeRule biomeRule() {
        return biomeRule;
    }

    public GroundRule groundRule() {
        return groundRule;
    }

    public PlacementKind placementKind() {
        return groundRule.kind();
    }

    public Condition condition() {
        return condition;
    }

    /** True when the plant has a dormant stage, which is every plant with a real condition. */
    public boolean hasDormantStage() {
        return condition != Condition.ALWAYS;
    }

    /** The rarity class word from the roster, for documentation only. */
    public String rarity() {
        return rarity;
    }

    /** Percent chance, rolled once per generated chunk, that placement is attempted. */
    public int chunkChancePercent() {
        return chunkChancePercent;
    }

    /** Multiplier of the pack's base regrow time (0.1 to 10): 0.5 comes back fast, 2.0 slowly. */
    public double regrowFactor() {
        return regrowFactor;
    }

    public int yieldPerPick() {
        return yieldPerPick;
    }

    /** True when the plant ships as a 3D model rather than a flat cross. */
    public boolean isThreeD() {
        return threeD;
    }

    /** Light level 0 to 15 emitted while in bloom. */
    public int glowLight() {
        return glowLight;
    }

    public boolean isShowcase() {
        return showcase;
    }

    @Override
    public String toString() {
        return "PlantDefinition{" + id + "}";
    }
}
