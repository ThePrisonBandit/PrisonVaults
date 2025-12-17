package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil; // Added

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StaffMailCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public StaffMailCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!plugin.rankManager.isStaff(player)) {
            player.sendMessage(ChatColor.RED + "This command is for Staff only.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("read")) {
            plugin.staffMailManager.readGlobalMail(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!plugin.rankManager.isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "Only Admins+ can clear the staff inbox.");
                return true;
            }
            plugin.staffMailManager.clearGlobalMail(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /staffmail <read|clear>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player && plugin.rankManager.isStaff((Player) sender)) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], Arrays.asList("read", "clear"), new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }
}