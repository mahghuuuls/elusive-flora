package com.mahghuuls.elusiveflora.world;

import com.mahghuuls.elusiveflora.ElusiveFloraLog;
import com.mahghuuls.elusiveflora.block.BlockPlantBase;
import com.mahghuuls.elusiveflora.block.PlantStage;
import com.mahghuuls.elusiveflora.config.PlantSettings;
import com.mahghuuls.elusiveflora.registry.PlantRegistry;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Places plants when a chunk is generated, in every dimension. For each enabled plant whose
 * dimension matches: one roll against its chunk chance, then up to {@link #CANDIDATES} surface
 * positions inside the chunk, placing at the first one {@link PlacementCheck} accepts. At most
 * one plant of each kind per chunk.
 *
 * <p>Positions are offset by 8 like vanilla decoration, so placement never reaches into a chunk
 * that is not generated yet.
 */
public final class PlantWorldGenerator implements IWorldGenerator {

    /** Candidate positions tried per successful roll. The performance bound in the requirements. */
    static final int CANDIDATES = 16;

    private final PlantRegistry registry;
    private final PlacementCheck check;

    public PlantWorldGenerator(PlantRegistry registry, PlacementCheck check) {
        this.registry = registry;
        this.check = check;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,
                         IChunkProvider chunkProvider) {
        boolean debug = registry.debugLog();
        for (PlantDefinition plant : registry.plants()) {
            BlockPlantBase block = registry.blockOf(plant);
            if (block == null) {
                continue;
            }
            // The two cheapest rules are asked up front so a disabled or out-of-dimension plant
            // costs no candidate positions at all; the check below asks them again as the one
            // owner of the answer, at no meaningful cost.
            PlantSettings settings = registry.settings(plant);
            if (!settings.enabled() || !PlacementCheck.dimensionMatches(world, plant.dimension())) {
                continue;
            }
            if (random.nextInt(100) >= settings.chunkChancePercent()) {
                continue;
            }
            for (int attempt = 0; attempt < CANDIDATES; attempt++) {
                int x = chunkX * 16 + 8 + random.nextInt(16);
                int z = chunkZ * 16 + 8 + random.nextInt(16);
                BlockPos pos = SurfaceFinder.find(world, plant.dimension(), x, z, random);
                if (pos == null || !world.isAirBlock(pos)) {
                    continue;
                }
                if (check.check(world, pos, plant, false) != null) {
                    continue;
                }
                PlantStage stage = block.lifecycle().initialStage(world, pos);
                IBlockState state = block.getDefaultState().withProperty(BlockPlantBase.STAGE, stage);
                world.setBlockState(pos, state, 2);
                if (debug) {
                    ElusiveFloraLog.LOGGER.info("Placed {} at {} {} {} ({})", plant.id(),
                            pos.getX(), pos.getY(), pos.getZ(), stage.getName());
                }
                break;
            }
        }
    }
}
