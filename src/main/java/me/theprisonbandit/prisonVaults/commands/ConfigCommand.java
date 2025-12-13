package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public ConfigCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        // Help Message
        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvconfig set <rob|pickpocket> <0.0-1.0>");
            sender.sendMessage(ChatColor.GRAY + "Example: /pvconfig set rob 0.5 (for 50% chance)");
            return true;
        }

        String feature = args[1].toLowerCase();
        String valueStr = args[2];
        double value;

        // Validate Number
        try {
            value = Double.parseDouble(valueStr);
            if (value < 0.0 || value > 1.0) {
                sender.sendMessage(ChatColor.RED + "Chance must be between 0.0 and 1.0.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
            return true;
        }

        // Apply Config Change
        if (feature.equals("rob")) {
            plugin.getConfig().set("rob.success-chance", value);
            plugin.saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Success! Robbery chance set to " + (value * 100) + "%.");
        } else if (feature.equals("pickpocket")) {
            plugin.getConfig().set("pickpocket.success-chance", value);
            plugin.saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Success! Pickpocket chance set to " + (value * 100) + "%.");
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown feature. Use 'rob' or 'pickpocket'.");
            return true;
        }

        if (sender instanceof Player) {
            SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        return true;
    }
}