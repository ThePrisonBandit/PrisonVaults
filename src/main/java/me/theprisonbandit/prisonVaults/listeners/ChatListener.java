package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.managers.ChatChannelManager; // NEW IMPORT
import me.theprisonbandit.prisonVaults.managers.RankManager;
import me.theprisonbandit.prisonVaults.utils.GradientUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final PrisonVaults plugin;

    public ChatListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // --- NEW CODE: CHANNEL CHECKS ---
        // Check if player is in a specific chat channel (Staff or Gang)
        // If so, send to that channel and cancel the global chat event.
        if (plugin.chatChannelManager != null) {
            ChatChannelManager.Channel channel = plugin.chatChannelManager.getActiveChannel(event.getPlayer());

            if (channel == ChatChannelManager.Channel.STAFF) {
                event.setCancelled(true);
                plugin.chatChannelManager.sendStaffMessage(event.getPlayer(), event.getMessage());
                return;
            }

            if (channel == ChatChannelManager.Channel.GANG) {
                event.setCancelled(true);
                plugin.chatChannelManager.sendGangMessage(event.getPlayer(), event.getMessage());
                return;
            }
        }
        // --------------------------------

        // --- OLD CODE (Preserved) ---

        // 1. Get Prison Rank (Old Code)
        String rank = plugin.getPlayerRank(event.getPlayer());

        // 2. Get Saved Color Preset & Name (Old Code)
        FileConfiguration data = plugin.getPlayerData(event.getPlayer().getUniqueId());
        String colorPreset = data.getString("chat-color");
        String displayName;

        // Apply Gradient if preset exists, otherwise default White
        if (colorPreset != null) {
            displayName = GradientUtils.getGradient(event.getPlayer().getName(), colorPreset);
        } else {
            displayName = ChatColor.WHITE + event.getPlayer().getName();
        }

        // 3. GANG TAG LOGIC (Old Code)
        Gang gang = plugin.gangManager.getPlayerGang(event.getPlayer().getUniqueId());
        String gangPrefix = "";

        if (gang != null) {
            // Format: [TAG] (With the gang's chosen color)
            gangPrefix = ChatColor.translateAlternateColorCodes('&',
                    "&8[" + gang.getColor() + gang.getTag() + "&8] ");
        }

        // 4. Construct the Prison Rank Prefix (Old Code)
        String prisonRankPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + rank + ChatColor.DARK_GRAY + "] ";

        // 5. NEW CODE (From previous update): Staff/Server Rank Logic
        // Fetch the rank from RankManager (Owner, Admin, Member, etc.)
        RankManager.Rank serverRank = plugin.rankManager.getRank(event.getPlayer());

        // Build the prefix using the Rank's color and display name
        // Example: [Owner] or [Member]
        String staffPrefix = serverRank.color + "[" + serverRank.display + "] " + ChatColor.RESET;

        // 6. Final Assembly (Updated)
        // Order: [Staff] [Gang] [PrisonRank] Name: Message
        event.setFormat(staffPrefix + gangPrefix + prisonRankPrefix + displayName + ChatColor.GRAY + ": " + ChatColor.WHITE + "%2$s");
    }
}