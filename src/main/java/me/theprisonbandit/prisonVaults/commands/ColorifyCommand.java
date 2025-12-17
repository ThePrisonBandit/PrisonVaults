package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.GradientUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added Import
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil; // Added Import

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Added "implements TabCompleter"
public class ColorifyCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public ColorifyCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public enum GradientStyle {
        RAINBOW, SUNSET, DAWNINGDAY, MIDNIGHT, GRASSLANDS, SWAMP, STEEL, GOLDEN,
        FOOLSGOLD, DIAMOND, EMERALD, AMETHYST, WOODEN, SUNNYDAY, CYBERLORD,
        VOLCANO, BLAZINGFIRE, DESERT, BLURPLE, REDINK, LEMONLIME, BLUECREW
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use colorify.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prisonvaults.colorify")) {
            player.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.DARK_GRAY + "--- " + ChatColor.GOLD + "Available Gradients" + ChatColor.DARK_GRAY + " ---");
            String styles = Arrays.stream(GradientStyle.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.YELLOW));
            player.sendMessage(ChatColor.YELLOW + styles);
            return true;
        }

        String inputName = args[0].toUpperCase();
        GradientStyle selectedStyle;

        try {
            selectedStyle = GradientStyle.valueOf(inputName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Unknown style! Type /colorify for a list.");
            return true;
        }

        // 1. SAVE THE SELECTION TO CONFIG
        savePlayerGradient(player, selectedStyle.name());

        // 2. APPLY IMMEDIATELY (Visuals)
        String newDisplayName = GradientUtils.getGradient(player.getName(), selectedStyle.name());
        player.setDisplayName(newDisplayName);
        player.setPlayerListName(newDisplayName);

        // 3. UPDATE SCOREBOARD IMMEDIATELY
        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.updateScoreboard(player);
        }

        player.sendMessage(ChatColor.GREEN + "Your name color has been updated to: " + selectedStyle.name().toLowerCase());
        return true;
    }

    private void savePlayerGradient(Player player, String gradientName) {
        File file = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        // Saving to 'chat-color' so ScoreboardManager can find it
        data.set("chat-color", gradientName);

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- NEW: TAB COMPLETION ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> styles = new ArrayList<>();
            for (GradientStyle style : GradientStyle.values()) {
                styles.add(style.name().toLowerCase());
            }
            return StringUtil.copyPartialMatches(args[0], styles, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}