package com.mahghuuls.elusiveflora.roster;

import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The water bed rule on a stub world: the floor, the water column, and the drained case. */
class WaterRuleTest {

    private static final BlockPos BED = new BlockPos(0, 40, 0);
    private static final BlockPos PLANT = BED.up();

    private static GroundRule deep() {
        return GroundRule.parse("pearlfrond", "water_bed:8+");
    }

    private static GroundRule shallow() {
        return GroundRule.parse("streamreed", "water_bed:2-4");
    }

    /** A sand floor with a water column of the given height standing on it. */
    private static StubWorld lake(int depth) {
        StubWorld world = new StubWorld();
        world.put(BED, Material.SAND, EnumFacing.UP);
        world.fillUp(PLANT, depth, Material.WATER);
        return world;
    }

    @Test
    void theDeepRuleNeedsEightBlocksOfWaterCountingThePlantsOwn() {
        assertFalse(deep().matchesWaterBed(lake(7).access(), PLANT));
        assertTrue(deep().matchesWaterBed(lake(8).access(), PLANT));
        assertTrue(deep().matchesWaterBed(lake(40).access(), PLANT));
    }

    @Test
    void theShallowRuleHasBothEnds() {
        assertFalse(shallow().matchesWaterBed(lake(1).access(), PLANT), "a plant at the surface leaves a hole in it");
        assertTrue(shallow().matchesWaterBed(lake(2).access(), PLANT));
        assertTrue(shallow().matchesWaterBed(lake(4).access(), PLANT));
        assertFalse(shallow().matchesWaterBed(lake(5).access(), PLANT));
    }

    @Test
    void theFloorMustBeANaturalBedWithASolidTop() {
        for (Material floor : new Material[] {Material.SAND, Material.GROUND, Material.CLAY, Material.ROCK}) {
            StubWorld world = lake(3);
            world.put(BED, floor, EnumFacing.UP);
            assertTrue(shallow().matchesWaterBed(world.access(), PLANT), floor.toString());
        }
        StubWorld onWood = lake(3);
        onWood.put(BED, Material.WOOD, EnumFacing.UP);
        assertFalse(shallow().matchesWaterBed(onWood.access(), PLANT), "a sunken boat hull is not a bed");
        StubWorld onSlab = lake(3);
        onSlab.put(BED, Material.ROCK, EnumFacing.DOWN);
        assertFalse(shallow().matchesWaterBed(onSlab.access(), PLANT), "the top face must be solid");
    }

    /** Ice or a pillar above changes the depth but must not uproot a plant that already stands. */
    @Test
    void stayingDoesNotDependOnDepth() {
        StubWorld world = lake(8);
        world.put(PLANT.up(3), Material.ICE);
        assertFalse(deep().matchesWaterBed(world.access(), PLANT), "too shallow to appear now");
        assertTrue(deep().holdsInWater(world.access(), PLANT), "but deep enough to stay");
    }

    @Test
    void aDrainedPlaceHoldsNothingAndIsNotRefilled() {
        StubWorld drained = new StubWorld();
        drained.put(BED, Material.SAND, EnumFacing.UP);
        drained.put(PLANT, Material.WATER);
        assertFalse(GroundRule.hasWaterBeside(drained.access(), PLANT), "its own block does not count");
        assertFalse(deep().holdsInWater(drained.access(), PLANT));
        drained.put(PLANT.east(), Material.WATER);
        assertTrue(GroundRule.hasWaterBeside(drained.access(), PLANT));
        assertTrue(deep().holdsInWater(drained.access(), PLANT));
        StubWorld waterBelowOnly = new StubWorld();
        waterBelowOnly.put(PLANT.down(), Material.WATER);
        assertFalse(GroundRule.hasWaterBeside(waterBelowOnly.access(), PLANT));
    }

    @Test
    void aDryPositionBesideWaterIsNotInWater() {
        StubWorld world = lake(8);
        BlockPos shore = PLANT.west();
        world.put(shore.down(), Material.SAND, EnumFacing.UP);
        assertFalse(deep().matchesWaterBed(world.access(), shore), "air with a lake beside it has depth 0");
        assertFalse(shallow().matchesWaterBed(world.access(), shore));
    }

    @Test
    void theBedGoneMeansThePlantGoes() {
        StubWorld world = lake(8);
        world.put(BED, Material.WATER);
        assertFalse(deep().holdsInWater(world.access(), PLANT));
    }

    @Test
    void otherKindsNeverHoldInWater() {
        assertFalse(GroundRule.parse("frostbell", "on:snow").holdsInWater(lake(8).access(), PLANT));
        assertFalse(GroundRule.parse("ledgebloom", "side:stone").matchesWaterBed(lake(8).access(), PLANT));
    }
}
