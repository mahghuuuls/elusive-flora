package com.mahghuuls.elusiveflora.world;

import com.mahghuuls.elusiveflora.config.PlantSettings;
import com.mahghuuls.elusiveflora.registry.PlantRegistry;
import com.mahghuuls.elusiveflora.registry.ResolvedBiomeRule;
import com.mahghuuls.elusiveflora.roster.DimensionKind;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.season.SeasonBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

/**
 * The one answer to "may this plant be here": either yes or the first failing rule, in a fixed
 * order. The world generator and the check tool both ask this class, so they cannot disagree.
 *
 * <p>Order: disabled, dimension, biome, ground, condition. The generator stops before condition
 * because a dormant plant may be placed; the check tool reports all five.
 */
public final class PlacementCheck {

    /** Why a plant cannot be here, in the order the rules are checked. */
    public enum Reason {
        DISABLED("disabled in config"),
        DIMENSION("wrong dimension"),
        BIOME("biome does not match"),
        GROUND("ground does not match"),
        CONDITION("condition not met now");

        private final String text;

        Reason(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    /**
     * The five rules as questions answered lazily, so an early failure costs nothing for the
     * later rules. The world-facing {@link #check} builds one from a world; tests build one from
     * booleans.
     */
    interface Rules {
        boolean enabled();

        boolean dimension();

        boolean biome();

        boolean ground();

        boolean condition();
    }

    private final PlantRegistry registry;
    private final SeasonBridge seasons;

    public PlacementCheck(PlantRegistry registry, SeasonBridge seasons) {
        this.registry = registry;
        this.seasons = seasons;
    }

    /**
     * The first failing rule for placing the plant at {@code pos}, or null when every rule
     * passes. {@code includeCondition} false stops after the ground rule.
     */
    public Reason check(final World world, final BlockPos pos, final PlantDefinition plant, boolean includeCondition) {
        final PlantSettings settings = registry.settings(plant);
        return firstFailure(new Rules() {
            @Override
            public boolean enabled() {
                return settings.enabled();
            }

            @Override
            public boolean dimension() {
                return dimensionMatches(world, plant.dimension());
            }

            @Override
            public boolean biome() {
                return biomeAllows(world, pos, plant);
            }

            @Override
            public boolean ground() {
                return plant.groundRule().matches(world, pos);
            }

            @Override
            public boolean condition() {
                return plant.condition().isMet(world, pos, seasons);
            }
        }, includeCondition);
    }

    /**
     * The biome rule alone, which depends on the column and not on the height. World generation
     * asks it before it searches a column for a position, because that search is the costly part
     * and most columns of most chunks are in the wrong biome.
     */
    public boolean biomeAllows(World world, BlockPos column, PlantDefinition plant) {
        ResolvedBiomeRule biomeRule = registry.biomeRuleOf(plant);
        return biomeRule != null && biomeRule.matches(world.getBiome(column));
    }

    /** The rule order itself, world-free so it can be tested. */
    static Reason firstFailure(Rules rules, boolean includeCondition) {
        if (!rules.enabled()) {
            return Reason.DISABLED;
        }
        if (!rules.dimension()) {
            return Reason.DIMENSION;
        }
        if (!rules.biome()) {
            return Reason.BIOME;
        }
        if (!rules.ground()) {
            return Reason.GROUND;
        }
        if (includeCondition && !rules.condition()) {
            return Reason.CONDITION;
        }
        return null;
    }

    /**
     * Overworld, Nether, and End are the vanilla dimension types. The Aether is "none of those":
     * its id is configurable, so the roster relies on its biome name for the exact match.
     */
    public static boolean dimensionMatches(World world, DimensionKind kind) {
        DimensionType type = world.provider.getDimensionType();
        switch (kind) {
            case OVERWORLD:
                return type == DimensionType.OVERWORLD;
            case NETHER:
                return type == DimensionType.NETHER;
            case END:
                return type == DimensionType.THE_END;
            default:
                return type != DimensionType.OVERWORLD && type != DimensionType.NETHER
                        && type != DimensionType.THE_END;
        }
    }
}
