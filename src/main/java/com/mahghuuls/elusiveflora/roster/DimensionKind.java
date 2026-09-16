package com.mahghuuls.elusiveflora.roster;

import java.util.Locale;

/**
 * The world a plant belongs to, from the roster's {@code dimension} column.
 *
 * <p>The first three map to the vanilla dimension types. {@link #AETHER} means "none of those
 * three": the Aether is a modded dimension whose id is configurable, so the roster relies on the
 * exact biome name to place plants there and only excludes the vanilla dimensions by kind.
 */
public enum DimensionKind {
    OVERWORLD,
    NETHER,
    END,
    AETHER;

    static DimensionKind parse(String rowId, String text) {
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RosterException(rowId, "dimension", "unknown dimension '" + text + "'");
        }
    }
}
