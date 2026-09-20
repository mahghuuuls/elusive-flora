package com.mahghuuls.elusiveflora.command;

import com.mahghuuls.elusiveflora.config.PlantSettings;
import com.mahghuuls.elusiveflora.registry.PlantRegistry;
import com.mahghuuls.elusiveflora.registry.ResolvedBiomeRule;
import com.mahghuuls.elusiveflora.roster.PlantDefinition;
import com.mahghuuls.elusiveflora.world.PlacementCheck;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

/**
 * The check tool: an operator command that says, for every plant, whether it could appear where
 * the player stands and otherwise the first failing rule, plus a listing of the effective
 * settings. Registered only when the pack maker enables it in config. It reads; it never places
 * or changes anything.
 *
 * <p>The position checked is the block the player's feet occupy, so the ground rule looks at the
 * block stood on. The five rules are the whole answer; the generator additionally needs that
 * position to be free (air, or a thin snow layer it replaces), so an operator standing in tall
 * grass or water may read "can appear here" for a spot generation would skip. Stand on a bare
 * block to ask the exact question generation asks.
 */
public final class CommandElusiveFlora extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList("here", "list");

    private final PlantRegistry registry;
    private final PlacementCheck check;

    public CommandElusiveFlora(PlantRegistry registry, PlacementCheck check) {
        this.registry = registry;
        this.check = check;
    }

    @Override
    public String getName() {
        return "elusiveflora";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/elusiveflora <here|list>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (args[0].equals("here")) {
            here(sender);
        } else if (args[0].equals("list")) {
            list(sender);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, SUBCOMMANDS) : super.getTabCompletions(server, sender, args, targetPos);
    }

    /** "here" needs a player's position; the console and command blocks are refused plainly. */
    private void here(ICommandSender sender) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.getEntityWorld();
        BlockPos pos = player.getPosition();
        sender.sendMessage(new TextComponentString("Elusive Flora at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " in " + biomeName(world, pos) + ":"));
        for (PlantDefinition plant : registry.plants()) {
            if (registry.blockOf(plant) == null) {
                sender.sendMessage(new TextComponentString(plant.displayName() + ": not placeable yet (no block class)"));
                continue;
            }
            sender.sendMessage(new TextComponentString(lineFor(plant.displayName(), check.check(world, pos, plant, true))));
        }
    }

    private void list(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("Elusive Flora plants and effective settings:"));
        for (PlantDefinition plant : registry.plants()) {
            PlantSettings settings = registry.settings(plant);
            ResolvedBiomeRule rule = registry.biomeRuleOf(plant);
            String installed = installedText(rule != null, rule != null && rule.anyInstalled());
            sender.sendMessage(new TextComponentString(listLine(plant, settings, installed)));
        }
    }

    /** One line of the "here" report. Package-private for tests. */
    static String lineFor(String displayName, PlacementCheck.Reason reason) {
        return displayName + ": " + (reason == null ? "can appear here" : reason.text());
    }

    /** The "installed" suffix of a "list" line. Package-private for tests. */
    static String installedText(boolean resolved, boolean anyInstalled) {
        if (!resolved) {
            return "biomes not resolved yet";
        }
        return anyInstalled ? "matches installed biomes" : "matches no installed biome";
    }

    /** One line of the "list" report. Package-private for tests. */
    static String listLine(PlantDefinition plant, PlantSettings settings, String installed) {
        return plant.displayName() + " (" + plant.id() + "): "
                + (settings.enabled() ? "enabled" : "disabled")
                + ", chunk chance " + settings.chunkChancePercent() + " percent"
                + ", biome types " + join(settings.biomeRule().typeNames())
                + ", biome names " + join(settings.biomeRule().biomeNames())
                + ", " + installed;
    }

    private static String join(Iterable<String> values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(value);
        }
        return text.length() == 0 ? "none" : text.toString();
    }

    private static String biomeName(World world, BlockPos pos) {
        return String.valueOf(world.getBiome(pos).getRegistryName());
    }
}
