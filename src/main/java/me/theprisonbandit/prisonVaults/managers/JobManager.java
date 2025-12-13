package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils; // Import
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound; // Import
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class JobManager {

    private final PrisonVaults plugin;
    // Map<PlayerUUID, QuestTargetMaterial>
    private final Map<UUID, Material> activeQuests = new HashMap<>();
    private final Map<UUID, Integer> questProgress = new HashMap<>();

    public JobManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public enum JobRank {
        COOK(14.0, 0),
        CHEF(28.0, 100),       // Requires 100 XP
        HEAD_CHEF(56.0, 500);  // Requires 500 XP

        private final double pay;
        private final int xpReq;

        JobRank(double pay, int xpReq) {
            this.pay = pay;
            this.xpReq = xpReq;
        }

        public double getPay() { return pay; }
        public int getXpReq() { return xpReq; }
    }

    // --- DATA METHODS ---

    public String getJob(Player player) {
        return plugin.getPlayerData(player.getUniqueId()).getString("job.current", "None");
    }

    public JobRank getJobRank(Player player) {
        String rankName = plugin.getPlayerData(player.getUniqueId()).getString("job.rank", "COOK");
        try {
            return JobRank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            return JobRank.COOK;
        }
    }

    public int getJobXP(Player player) {
        return plugin.getPlayerData(player.getUniqueId()).getInt("job.xp", 0);
    }

    public String getBio(Player player) {
        return plugin.getPlayerData(player.getUniqueId()).getString("profile.bio", "None");
    }

    public void setBio(Player player, String bio) {
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        data.set("profile.bio", bio);
        saveData(player, data);
    }

    // --- JOB ACTIONS ---

    public void joinJob(Player player, String jobName) {
        if (jobName.equalsIgnoreCase("Cooking")) {
            FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
            data.set("job.current", "Cooking");
            data.set("job.rank", JobRank.COOK.name());
            data.set("job.xp", 0);
            saveData(player, data);

            assignQuest(player); // Give first quest
            player.sendMessage(ChatColor.GREEN + "You have joined the Cooking job! Start cooking food!");

            // NEW: Join Sound
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

            updateScoreboard(player);
        } else {
            player.sendMessage(ChatColor.RED + "That job does not exist. Available: Cooking");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    public void quitJob(Player player) {
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        data.set("job.current", "None");
        data.set("job.rank", null);
        data.set("job.xp", 0);
        saveData(player, data);

        // Clear active quest
        activeQuests.remove(player.getUniqueId());
        questProgress.remove(player.getUniqueId());

        player.sendMessage(ChatColor.YELLOW + "You have quit your job.");
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);

        updateScoreboard(player);
    }

    // --- QUEST LOGIC ---

    public void assignQuest(Player player) {
        if (!getJob(player).equals("Cooking")) return;

        // Simple quest: Smelt 16 of a random food
        Material[] foods = {Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.COD, Material.SALMON, Material.POTATO};
        Material target = foods[new Random().nextInt(foods.length)];

        activeQuests.put(player.getUniqueId(), target);
        questProgress.put(player.getUniqueId(), 0);

        player.sendMessage(ChatColor.GOLD + "§lNEW QUEST: §eCook 16 " + formatMat(target) + " to get paid!");

        // NEW: New Quest Sound
        SoundUtils.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    public void addQuestProgress(Player player, Material material, int amount) {
        if (!activeQuests.containsKey(player.getUniqueId())) return;

        Material targetRaw = activeQuests.get(player.getUniqueId());
        Material result = getSmeltResult(targetRaw);

        if (material == result) {
            int current = questProgress.getOrDefault(player.getUniqueId(), 0);
            int needed = 16;

            if (current + amount >= needed) {
                completeQuest(player);
            } else {
                questProgress.put(player.getUniqueId(), current + amount);
            }
        }
    }

    private void completeQuest(Player player) {
        JobRank rank = getJobRank(player);
        double pay = rank.getPay();

        // Reward
        plugin.addMoney(player, pay);
        addXp(player, 10); // 10 XP per quest

        player.sendMessage(ChatColor.GREEN + "§lQUEST COMPLETE! §aYou earned §2$" + pay + " §aand 10 Job XP.");

        // NEW: Completion Sound
        SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Assign new one
        assignQuest(player);
    }

    private void addXp(Player player, int amount) {
        int currentXp = getJobXP(player);
        int newXp = currentXp + amount;

        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        data.set("job.xp", newXp);

        // Check Promotion
        JobRank currentRank = getJobRank(player);
        JobRank nextRank = null;

        if (currentRank == JobRank.COOK && newXp >= JobRank.CHEF.getXpReq()) nextRank = JobRank.CHEF;
        else if (currentRank == JobRank.CHEF && newXp >= JobRank.HEAD_CHEF.getXpReq()) nextRank = JobRank.HEAD_CHEF;

        if (nextRank != null) {
            data.set("job.rank", nextRank.name());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "§lPROMOTION! §dYou are now a " + nextRank.name() + "! Your pay has increased.");

            // NEW: Promotion Sound
            SoundUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        saveData(player, data);
        updateScoreboard(player);
    }

    // --- HELPERS ---

    private void saveData(Player player, FileConfiguration data) {
        try {
            data.save(plugin.getPlayerDataFile(player.getUniqueId()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void updateScoreboard(Player player) {
        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.setScoreboard(player);
        }
    }

    private String formatMat(Material mat) {
        return mat.name().replace("_", " ").toLowerCase();
    }

    private Material getSmeltResult(Material raw) {
        switch (raw) {
            case BEEF: return Material.COOKED_BEEF;
            case PORKCHOP: return Material.COOKED_PORKCHOP;
            case CHICKEN: return Material.COOKED_CHICKEN;
            case COD: return Material.COOKED_COD;
            case SALMON: return Material.COOKED_SALMON;
            case POTATO: return Material.BAKED_POTATO;
            default: return Material.AIR;
        }
    }
}