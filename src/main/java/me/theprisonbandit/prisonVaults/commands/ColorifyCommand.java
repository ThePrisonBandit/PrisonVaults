package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ColorifyCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public ColorifyCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Ensure sender is a Player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // 2. Permission Check
        if (!player.hasPermission("prisonvaults.colorify")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        // 3. Validate Args
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /colorify <blurple|lemonlime|redink|rainbow|reset>");
            return true;
        }

        String preset = args[0].toLowerCase();

        // 4. Save to File
        File file = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (preset.equals("reset")) {
            data.set("chat-color", null); // Remove color
            player.sendMessage(ChatColor.GREEN + "Reset your name color.");
        } else {
            // Validate preset name
            if (!preset.matches("blurple|lemonlime|redink|rainbow")) {
                player.sendMessage(ChatColor.RED + "Invalid preset. Use: blurple, lemonlime, redink, rainbow");
                return true;
            }
            data.set("chat-color", preset);
            player.sendMessage(ChatColor.GREEN + "Set your name color to " + preset + "!");
        }

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- THE FIX: Update the Scoreboard Immediately ---
        plugin.scoreboardManager.setScoreboard(player);

        return true;
    }
}