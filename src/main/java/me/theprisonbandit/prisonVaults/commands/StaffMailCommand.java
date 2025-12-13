package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffMailCommand implements CommandExecutor {

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

        // /staffmail read
        if (args.length == 0 || args[0].equalsIgnoreCase("read")) {
            plugin.staffMailManager.readGlobalMail(player);
            return true;
        }

        // /staffmail clear
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
}