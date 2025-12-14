package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatChannelManager {

    private final PrisonVaults plugin;
    public enum Channel { NONE, STAFF, GANG }
    private final Map<UUID, Channel> activeChannels = new HashMap<>();

    public ChatChannelManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public void toggleChannel(Player p, Channel channel) {
        if (activeChannels.get(p.getUniqueId()) == channel) {
            activeChannels.remove(p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW + "You are now talking in global chat.");
        } else {
            activeChannels.put(p.getUniqueId(), channel);
            String name = (channel == Channel.STAFF) ? "Staff" : "Gang";
            p.sendMessage(ChatColor.GREEN + "You are now talking in " + name + " chat.");
        }
    }

    public Channel getActiveChannel(Player p) {
        return activeChannels.getOrDefault(p.getUniqueId(), Channel.NONE);
    }

    public void sendStaffMessage(Player sender, String message) {
        String format = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "SC" + ChatColor.DARK_GRAY + "] "
                + ChatColor.YELLOW + sender.getName() + ChatColor.DARK_GRAY + ": "
                + ChatColor.WHITE + message;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("prisonvaults.staffchat")) {
                p.sendMessage(format);
            }
        }
        Bukkit.getConsoleSender().sendMessage(format);
    }

    public void sendGangMessage(Player sender, String message) {
        // FIX: Use getPlayerGang(UUID)
        Gang senderGang = plugin.gangManager.getPlayerGang(sender.getUniqueId());

        if (senderGang == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a gang!");
            activeChannels.remove(sender.getUniqueId());
            return;
        }

        String format = ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "GC" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GOLD + sender.getName() + ChatColor.DARK_GRAY + ": "
                + ChatColor.WHITE + message;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Gang targetGang = plugin.gangManager.getPlayerGang(p.getUniqueId());
            // Compare IDs to ensure same gang
            if (targetGang != null && targetGang.getId().equals(senderGang.getId())) {
                p.sendMessage(format);
            }
        }
    }
}