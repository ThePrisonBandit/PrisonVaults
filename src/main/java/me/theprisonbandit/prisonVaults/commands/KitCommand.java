package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.kits.Kit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KitCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public KitCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // --- /kits (List Command) ---
        if (label.equalsIgnoreCase("kits") || args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "--- Available Kits ---");

            List<Kit> allKits = new ArrayList<>(plugin.kitManager.getAllKits());
            Kit starterKit = null;
            List<Kit> rankKits = new ArrayList<>();
            List<Kit> customKits = new ArrayList<>();

            for (Kit k : allKits) {
                if (k.getName().equalsIgnoreCase("Starter")) {
                    starterKit = k;
                } else if (k.getName().matches("(?i)Rank[A-Z]")) {
                    rankKits.add(k);
                } else {
                    customKits.add(k);
                }
            }

            rankKits.sort(Comparator.comparing(Kit::getName));
            customKits.sort(Comparator.comparing(Kit::getName));

            if (starterKit != null) printKit(player, starterKit);

            if (!rankKits.isEmpty()) {
                player.sendMessage(ChatColor.DARK_GRAY + "--- Ranks ---");
                for (Kit k : rankKits) printKit(player, k);
            }

            if (!customKits.isEmpty()) {
                player.sendMessage(ChatColor.DARK_GRAY + "--- Custom Kits ---");
                for (Kit k : customKits) printKit(player, k);
            }

            return true;
        }

        // --- /kit <name> (Give Command) ---
        String kitName = args[0];
        Kit kit = plugin.kitManager.getKit(kitName);

        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit not found.");
            return true;
        }

        if (!player.hasPermission(kit.getPermission())) {
            player.sendMessage(ChatColor.RED + "You do not have permission for this kit.");
            return true;
        }

        // --- 1. COOLDOWN CHECK ---
        String cooldownKey = "kit_" + kit.getName().toLowerCase();

        // Bypass permission check (optional, good for admins)
        if (!player.hasPermission("prisonvaults.bypass.cooldown")) {
            if (plugin.cooldownManager.isOnCooldown(player.getUniqueId(), cooldownKey)) {
                long remaining = plugin.cooldownManager.getRemainingTime(player.getUniqueId(), cooldownKey);
                player.sendMessage(ChatColor.RED + "You must wait " + formatTime(remaining) + " before using this kit again.");
                return true;
            }
        }

        // --- 2. GIVE KIT ---
        plugin.kitManager.giveKit(player, kitName);

        // --- 3. SET COOLDOWN (1 Day = 86400 Seconds) ---
        // Only set cooldown if they don't have bypass permission
        if (!player.hasPermission("prisonvaults.bypass.cooldown")) {
            plugin.cooldownManager.setCooldown(player.getUniqueId(), cooldownKey, 86400);
        }

        return true;
    }

    // Helper method to print kit status with Cooldown check visually
    private void printKit(Player player, Kit kit) {
        String cooldownKey = "kit_" + kit.getName().toLowerCase();
        boolean onCooldown = plugin.cooldownManager.isOnCooldown(player.getUniqueId(), cooldownKey);

        if (player.hasPermission(kit.getPermission())) {
            if (onCooldown) {
                // If unlocked but on cooldown, show in Red/Gray
                long remaining = plugin.cooldownManager.getRemainingTime(player.getUniqueId(), cooldownKey);
                player.sendMessage(ChatColor.RED + "- " + kit.getName() + ChatColor.GRAY + " (Wait: " + formatTime(remaining) + ")");
            } else {
                // If unlocked and ready
                player.sendMessage(ChatColor.GREEN + "- " + kit.getName() + " " + ChatColor.GRAY + "(Ready)");
            }
        } else {
            // Locked
            player.sendMessage(ChatColor.RED + "- " + kit.getName() + " (Locked)");
        }
    }

    // Helper to make seconds look nice (e.g., "23h 59m 10s")
    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}