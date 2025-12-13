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

    public void setScoreboardVisible(Player player, boolean visible) {
        // ... (Same toggle logic as before) ...
        // For brevity, assuming you kept the toggle logic I sent in the previous turn
        // If you need it again, let me know!
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        data.set("scoreboard-enabled", visible);
        try { data.save(plugin.getPlayerDataFile(player.getUniqueId())); } catch (Exception e) {}
        if (visible) updateScoreboard(player);
        else removeScoreboard(player);
    }

    public boolean isScoreboardVisible(Player player) {
        return plugin.getPlayerData(player.getUniqueId()).getBoolean("scoreboard-enabled", true);
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    // --- CORE LOGIC ---

    public void setScoreboard(Player player) {
        if (!isScoreboardVisible(player)) return;

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

        int jobLevel = 0;
        double jobXp = 0;

        if (!currentJob.equals("None")) {
            // --- FIX 1: Fetch the correct Promotion Title ---
            jobLevel = plugin.jobManager.getLevel(player);
            jobXp = plugin.jobManager.getXp(player);
            jobTitle = plugin.jobManager.getPromotionTitle(currentJob, jobLevel);
        }

        // --- 2. BUILD LINES (Max 15) ---
        int score = 15;

        // Top Separator
        createLine(board, obj, "&8&m---------------------", score--);

        // PLAYER SECTION
        createDynamicLine(board, obj, "playerName", "&f \uD83D\uDC64 ", coloredName, score--);
        createLine(board, obj, "   &7Rank: &f" + plugin.getPlayerRank(player), score--);

        // JOB SECTION
        if (!currentJob.equals("None")) {
            // FIX 2: Condensed Lines (Removed empty spacers to fit stats)
            createLine(board, obj, "&b&l JOB INFO", score--);
            createLine(board, obj, "   &7Job: &f" + currentJob, score--);
            createLine(board, obj, "   &7Title: &f" + jobTitle, score--);
            // Combined Level and XP into one line
            createLine(board, obj, "   &7Lvl: &f" + jobLevel + " &7(" + (int)jobXp + "/100)", score--);

            String day = plugin.jobScheduleManager.getCurrentDayName();
            String status = plugin.jobScheduleManager.isWorkDay() ? "&a(Open)" : "&c(Closed)";
            createLine(board, obj, "   &7Day: &e" + day + " " + status, score--);

        } else {
            createLine(board, obj, "&b&l JOB INFO", score--);
            createLine(board, obj, "   &7Status: &8Unemployed", score--);
        }

        // GANG SECTION
        createLine(board, obj, "&d&l GANG", score--);
        createDynamicLine(board, obj, "gangEntry", "   ", gangDisplay, score--);

        // STATS SECTION
        createLine(board, obj, "&a&l STATISTICS", score--);
        double bal = plugin.getBalance(player);
        createLine(board, obj, "   &7Balance: &2$&a" + NumberUtils.format(bal), score--);
        createLine(board, obj, "   &7Vaults: &f" + plugin.getMaxVaults(player), score--);

        // Bottom Separator
        createLine(board, obj, "&8&m---------------------", score);

        player.setScoreboard(board);
    }

    private void createLine(Scoreboard board, Objective obj, String text, int scoreNum) {
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        while (board.getEntries().contains(colored)) {
            colored += ChatColor.RESET;
        }
        Score s = obj.getScore(colored);
        s.setScore(scoreNum);
    }

    private void createDynamicLine(Scoreboard board, Objective obj, String teamName, String prefix, String suffix, int scoreNum) {
        Team team = board.registerNewTeam(teamName);
        String entry = ChatColor.values()[Math.abs(scoreNum) % 15].toString() + ChatColor.RESET;
        team.addEntry(entry);
        team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));
        team.setSuffix(suffix);
        Score s = obj.getScore(entry);
        s.setScore(scoreNum);
    }

    public void updateScoreboard(Player player) {
        setScoreboard(player);
    }

    public void updateNameAnimation(Player player, int animationStep) {
        if (!isScoreboardVisible(player)) return;
        org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
        org.bukkit.scoreboard.Team nameTeam = board.getTeam("playerName");

        if (nameTeam != null) {
            FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
            String preset = data.getString("chat-color");
            String animated = GradientUtils.getAnimatedGradient(player.getName(), preset, animationStep);
            nameTeam.setSuffix(animated);
        }
    }
}