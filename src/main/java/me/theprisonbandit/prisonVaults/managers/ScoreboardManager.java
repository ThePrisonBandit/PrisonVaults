package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.utils.GradientUtils;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    private final PrisonVaults plugin;

    public ScoreboardManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public void setScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        // Title
        Objective obj = board.registerNewObjective("PrisonStats", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "PRISON");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // --- 1. PREPARE DATA ---

        // Player Name & Gradient
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        String preset = data.getString("chat-color");
        String rawName = player.getName();
        String coloredName;

        if (preset != null) {
            coloredName = GradientUtils.getGradient(rawName, preset);
        } else {
            coloredName = ChatColor.WHITE + rawName;
        }

        // Gang Info
        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
        String gangDisplay = ChatColor.GRAY + "None";

        if (gang != null) {
            String colorCode = gang.getColor();
            // Default to White if no color is set
            if (colorCode == null || colorCode.isEmpty()) {
                colorCode = "&f";
            }

            // Build the string: Color + Name + Gray Bracket + Color + Tag
            String rawString = colorCode + gang.getName() + " &7[" + colorCode + gang.getTag() + "&7]";
            gangDisplay = ChatColor.translateAlternateColorCodes('&', rawString);
        }

        // --- 2. BUILD THE SCOREBOARD ---

        // Spacer
        Score line15 = obj.getScore(ChatColor.GRAY + "");
        line15.setScore(15);

        // PLAYER HEADER
        Score nameHeader = obj.getScore(ChatColor.GRAY + "Player:");
        nameHeader.setScore(14);

        // --- PLAYER TEAM (For Gradient Name) ---
        Team nameTeam = board.registerNewTeam("nameDisplay");
        // Unique hidden key (Black + White)
        String playerKey = ChatColor.BLACK + "" + ChatColor.WHITE;
        nameTeam.addEntry(playerKey);
        try {
            nameTeam.setPrefix(coloredName);
        } catch (IllegalArgumentException e) {
            nameTeam.setPrefix(ChatColor.WHITE + rawName);
        }
        Score nameValue = obj.getScore(playerKey);
        nameValue.setScore(13);

        // Spacer
        Score line12 = obj.getScore(ChatColor.DARK_GRAY + "");
        line12.setScore(12);

        // RANK LINE
        Score rankHeader = obj.getScore(ChatColor.YELLOW + "Rank:");
        rankHeader.setScore(11);

        String rank = plugin.getPlayerRank(player);
        Score rankValue = obj.getScore(ChatColor.WHITE + rank);
        rankValue.setScore(10);

        // Spacer
        Score line9 = obj.getScore(ChatColor.BLUE + "");
        line9.setScore(9);

        // GANG HEADER
        Score gangHeader = obj.getScore(ChatColor.LIGHT_PURPLE + "Gang:");
        gangHeader.setScore(8);

        // --- NEW: GANG TEAM (For Color Formatting) ---
        Team gangTeam = board.registerNewTeam("gangDisplay");
        // Unique hidden key (Black + Gold) - Must be different from playerKey!
        String gangKey = ChatColor.BLACK + "" + ChatColor.GOLD;
        gangTeam.addEntry(gangKey);

        // Safeguard for very long strings (though 1.21 limits are high)
        if (gangDisplay.length() > 64) {
            gangDisplay = gangDisplay.substring(0, 64);
        }
        gangTeam.setPrefix(gangDisplay);

        Score gangValue = obj.getScore(gangKey);
        gangValue.setScore(7);

        // Spacer
        Score line6 = obj.getScore(ChatColor.RESET + "");
        line6.setScore(6);

        // MONEY LINE
        Score moneyHeader = obj.getScore(ChatColor.GREEN + "Balance:");
        moneyHeader.setScore(5);

        double bal = plugin.getBalance(player);
        Score moneyValue = obj.getScore(ChatColor.WHITE + "$" + NumberUtils.format(bal));
        moneyValue.setScore(4);

        // Spacer
        Score line3 = obj.getScore(ChatColor.RED + "");
        line3.setScore(3);

        // VAULTS LINE
        Score vaultHeader = obj.getScore(ChatColor.AQUA + "Vaults Unlocked:");
        vaultHeader.setScore(2);

        int maxVaults = plugin.getMaxVaults(player);
        Score vaultValue = obj.getScore(ChatColor.WHITE + "" + maxVaults);
        vaultValue.setScore(1);

        player.setScoreboard(board);
    }
}