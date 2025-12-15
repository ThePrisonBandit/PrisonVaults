package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class DonationListener implements Listener {

    private final PrisonVaults plugin;

    public DonationListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Create the clickable part
        TextComponent link = new TextComponent("[BECOME A SUPPORTER]");
        link.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        link.setBold(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://buymeacoffee.com/theprisonbandit/membership"));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to view perks and rewards!")
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .create()));

        // 2. Build the full message structure
        // Line 1: Separator
        player.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");

        // Line 2: Intro text
        player.sendMessage(ChatColor.GRAY + " Enjoying PrisonVaults? Help fund development!");

        // Line 3: Centering spacer + The Link
        TextComponent message = new TextComponent("   "); // Indent slightly
        message.addExtra(link);

        // Send the clickable component
        player.spigot().sendMessage(message);

        // Line 4: Separator
        player.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");
    }
}