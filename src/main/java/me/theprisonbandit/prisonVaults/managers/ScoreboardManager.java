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

        Objective obj = board.registerNewObjective("PrisonStats", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "PRISON");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // --- 1. PREPARE DATA ---

        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());

        // This MUST match what ColorifyCommand saves ("chat-color")
        String preset = data.getString("chat-color");

        String coloredName = GradientUtils.getGradient(player.getName(), preset);

        // Fallback if something fails
        if (coloredName == null || coloredName.isEmpty()) {
            coloredName = ChatColor.WHITE + player.getName();
        }

        // Gang Info Logic
        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
        String gangDisplay = ChatColor.GRAY + "None";

        if (gang != null) {
            String colorCode = gang.getColor();
            if (colorCode == null || colorCode.isEmpty()) colorCode = "&f";
            String rawString = colorCode + gang.getName() + " &7[" + colorCode + gang.getTag() + "&7]";
            gangDisplay = ChatColor.translateAlternateColorCodes('&', rawString);
        }

        // --- 2. BUILD SCOREBOARD LINES ---

        obj.getScore(ChatColor.GRAY + "").setScore(15);

        // PLAYER
        obj.getScore(ChatColor.GRAY + "Player:").setScore(14);

        Team nameTeam = board.registerNewTeam("nameDisplay");
        String playerKey = ChatColor.BLACK + "" + ChatColor.WHITE;
        nameTeam.addEntry(playerKey);
        nameTeam.setPrefix(coloredName); // Set the gradient name here
        obj.getScore(playerKey).setScore(13);

        obj.getScore(ChatColor.DARK_GRAY + "").setScore(12);

        // RANK
        obj.getScore(ChatColor.YELLOW + "Rank:").setScore(11);
        obj.getScore(ChatColor.WHITE + plugin.getPlayerRank(player)).setScore(10);

        obj.getScore(ChatColor.BLUE + "").setScore(9);

        // GANG
        obj.getScore(ChatColor.LIGHT_PURPLE + "Gang:").setScore(8);
        Team gangTeam = board.registerNewTeam("gangDisplay");
        String gangKey = ChatColor.BLACK + "" + ChatColor.GOLD;
        gangTeam.addEntry(gangKey);
        if (gangDisplay.length() > 64) gangDisplay = gangDisplay.substring(0, 64);
        gangTeam.setPrefix(gangDisplay);
        obj.getScore(gangKey).setScore(7);

        obj.getScore(ChatColor.RESET + "").setScore(6);

        // BALANCE
        obj.getScore(ChatColor.GREEN + "Balance:").setScore(5);
        double bal = plugin.getBalance(player);
        // Ensure NumberUtils.format handles the math correctly
        obj.getScore(ChatColor.WHITE + "$" + NumberUtils.format(bal)).setScore(4);

        obj.getScore(ChatColor.RED + "").setScore(3);

        // VAULTS
        obj.getScore(ChatColor.AQUA + "Vaults Unlocked:").setScore(2);
        obj.getScore(ChatColor.WHITE + "" + plugin.getMaxVaults(player)).setScore(1);

        player.setScoreboard(board);
    }

    // Helper to refresh existing board without flickering (Optional)
    public void updateScoreboard(Player player) {
        setScoreboard(player);
    }
}