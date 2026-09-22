package com.mahghuuls.elusiveflora.client.journal;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.ElusiveFloraMod;
import com.mahghuuls.elusiveflora.registry.PlantRegistry;
import com.mahghuuls.elusiveflora.registry.ResolvedBiomeRule;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariableProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Fills the three plant page templates. A page names its plant in the {@code plant} variable; this
 * class answers the templates' {@code #condition} and {@code #biomes} from the roster and the
 * resolved biome rules, so a page never repeats what the roster and the config already say.
 * Patchouli creates one instance per template page through reflection, by the class name in the
 * template, and calls {@link #setup} each time it builds the book.
 */
public class PlantEntryProcessor implements IComponentProcessor {

    private PlantDefinition plant;

    @Override
    public void setup(IVariableProvider<String> variables) {
        String id = variables.has("plant") ? variables.get("plant") : "";
        PlantRegistry registry = ElusiveFloraMod.registry();
        if (registry == null) {
            // Not reached in the normal load order (Patchouli builds books after every mod has
            // loaded), kept so a build from an unexpected reload shows an empty line, not a crash.
            plant = null;
            return;
        }
        plant = registry.byId(id);
        if (plant == null) {
            ElusiveFloraLog.warnOnce("journal.plant." + id, "Journal entry names an unknown plant '" + id + "'");
        }
    }

    @Override
    public String process(String key) {
        if (plant == null) {
            return null;
        }
        if (key.equals("condition")) {
            return JournalText.condition(plant.condition());
        }
        if (key.equals("biomes")) {
            ResolvedBiomeRule rule = ElusiveFloraMod.registry().biomeRuleOf(plant);
            return JournalText.biomes(installedBiomeNames(rule), rule != null, rule != null && rule.anyInstalled());
        }
        return null;
    }

    /** Readable names of every registered biome the rule accepts, sorted and without repeats. */
    private static List<String> installedBiomeNames(ResolvedBiomeRule rule) {
        List<String> names = new ArrayList<String>();
        if (rule == null) {
            return names;
        }
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (rule.matches(biome)) {
                String name = biome.getBiomeName();
                names.add(name == null || name.isEmpty() ? String.valueOf(biome.getRegistryName()) : JournalText.readable(name));
            }
        }
        Collections.sort(names);
        return new ArrayList<String>(new LinkedHashSet<String>(names));
    }
}
