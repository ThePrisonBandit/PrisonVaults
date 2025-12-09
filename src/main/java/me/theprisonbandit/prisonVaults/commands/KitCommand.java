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

            // 1. Get all kits
            List<Kit> allKits = new ArrayList<>(plugin.kitManager.getAllKits());

            // 2. Create categories
            Kit starterKit = null;
            List<Kit> rankKits = new ArrayList<>();
            List<Kit> customKits = new ArrayList<>();

            // 3. Sort them into buckets
            for (Kit k : allKits) {
                if (k.getName().equalsIgnoreCase("Starter")) {
                    starterKit = k;
                } else if (k.getName().matches("(?i)Rank[A-Z]")) {
                    // Matches RankA, RankB, ... RankZ (Case insensitive)
                    rankKits.add(k);
                } else {
                    // Everything else (GodKit, PvP, etc.)
                    customKits.add(k);
                }
            }

            // 4. Sort the lists Alphabetically
            rankKits.sort(Comparator.comparing(Kit::getName));
            customKits.sort(Comparator.comparing(Kit::getName));

            // 5. Display in specific order: Starter -> Ranks -> Custom

            // A. Show Starter
            if (starterKit != null) {
                printKit(player, starterKit);
            }

            // B. Show Ranks
            if (!rankKits.isEmpty()) {
                player.sendMessage(ChatColor.DARK_GRAY + "--- Ranks ---");
                for (Kit k : rankKits) printKit(player, k);
            }

            // C. Show Custom/Created Kits
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

        plugin.kitManager.giveKit(player, kitName);
        return true;
    }

    // Helper method to print kit status
    private void printKit(Player player, Kit kit) {
        if (player.hasPermission(kit.getPermission())) {
            player.sendMessage(ChatColor.GREEN + "- " + kit.getName() + " " + ChatColor.GRAY + "(Unlocked)");
        } else {
            player.sendMessage(ChatColor.RED + "- " + kit.getName() + " (Locked)");
        }
    }
}