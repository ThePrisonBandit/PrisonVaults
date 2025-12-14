package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class InfoCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public InfoCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginDescriptionFile pdf = plugin.getDescription();

        sender.sendMessage(ChatColor.DARK_GRAY + "----------------[" + ChatColor.GOLD + " PrisonVaults Info " + ChatColor.DARK_GRAY + "]----------------");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.AQUA + pdf.getVersion());
        sender.sendMessage(ChatColor.GRAY + "Authors: " + ChatColor.YELLOW + String.join(", ", pdf.getAuthors()));
        sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + pdf.getDescription());
        sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Active (1.21)");

        // Show update status if available
        if (plugin.updateChecker != null) { // We will add public access to this field in main class
            sender.sendMessage(ChatColor.GRAY + "Update Checker: " + ChatColor.GREEN + "Active");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------------");
        return true;
    }
}