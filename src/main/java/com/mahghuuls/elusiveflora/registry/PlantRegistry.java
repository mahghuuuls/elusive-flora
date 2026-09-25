package com.mahghuuls.elusiveflora.registry;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.block.BlockAttachedPlant;
import com.mahghuuls.elusiveflora.block.BlockGroundPlant;
import com.mahghuuls.elusiveflora.block.BlockPlantBase;
import com.mahghuuls.elusiveflora.block.BlockWaterPlant;
import com.mahghuuls.elusiveflora.block.PlantLifecycle;
import com.mahghuuls.elusiveflora.config.ElusiveFloraConfig;
import com.mahghuuls.elusiveflora.config.PlantSettings;
import com.mahghuuls.elusiveflora.item.ItemPickedPlant;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.roster.PlantRoster;
import com.mahghuuls.elusiveflora.season.SeasonBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every plant as the running game knows it: its definition, its effective settings, its block,
 * and its item. Built once at pre-initialization; biome rules are resolved at post-initialization
 * when every mod's biomes exist. The only place that decides which block class a placement kind
 * gets.
 */
public final class PlantRegistry {

    private final PlantRoster roster;
    private final ElusiveFloraConfig config;
    private final Map<String, BlockPlantBase> blocks = new LinkedHashMap<String, BlockPlantBase>();
    private final Map<String, ItemPickedPlant> items = new LinkedHashMap<String, ItemPickedPlant>();
    private final Map<String, ResolvedBiomeRule> biomeRules = new LinkedHashMap<String, ResolvedBiomeRule>();
    private final ModCreativeTab creativeTab;

    public PlantRegistry(PlantRoster roster, ElusiveFloraConfig config, SeasonBridge seasons) {
        this.roster = roster;
        this.config = config;
        PlantLifecycle.RegrowthScale scale = new PlantLifecycle.RegrowthScale() {
            @Override
            public int baseMinutes() {
                return PlantRegistry.this.config.regrowBaseMinutes();
            }

            @Override
            public double factor(String plantId) {
                return PlantRegistry.this.config.plant(plantId).regrowFactor();
            }
        };
        for (PlantDefinition plant : roster.plants()) {
            ItemPickedPlant item = new ItemPickedPlant(plant);
            items.put(plant.id(), item);
            PlantLifecycle lifecycle = new PlantLifecycle(plant, item, seasons, scale);
            blocks.put(plant.id(), createBlock(plant, lifecycle));
        }
        creativeTab = new ModCreativeTab(items.values().iterator().next());
        for (ItemPickedPlant item : items.values()) {
            item.setCreativeTab(creativeTab);
        }
    }

    /**
     * The block class for a placement kind. Material and state properties are fixed per block
     * class in this game version, which is why there are three.
     */
    private static BlockPlantBase createBlock(PlantDefinition plant, PlantLifecycle lifecycle) {
        switch (plant.placementKind()) {
            case GROUND:
                return new BlockGroundPlant(lifecycle);
            case ATTACHED:
                return new BlockAttachedPlant(lifecycle);
            case WATER:
                return new BlockWaterPlant(lifecycle);
            default:
                throw new IllegalStateException("no block class for " + plant.placementKind());
        }
    }

    /** Resolves every plant's effective biome rule against the live registries. */
    public void resolveBiomes() {
        for (PlantDefinition plant : roster.plants()) {
            PlantSettings settings = config.plant(plant.id());
            ResolvedBiomeRule resolved = ResolvedBiomeRule.resolve(plant.id(), settings.biomeRule());
            biomeRules.put(plant.id(), resolved);
            if (!resolved.anyInstalled()) {
                ElusiveFloraLog.LOGGER.info("Plant {}: no installed biome matches its rules; it will not generate",
                        plant.id());
            }
        }
    }

    public List<PlantDefinition> plants() {
        return roster.plants();
    }

    public PlantDefinition byId(String id) {
        return roster.byId(id);
    }

    public PlantSettings settings(PlantDefinition plant) {
        return config.plant(plant.id());
    }

    /** The plant's block, or null for a placement kind without a block class yet. */
    public BlockPlantBase blockOf(PlantDefinition plant) {
        return blocks.get(plant.id());
    }

    public ItemPickedPlant itemOf(PlantDefinition plant) {
        return items.get(plant.id());
    }

    /** The effective regrow time of a plant in real minutes: the base times the plant's factor, rounded. */
    public int regrowMinutesOf(PlantDefinition plant) {
        return (int) Math.round(config.regrowBaseMinutes() * config.plant(plant.id()).regrowFactor());
    }

    /** Null before {@link #resolveBiomes()} has run. */
    public ResolvedBiomeRule biomeRuleOf(PlantDefinition plant) {
        return biomeRules.get(plant.id());
    }

    public List<BlockPlantBase> blocks() {
        return Collections.unmodifiableList(new ArrayList<BlockPlantBase>(blocks.values()));
    }

    public List<ItemPickedPlant> items() {
        return Collections.unmodifiableList(new ArrayList<ItemPickedPlant>(items.values()));
    }

    /** Whether world generation logs each placement. */
    public boolean debugLog() {
        return config.debugLog();
    }

    /** Whether the check command is registered. */
    public boolean enableCheckCommand() {
        return config.enableCheckCommand();
    }
}
