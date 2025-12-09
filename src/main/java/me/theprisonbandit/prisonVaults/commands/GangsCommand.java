package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.gangs.Gang; // Import the standalone Gang class
import me.theprisonbandit.prisonVaults.gangs.GangManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GangsCommand implements CommandExecutor {

    private final GangManager gangManager;

    public GangsCommand(GangManager gangManager) {
        this.gangManager = gangManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gangs")) {
            sender.sendMessage(ChatColor.GOLD + "--- Current Gangs ---");

            // FIX: Use 'Gang' not 'GangManager.Gang'
            // FIX: Ensure GangManager has the getAllGangs() method I provided earlier
            for (Gang gang : gangManager.getAllGangs()) {

                String color = gang.getColor();
                if (color == null) color = "&f";

                sender.sendMessage(ChatColor.YELLOW + gang.getName() +
                        ChatColor.GRAY + " [" +
                        ChatColor.translateAlternateColorCodes('&', color + gang.getTag()) +
                        ChatColor.GRAY + "]");
            }

            sender.sendMessage(ChatColor.GOLD + "---------------------");
            return true;
        }
        return false;
    }
}