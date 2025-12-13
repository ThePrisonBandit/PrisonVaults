package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        // --- /setbio or /setdesc ---
        if (label.equalsIgnoreCase("setbio") || label.equalsIgnoreCase("setdesc")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /setbio <text>");
                return true;
            }
            // Join args into one string
            String bio = String.join(" ", args);

            // Note: Ensure setBio supports OfflinePlayer or use player object
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

            // UPDATED: Supports Offline Players
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player " + args[0] + " has never joined the server.");
                return true;
            }

            openProfileGUI(player, target);
            return true;
        }

        return true;
    }

    private void openProfileGUI(Player viewer, OfflinePlayer target) {
        // Safe name handling for offline players
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Profile: " + targetName);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + targetName);

        // --- GATHER DATA ---

        // 1. Job Data
        // Ensure your JobManager accepts OfflinePlayer. If not, you might need to check if online first.
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

        // 4. Statistics (NEW CODE INTEGRATION)
        String statusStr = target.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline";
        String healthStr = ChatColor.RED + "N/A (Offline)";
        String kdStr = ChatColor.GRAY + "N/A";
        String playtimeStr = ChatColor.GRAY + "N/A";

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();

            // Health
            int health = (int) onlineTarget.getHealth();
            healthStr = ChatColor.RED + "" + health + "/20 ❤";

            // K/D Ratio
            int kills = onlineTarget.getStatistic(Statistic.PLAYER_KILLS);
            int deaths = onlineTarget.getStatistic(Statistic.DEATHS);
            kdStr = ChatColor.WHITE + "" + kills + " / " + deaths;

            // Playtime
            long ticks = onlineTarget.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long hours = (ticks / 20) / 3600;
            playtimeStr = ChatColor.LIGHT_PURPLE + "" + hours + " hours";
        }

        // --- BUILD LORE ---
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Status: " + statusStr);
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

        // Stats Section (Newly Added)
        lore.add(ChatColor.GRAY + "Health: " + healthStr);
        lore.add(ChatColor.GRAY + "K/D Ratio: " + kdStr);
        lore.add(ChatColor.GRAY + "Playtime: " + playtimeStr);
        lore.add(""); // Spacer

        // Bio Section
        lore.add(ChatColor.GOLD + "Biography:");

        // Handle Biography Paragraphing (Split by |)
        if (bioRaw != null && !bioRaw.isEmpty()) {
            String[] lines = bioRaw.split("\\|");
            for (String line : lines) {
                lore.add(ChatColor.WHITE + line.trim());
            }
        } else {
            lore.add(ChatColor.WHITE + "No biography set.");
        }

        meta.setLore(lore);
        head.setItemMeta(meta);

        gui.setItem(13, head); // Center item

        viewer.openInventory(gui);
    }
}