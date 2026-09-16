package com.mahghuuls.elusiveflora.roster;

/**
 * A defect in the bundled plant roster. The roster ships inside the jar, so a bad row is a build
 * mistake, not a user error: the mod fails at construction with the row and column named instead
 * of loading a partial plant list.
 */
public final class RosterException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public RosterException(String rowId, String column, String problem) {
        super("plants.csv row '" + rowId + "', column '" + column + "': " + problem);
    }

    public RosterException(String problem) {
        super("plants.csv: " + problem);
    }
}
