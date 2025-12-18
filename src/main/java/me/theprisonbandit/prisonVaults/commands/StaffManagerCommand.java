package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffManagerCommand implements CommandExecutor {

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

        if (!player.hasPermission("prisonvaults.staff.manage")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (plugin.staffManagementListener == null) {
            player.sendMessage(ChatColor.RED + "Error: Listener not initialized.");
            return true;
        }

        // NO ARGS: Open Main Menu
        if (args.length == 0) {
            plugin.staffManagementListener.openStaffMainMenu(player);
            return true;
        }

        // ARGS: Try to find specific offline/online player
        if (args.length == 1) {
            String targetName = args[0];
            // Get offline player (Works for online too)
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // Safety Check: Have they ever played?
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player '" + targetName + "' has never played on this server.");
                return true;
            }

            // Open Specific Editor
            plugin.staffManagementListener.openSpecificEditor(player, target);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /staff [player]");
        return true;
    }
}