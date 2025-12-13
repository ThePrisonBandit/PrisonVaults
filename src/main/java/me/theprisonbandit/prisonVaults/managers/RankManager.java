package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RankManager {

    private final PrisonVaults plugin;

    public enum Rank {
        OWNER(ChatColor.DARK_RED, "Owner", 10),
        CO_OWNER(ChatColor.RED, "Co-Owner", 9),
        ADMIN(ChatColor.RED, "Admin", 8),
        MODERATOR(ChatColor.DARK_GREEN, "Mod", 5),
        HELPER(ChatColor.BLUE, "Helper", 3),
        MEMBER(ChatColor.GOLD, "Member", 1);

        public final ChatColor color;
        public final String display;
        public final int weight;

        Rank(ChatColor color, String display, int weight) {
            this.color = color;
            this.display = display;
            this.weight = weight;
        }
    }

    public RankManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public Rank getRank(OfflinePlayer player) {
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        // FIX: Changed key to "staff_rank" to avoid conflict with Prison Ranks (A-Z)
        String rankName = data.getString("staff_rank", "MEMBER");
        try {
            return Rank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            return Rank.MEMBER;
        }
    }

    public void setRank(OfflinePlayer player, Rank rank) {
        File f = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        // FIX: Saving to "staff_rank"
        data.set("staff_rank", rank.name());
        try { data.save(f); } catch (IOException e) { e.printStackTrace(); }
    }

    public String getPrefix(OfflinePlayer player) {
        Rank rank = getRank(player);
        return rank.color + "[" + rank.display + "] " + ChatColor.RESET;
    }

    // Permission Logic
    public boolean isAdmin(OfflinePlayer player) {
        return getRank(player).weight >= 8;
    }

    public boolean isStaff(OfflinePlayer player) {
        return getRank(player).weight >= 3;
    }
}