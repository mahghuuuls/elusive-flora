package com.mahghuuls.elusiveflora.registry;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.block.BlockGroundPlant;
import com.mahghuuls.elusiveflora.block.BlockPlantBase;
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
            public double multiplier() {
                return PlantRegistry.this.config.regrowthMultiplier();
            }
        };
        for (PlantDefinition plant : roster.plants()) {
            ItemPickedPlant item = new ItemPickedPlant(plant);
            items.put(plant.id(), item);
            PlantLifecycle lifecycle = new PlantLifecycle(plant, item, seasons, scale);
            BlockPlantBase block = createBlock(plant, lifecycle);
            if (block != null) {
                blocks.put(plant.id(), block);
            }
        }
        creativeTab = new ModCreativeTab(items.values().iterator().next());
        for (ItemPickedPlant item : items.values()) {
            item.setCreativeTab(creativeTab);
        }
    }

    /**
     * The block class for a placement kind. Attached and water plants arrive with their own
     * slices; until then they have no block, are never placed, and log once.
     */
    private static BlockPlantBase createBlock(PlantDefinition plant, PlantLifecycle lifecycle) {
        switch (plant.placementKind()) {
            case GROUND:
                return new BlockGroundPlant(lifecycle);
            default:
                ElusiveFloraLog.LOGGER.info("Plant {} ({} kind) has no block class yet; not placed",
                        plant.id(), plant.placementKind());
                return null;
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
