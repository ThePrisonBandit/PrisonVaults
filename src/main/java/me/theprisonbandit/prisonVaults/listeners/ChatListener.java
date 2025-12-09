package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
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
        // 1. Get Rank
        String rank = plugin.getPlayerRank(event.getPlayer());

        // 2. Get Saved Color Preset & Name
        FileConfiguration data = plugin.getPlayerData(event.getPlayer().getUniqueId());
        String colorPreset = data.getString("chat-color");
        String displayName;

        // Apply Gradient if preset exists, otherwise default White
        if (colorPreset != null) {
            displayName = GradientUtils.getGradient(event.getPlayer().getName(), colorPreset);
        } else {
            displayName = ChatColor.WHITE + event.getPlayer().getName();
        }

        // 3. GANG TAG LOGIC
        // We fetch the gang from the manager
        Gang gang = plugin.gangManager.getPlayerGang(event.getPlayer().getUniqueId());
        String gangPrefix = "";

        if (gang != null) {
            // Format: [TAG] (With the gang's chosen color)
            // Example: &8[&cNC&8]
            gangPrefix = ChatColor.translateAlternateColorCodes('&',
                    "&8[" + gang.getColor() + gang.getTag() + "&8] ");
        }

        // 4. Construct the Rank Prefix
        // Format: [Rank]
        String rankPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + rank + ChatColor.DARK_GRAY + "] ";

        // 5. Final Assembly
        // Order: [Gang] [Rank] Name: Message
        // We manually build the string to support Gradients properly
        event.setFormat(gangPrefix + rankPrefix + displayName + ChatColor.GRAY + ": " + ChatColor.WHITE + "%2$s");
    }
}