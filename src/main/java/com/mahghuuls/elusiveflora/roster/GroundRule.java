package com.mahghuuls.elusiveflora.roster;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a plant may sit, parsed from the roster's {@code ground} column. This class is the one
 * owner of the ground grammar and of the meaning of each keyword.
 *
 * <p>Grammar: {@code ;}-separated clauses. Exactly one placement clause:
 * <ul>
 *   <li>{@code on:<keyword>|<keyword>...} sits on top of a block of a listed kind;</li>
 *   <li>{@code side:stone} or {@code side:log} attaches to a side face;</li>
 *   <li>{@code water_bed:<min>-<max>} or {@code water_bed:<min>+} sits in water above the bed,
 *       with that many water blocks above the bed.</li>
 * </ul>
 * Optional modifiers: {@code near_lava}, {@code near_water}, {@code sea_level}, {@code min_y:<n>}.
 *
 * <p>Ground keywords are vanilla block paths ({@code grass}, {@code dirt}, {@code sand},
 * {@code snow}, {@code snow_layer}, {@code mycelium}, {@code soul_sand}, {@code netherrack},
 * {@code end_stone}, {@code stone}), a full registry name such as
 * {@code aether_legacy:aether_grass}, or {@code any_solid}. {@code dirt} covers dirt, coarse dirt,
 * and podzol; {@code stone} covers every stone variant.
 *
 * <p>Callers ask this class whether a position qualifies; they do not interpret the parsed fields
 * themselves. The world-facing check that applies the keywords and modifiers to blocks belongs
 * here too and is added with the first block slice. The getters below exist for tests, diagnostics,
 * and the check tool's reason text, not for re-implementing the rule elsewhere.
 */
public final class GroundRule {

    /** Side materials a plant may attach to. */
    public enum SideKind { STONE, LOG }

    /** Keyword meaning "any full opaque block". */
    public static final String ANY_SOLID = "any_solid";

    private static final Set<String> VANILLA_GROUND = new HashSet<String>(Arrays.asList(
            "grass", "dirt", "sand", "snow", "snow_layer", "mycelium", "soul_sand",
            "netherrack", "end_stone", "stone", ANY_SOLID));

    private final PlacementKind kind;
    private final List<String> groundKeywords;
    private final SideKind sideKind;
    private final int minDepth;
    private final int maxDepth;
    private final boolean nearLava;
    private final boolean nearWater;
    private final boolean seaLevel;
    private final int minY;

    /** Sentinel for "no maximum depth". */
    public static final int UNBOUNDED = Integer.MAX_VALUE;

    /** Sentinel for "no minimum height". */
    public static final int NO_MIN_Y = Integer.MIN_VALUE;

    private GroundRule(PlacementKind kind, List<String> groundKeywords, SideKind sideKind,
                       int minDepth, int maxDepth, boolean nearLava, boolean nearWater,
                       boolean seaLevel, int minY) {
        this.kind = kind;
        this.groundKeywords = Collections.unmodifiableList(groundKeywords);
        this.sideKind = sideKind;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.nearLava = nearLava;
        this.nearWater = nearWater;
        this.seaLevel = seaLevel;
        this.minY = minY;
    }

    static GroundRule parse(String rowId, String column) {
        PlacementKind kind = null;
        List<String> keywords = Collections.emptyList();
        SideKind side = null;
        int minDepth = 0;
        int maxDepth = UNBOUNDED;
        boolean nearLava = false;
        boolean nearWater = false;
        boolean seaLevel = false;
        int minY = NO_MIN_Y;

        String text = column.trim();
        if (text.isEmpty()) {
            throw new RosterException(rowId, "ground", "empty");
        }
        for (String rawClause : text.split(";", -1)) {
            String clause = rawClause.trim();
            if (clause.isEmpty()) {
                throw new RosterException(rowId, "ground", "empty clause (a stray ';')");
            }
            if (clause.startsWith("on:")) {
                kind = requireSinglePlacement(rowId, kind, PlacementKind.GROUND);
                keywords = parseGroundKeywords(rowId, clause.substring(3));
            } else if (clause.startsWith("side:")) {
                kind = requireSinglePlacement(rowId, kind, PlacementKind.ATTACHED);
                side = parseSide(rowId, clause.substring(5));
            } else if (clause.startsWith("water_bed:")) {
                kind = requireSinglePlacement(rowId, kind, PlacementKind.WATER);
                int[] range = parseDepthRange(rowId, clause.substring(10));
                minDepth = range[0];
                maxDepth = range[1];
            } else if (clause.equals("near_lava")) {
                nearLava = true;
            } else if (clause.equals("near_water")) {
                nearWater = true;
            } else if (clause.equals("sea_level")) {
                seaLevel = true;
            } else if (clause.startsWith("min_y:")) {
                minY = parseInt(rowId, "min_y", clause.substring(6));
            } else {
                throw new RosterException(rowId, "ground", "unknown clause '" + clause + "'");
            }
        }
        if (kind == null) {
            throw new RosterException(rowId, "ground",
                    "no placement clause (expected on:, side:, or water_bed:)");
        }
        return new GroundRule(kind, keywords, side, minDepth, maxDepth, nearLava, nearWater,
                seaLevel, minY);
    }

    private static PlacementKind requireSinglePlacement(String rowId, PlacementKind existing,
                                                        PlacementKind found) {
        if (existing != null) {
            throw new RosterException(rowId, "ground", "more than one placement clause");
        }
        return found;
    }

    private static List<String> parseGroundKeywords(String rowId, String list) {
        String[] parts = BiomeRule.split(list);
        if (parts.length == 0) {
            throw new RosterException(rowId, "ground", "on: needs at least one keyword");
        }
        for (String keyword : parts) {
            if (keyword.isEmpty()) {
                throw new RosterException(rowId, "ground", "empty keyword in the on: list");
            }
            int colon = keyword.indexOf(':');
            boolean registryName = colon > 0 && colon < keyword.length() - 1;
            if (!registryName && !VANILLA_GROUND.contains(keyword)) {
                throw new RosterException(rowId, "ground", "unknown ground keyword '" + keyword + "'");
            }
        }
        return Arrays.asList(parts);
    }

    private static SideKind parseSide(String rowId, String text) {
        String value = text.trim();
        if (value.equals("stone")) {
            return SideKind.STONE;
        }
        if (value.equals("log")) {
            return SideKind.LOG;
        }
        throw new RosterException(rowId, "ground", "unknown side kind '" + value + "'");
    }

    /** Parses {@code 8+} or {@code 1-4} into {min, max}, with max {@link #UNBOUNDED} for {@code +}. */
    private static int[] parseDepthRange(String rowId, String text) {
        String value = text.trim();
        if (value.endsWith("+")) {
            int min = parseInt(rowId, "water_bed", value.substring(0, value.length() - 1));
            if (min < 1) {
                throw new RosterException(rowId, "ground", "water_bed range '" + value + "' is not valid");
            }
            return new int[] {min, UNBOUNDED};
        }
        int dash = value.indexOf('-');
        if (dash <= 0) {
            throw new RosterException(rowId, "ground",
                    "water_bed range '" + value + "' must be <min>-<max> or <min>+");
        }
        int min = parseInt(rowId, "water_bed", value.substring(0, dash));
        int max = parseInt(rowId, "water_bed", value.substring(dash + 1));
        if (min < 1 || max < min) {
            throw new RosterException(rowId, "ground", "water_bed range '" + value + "' is not valid");
        }
        return new int[] {min, max};
    }

    private static int parseInt(String rowId, String what, String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new RosterException(rowId, "ground", what + " needs a whole number, got '" + text + "'");
        }
    }

    public PlacementKind kind() {
        return kind;
    }

    /** Ground keywords for an {@code on:} rule; empty for the other kinds. */
    public List<String> groundKeywords() {
        return groundKeywords;
    }

    /** Side material for a {@code side:} rule; null for the other kinds. */
    public SideKind sideKind() {
        return sideKind;
    }

    /** Minimum water blocks above the bed for a {@code water_bed:} rule; 0 otherwise. */
    public int minDepth() {
        return minDepth;
    }

    /** Maximum water blocks above the bed, or {@link #UNBOUNDED}. */
    public int maxDepth() {
        return maxDepth;
    }

    public boolean nearLava() {
        return nearLava;
    }

    public boolean nearWater() {
        return nearWater;
    }

    public boolean seaLevel() {
        return seaLevel;
    }

    /** Minimum plant height, or {@link #NO_MIN_Y}. */
    public int minY() {
        return minY;
    }

    /** True when the rule accepts a water column of this many blocks above the bed. */
    public boolean acceptsDepth(int depth) {
        return depth >= minDepth && depth <= maxDepth;
    }

    /**
     * Whether a plant of this rule may stand at {@code plantPos} right now. This is the one
     * world-facing owner of the ground grammar: the generator asks it before placing, the block
     * asks it to decide whether it may stay after a neighbor changes.
     *
     * <p>Ground rules ({@code on:}) look at the block below the plant and at the modifiers. The
     * attached and water kinds are answered by their own slices; until then they never match.
     */
    public boolean matches(World world, BlockPos plantPos) {
        switch (kind) {
            case GROUND:
                return matchesGround(world, plantPos);
            default:
                return false;
        }
    }

    private boolean matchesGround(World world, BlockPos plantPos) {
        BlockPos below = plantPos.down();
        IBlockState ground = world.getBlockState(below);
        if (!groundMatches(world, below, ground)) {
            return false;
        }
        if (minY != NO_MIN_Y && plantPos.getY() < minY) {
            return false;
        }
        // Beach sand sits at the water's top block, one below the sea level value.
        if (seaLevel && Math.abs(below.getY() - (world.getSeaLevel() - 1)) > 1) {
            return false;
        }
        if (nearLava && !hasHorizontalNeighbor(world, below, Material.LAVA)) {
            return false;
        }
        if (nearWater && !hasHorizontalNeighbor(world, below, Material.WATER)) {
            return false;
        }
        return true;
    }

    private boolean groundMatches(World world, BlockPos below, IBlockState ground) {
        Block block = ground.getBlock();
        for (String keyword : groundKeywords) {
            if (keyword.equals(ANY_SOLID)) {
                if (ground.getMaterial().isSolid() && ground.isSideSolid(world, below, EnumFacing.UP)) {
                    return true;
                }
            } else if (block == blockFor(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The block a ground keyword names. Vanilla keywords cover their variants ({@code dirt} is
     * dirt, coarse dirt, and podzol; {@code stone} is every stone variant) because those are one
     * block with metadata. A registry name resolves through the registry and is null when the
     * mod that owns it is absent, so it never matches.
     */
    private static Block blockFor(String keyword) {
        switch (keyword) {
            case "grass": return Blocks.GRASS;
            case "dirt": return Blocks.DIRT;
            case "sand": return Blocks.SAND;
            case "snow": return Blocks.SNOW;
            case "snow_layer": return Blocks.SNOW_LAYER;
            case "mycelium": return Blocks.MYCELIUM;
            case "soul_sand": return Blocks.SOUL_SAND;
            case "netherrack": return Blocks.NETHERRACK;
            case "end_stone": return Blocks.END_STONE;
            case "stone": return Blocks.STONE;
            default:
                ResourceLocation name = new ResourceLocation(keyword);
                return Block.REGISTRY.containsKey(name) ? Block.REGISTRY.getObject(name) : null;
        }
    }

    private static boolean hasHorizontalNeighbor(World world, BlockPos pos, Material material) {
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            if (world.getBlockState(pos.offset(side)).getMaterial() == material) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "GroundRule{" + kind + ", on=" + groundKeywords + ", side=" + sideKind
                + ", depth=" + minDepth + ".." + (maxDepth == UNBOUNDED ? "+" : String.valueOf(maxDepth))
                + ", nearLava=" + nearLava + ", nearWater=" + nearWater + ", seaLevel=" + seaLevel
                + ", minY=" + (minY == NO_MIN_Y ? "none" : String.valueOf(minY)) + "}";
    }
}
