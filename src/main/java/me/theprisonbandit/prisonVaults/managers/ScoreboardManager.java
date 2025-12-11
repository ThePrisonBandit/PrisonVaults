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

        // --- TITLE ---
        Objective obj = board.registerNewObjective("PrisonStats", Criteria.DUMMY,
                ChatColor.translateAlternateColorCodes('&', "&6&lPRISON &e&lVAULTS"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // --- 1. PREPARE DATA ---

        // Player Name & Gradient
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        String preset = data.getString("chat-color");
        String coloredName = GradientUtils.getGradient(player.getName(), preset);
        if (coloredName == null || coloredName.isEmpty()) {
            coloredName = ChatColor.WHITE + player.getName();
        }

        // Gang Info
        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
        String gangDisplay = ChatColor.GRAY + "No Gang";
        if (gang != null) {
            String colorCode = gang.getColor();
            if (colorCode == null || colorCode.isEmpty()) colorCode = "&f";
            String rawString = colorCode + gang.getName() + " &8[" + colorCode + gang.getTag() + "&8]";
            gangDisplay = ChatColor.translateAlternateColorCodes('&', rawString);
        }

        // Job Info
        String currentJob = plugin.jobManager.getJob(player);
        String jobTitle = "Unemployed";
        if (!currentJob.equals("None")) {
            jobTitle = plugin.jobManager.getJobRank(player).name();
        }

        // --- 2. BUILD LINES ---
        int score = 15;

        // Top Separator
        createLine(board, obj, "&8&m---------------------", score--);

        // PLAYER SECTION (Dynamic Team Line)
        createDynamicLine(board, obj, "playerName", "&f \uD83D\uDC64 ", coloredName, score--);
        createLine(board, obj, "   &7Rank: &f" + plugin.getPlayerRank(player), score--);

        // Spacer
        createLine(board, obj, ChatColor.RED + "", score--);

        // JOB SECTION
        if (!currentJob.equals("None")) {
            createLine(board, obj, "&b&l JOB INFO", score--);
            createLine(board, obj, "   &7Job: &f" + currentJob, score--);
            createLine(board, obj, "   &7Title: &f" + jobTitle, score--);
        } else {
            createLine(board, obj, "&b&l JOB INFO", score--);
            createLine(board, obj, "   &7Status: &8Unemployed", score--);
        }

        // Spacer
        createLine(board, obj, ChatColor.GREEN + "", score--);

        // GANG SECTION
        createLine(board, obj, "&d&l GANG", score--);
        createDynamicLine(board, obj, "gangEntry", "   ", gangDisplay, score--);

        // Spacer
        createLine(board, obj, ChatColor.BLUE + "", score--);

        // STATS SECTION
        createLine(board, obj, "&a&l STATISTICS", score--);
        double bal = plugin.getBalance(player);
        createLine(board, obj, "   &7Balance: &2$&a" + NumberUtils.format(bal), score--);
        createLine(board, obj, "   &7Vaults: &f" + plugin.getMaxVaults(player), score--);

        // Bottom Separator
        createLine(board, obj, "&8&m---------------------", score);

        player.setScoreboard(board);
    }

    /**
     * Helper to create a standard text line.
     */
    private void createLine(Scoreboard board, Objective obj, String text, int scoreNum) {
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        while (board.getEntries().contains(colored)) {
            colored += ChatColor.RESET;
        }
        Score s = obj.getScore(colored);
        s.setScore(scoreNum);
    }

    /**
     * Helper to create a dynamic line using Teams.
     * Updated: Removed the 64-character limit for 1.21 support.
     */
    private void createDynamicLine(Scoreboard board, Objective obj, String teamName, String prefix, String suffix, int scoreNum) {
        Team team = board.registerNewTeam(teamName);

        // Create a unique entry key (Invisible)
        String entry = ChatColor.values()[scoreNum % 15].toString() + ChatColor.RESET;

        team.addEntry(entry);
        team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));

        // NO LIMIT: Gradients are long, so we allow the full suffix.
        team.setSuffix(suffix);

        Score s = obj.getScore(entry);
        s.setScore(scoreNum);
    }

    public void updateScoreboard(Player player) {
        setScoreboard(player);
    }

    public void updateNameAnimation(Player player, int animationStep) {
        org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
        org.bukkit.scoreboard.Team nameTeam = board.getTeam("playerName");

        if (nameTeam != null) {
            FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
            String preset = data.getString("chat-color");

            // Get the new animated string
            String animated = GradientUtils.getAnimatedGradient(player.getName(), preset, animationStep);

            // Update suffix
            nameTeam.setSuffix(animated);
        }
    }
}