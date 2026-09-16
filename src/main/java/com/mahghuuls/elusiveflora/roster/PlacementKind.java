package com.mahghuuls.elusiveflora.roster;

/**
 * How a plant occupies its position, derived from the placement clause of the ground rule. Each
 * kind is one block class, because Minecraft 1.12.2 fixes material and state properties per class.
 */
public enum PlacementKind {
    /** Sits on top of a ground block. */
    GROUND,
    /** Attaches to the side face of a stone or log block. */
    ATTACHED,
    /** Occupies a water block above a sea or river bed. */
    WATER
}
