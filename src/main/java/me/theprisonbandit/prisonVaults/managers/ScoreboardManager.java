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

        // Staff Rank Data
        RankManager.Rank staffRank = plugin.rankManager.getRank(player);

        // Gang Info
        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
        String gangDisplay = ChatColor.GRAY + "No Gang";
        String gangDesc = "";

        if (gang != null) {
            String colorCode = gang.getColor();
            if (colorCode == null || colorCode.isEmpty()) colorCode = "&f";
            String rawString = colorCode + gang.getName() + " &8[" + colorCode + gang.getTag() + "&8]";
            gangDisplay = ChatColor.translateAlternateColorCodes('&', rawString);

            // Truncate description if too long for scoreboard
            gangDesc = gang.getDescription();
            if (gangDesc.length() > 16) {
                gangDesc = gangDesc.substring(0, 16) + "..";
            }
        }

        // Job Info
        String currentJob = plugin.jobManager.getJob(player);
        String jobTitle = "Unemployed";
        int jobLevel = 0;
        double jobXp = 0;

        if (!currentJob.equals("None")) {
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

        // Add Staff Rank Line if they are Staff
        if (staffRank != RankManager.Rank.MEMBER) {
            createLine(board, obj, "   &7Staff: " + staffRank.color + staffRank.display, score--);
        }
        createLine(board, obj, "   &7Rank: &f" + plugin.getPlayerRank(player), score--);

        // JOB SECTION
        if (!currentJob.equals("None")) {
            createLine(board, obj, "&b&l JOB INFO", score--);
            createLine(board, obj, "   &7Title: &f" + jobTitle, score--);
            // Combined Level and XP
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

        // Show Description if in a gang
        if (gang != null && !gangDesc.isEmpty()) {
            createLine(board, obj, "   &7Desc: &f" + gangDesc, score--);
        }

        // STATS SECTION (Combined to save space)
        createLine(board, obj, "&a&l STATISTICS", score--);
        double bal = plugin.getBalance(player);
        // Format: Balance | Vaults
        createLine(board, obj, "   &2$&a" + NumberUtils.format(bal) + " &7| &fVaults: " + plugin.getMaxVaults(player), score--);

        // Bottom Separator (Only add if we have space, score > 0)
        if (score > 0) {
            createLine(board, obj, "&8&m---------------------", score);
        }

        player.setScoreboard(board);
    }

    private void createLine(Scoreboard board, Objective obj, String text, int scoreNum) {
        if (scoreNum < 1) return; // Prevent crash if over 15 lines
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        // Ensure uniqueness
        while (board.getEntries().contains(colored)) {
            colored += ChatColor.RESET;
        }
        Score s = obj.getScore(colored);
        s.setScore(scoreNum);
    }

    private void createDynamicLine(Scoreboard board, Objective obj, String teamName, String prefix, String suffix, int scoreNum) {
        if (scoreNum < 1) return;
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