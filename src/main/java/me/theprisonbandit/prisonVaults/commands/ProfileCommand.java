package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ProfileCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public ProfileCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // --- /setbio or /setdesc ---
        if (label.equalsIgnoreCase("setbio") || label.equalsIgnoreCase("setdesc")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /setbio <text>");
                return true;
            }
            // Join args into one string
            String bio = String.join(" ", args);
            plugin.jobManager.setBio(player, bio);
            player.sendMessage(ChatColor.GREEN + "Biography updated!");
            return true;
        }

        // --- /myprofile ---
        if (label.equalsIgnoreCase("myprofile")) {
            openProfileGUI(player, player);
            return true;
        }

        // --- /whois ---
        if (label.equalsIgnoreCase("whois")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /whois <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player offline.");
                return true;
            }
            openProfileGUI(player, target);
            return true;
        }

        return true;
    }

    private void openProfileGUI(Player viewer, Player target) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Profile: " + target.getName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + target.getName());

        // --- GATHER DATA ---

        // 1. Job Data
        String job = plugin.jobManager.getJob(target);
        String jobRank = "N/A";
        if (!job.equals("None")) {
            jobRank = plugin.jobManager.getJobRank(target).name();
        }

        // 2. Gang Data
        Gang gang = plugin.gangManager.getPlayerGang(target.getUniqueId());
        String gangDisplay = ChatColor.GRAY + "None";
        String gangTag = "";

        if (gang != null) {
            String color = gang.getColor();
            if (color == null || color.isEmpty()) color = "&f";

            // Format: Name
            gangDisplay = ChatColor.translateAlternateColorCodes('&', color + gang.getName());

            // Format: [TAG]
            gangTag = ChatColor.translateAlternateColorCodes('&', "&8[" + color + gang.getTag() + "&8]");
        }

        // 3. Other Data
        String rank = plugin.getPlayerRank(target);
        double bal = plugin.getBalance(target);
        String bioRaw = plugin.jobManager.getBio(target);

        // --- BUILD LORE ---
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rank: " + ChatColor.YELLOW + rank);
        lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" + NumberUtils.format(bal));
        lore.add(""); // Spacer

        // Gang Section
        lore.add(ChatColor.GRAY + "Gang: " + gangDisplay);
        if (gang != null) {
            lore.add(ChatColor.GRAY + "Tag: " + gangTag);
        }

        lore.add(""); // Spacer

        // Job Section
        lore.add(ChatColor.GRAY + "Job: " + ChatColor.AQUA + job);
        lore.add(ChatColor.GRAY + "Position: " + ChatColor.AQUA + jobRank);
        lore.add(""); // Spacer

        // Bio Section
        lore.add(ChatColor.GOLD + "Biography:");

        // Handle Biography Paragraphing (Split by |)
        String[] lines = bioRaw.split("\\|");
        for (String line : lines) {
            lore.add(ChatColor.WHITE + line.trim());
        }

        meta.setLore(lore);
        head.setItemMeta(meta);

        gui.setItem(13, head); // Center item

        viewer.openInventory(gui);
    }
}