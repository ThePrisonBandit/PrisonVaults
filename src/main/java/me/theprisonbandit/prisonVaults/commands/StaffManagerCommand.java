package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StaffManagerCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public StaffManagerCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /staff <serverstaff|servermember>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // --- SUBCOMMAND: SERVERSTAFF ---
        if (subCommand.equals("serverstaff")) {
            if (!player.hasPermission("prisonvaults.staff.serverstaff")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to view server staff.");
                return true;
            }
            plugin.staffManagementListener.openStaffMenu(player);
            return true;
        }

        // --- SUBCOMMAND: SERVERMEMBER ---
        if (subCommand.equals("servermember")) {
            if (!player.hasPermission("prisonvaults.staff.servermember")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to view server members.");
                return true;
            }
            plugin.staffManagementListener.openMemberMenu(player);
            return true;
        }

        // --- ARGS: Specific Player (Direct Lookup) ---
        if (player.hasPermission("prisonvaults.staff.manage")) {
            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player '" + targetName + "' has never played on this server.");
                return true;
            }

            plugin.staffManagementListener.openSpecificEditor(player, target);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /staff <serverstaff|servermember>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("serverstaff", "servermember"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}