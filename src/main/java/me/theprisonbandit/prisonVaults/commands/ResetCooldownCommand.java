package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResetCooldownCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public ResetCooldownCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin.resetcooldown")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        // 1. Reset Kit Cooldowns
        if (plugin.kitManager != null) {
            plugin.kitManager.resetAllCooldowns();
        }

        // 2. Reset General Cooldowns (Robbery, etc.)
        if (plugin.cooldownManager != null) {
            plugin.cooldownManager.resetAllCooldowns();
        }

        // 3. Reset Job Cooldowns (if any exist)
        if (plugin.jobManager != null) {
            // plugin.jobManager.resetCooldowns(); // implement if needed
        }

        sender.sendMessage(ChatColor.GREEN + "All server cooldowns (Kits, Robbery, etc.) have been reset for everyone.");
        return true;
    }
}