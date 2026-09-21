package com.mahghuuls.elusiveflora.roster;

import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stone face check on a stub world. Only the stone kind is covered here: a log is decided
 * by the block itself, which {@link StubWorld} cannot provide.
 */
class AttachRuleTest {

    private static final BlockPos PLANT = new BlockPos(10, 70, 10);

    private static GroundRule stoneRule() {
        return GroundRule.parse("ledgebloom", "side:stone");
    }

    @Test
    void thePlantPointsAwayFromTheStoneBehindIt() {
        StubWorld world = new StubWorld();
        world.put(PLANT.west(), Material.ROCK, EnumFacing.values());
        assertEquals(EnumFacing.EAST, stoneRule().facingToAttach(world.access(), PLANT));
        assertTrue(stoneRule().supportsFacing(world.access(), PLANT, EnumFacing.EAST));
        assertFalse(stoneRule().supportsFacing(world.access(), PLANT, EnumFacing.WEST),
                "pointing at the stone is not being held by it");
    }

    @Test
    void theFaceTurnedTowardThePlantMustBeSolid() {
        StubWorld world = new StubWorld();
        world.put(PLANT.north(), Material.ROCK, EnumFacing.NORTH, EnumFacing.UP);
        assertNull(stoneRule().facingToAttach(world.access(), PLANT), "only the far side is solid");
        world.put(PLANT.north(), Material.ROCK, EnumFacing.SOUTH);
        assertEquals(EnumFacing.SOUTH, stoneRule().facingToAttach(world.access(), PLANT));
    }

    @Test
    void otherMaterialsAndOtherDirectionsDoNotHold() {
        StubWorld world = new StubWorld();
        world.put(PLANT.east(), Material.GROUND, EnumFacing.values());
        world.put(PLANT.up(), Material.ROCK, EnumFacing.values());
        world.put(PLANT.down(), Material.ROCK, EnumFacing.values());
        assertNull(stoneRule().facingToAttach(world.access(), PLANT), "dirt beside, stone above and below");
    }

    @Test
    void aGroundRuleNeverAttaches() {
        StubWorld world = new StubWorld();
        world.put(PLANT.west(), Material.ROCK, EnumFacing.values());
        assertNull(GroundRule.parse("frostbell", "on:snow").facingToAttach(world.access(), PLANT));
    }
}
