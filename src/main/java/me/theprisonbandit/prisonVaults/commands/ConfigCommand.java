package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
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

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // --- NEW FEATURE: SET WORLD ---
        // Usage: /pvconfig setworld <worldName>
        if (args[0].equalsIgnoreCase("setworld")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pvconfig setworld <worldName>");
                return true;
            }

            String worldName = args[1];

            // 1. Validate that the world exists on the server
            if (Bukkit.getWorld(worldName) == null) {
                sender.sendMessage(ChatColor.RED + "Error: The world '" + worldName + "' does not exist!");
                // Optional: List available worlds to help the user
                StringBuilder available = new StringBuilder();
                Bukkit.getWorlds().forEach(w -> available.append(w.getName()).append(", "));
                sender.sendMessage(ChatColor.GRAY + "Available worlds: " + available.toString());
                return true;
            }

            // 2. Save to config.yml
            plugin.getConfig().set("job-world-name", worldName);
            plugin.saveConfig();

            // 3. Update the manager immediately
            plugin.jobScheduleManager.setWorldName(worldName);

            sender.sendMessage(ChatColor.GREEN + "Job Schedule world set to: " + ChatColor.YELLOW + worldName);
            if (sender instanceof Player) {
                SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
            return true;
        }

        // --- EXISTING FEATURE: SET CHANCES ---
        // Usage: /pvconfig set <rob|pickpocket> <0.0-1.0>
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /pvconfig set <rob|pickpocket> <0.0-1.0>");
                return true;
            }

            String feature = args[1].toLowerCase();
            String valueStr = args[2];
            double value;

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

        // Fallback if no valid command found
        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "1. /pvconfig set <rob|pickpocket> <0.0-1.0>");
        sender.sendMessage(ChatColor.RED + "2. /pvconfig setworld <worldName>");
    }
}