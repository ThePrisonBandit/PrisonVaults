package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AnnounceCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public AnnounceCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin.announce")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvannounce <message>");
            return true;
        }

        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        String rawMessage = builder.toString().trim();
        String message = ChatColor.translateAlternateColorCodes('&', rawMessage);
        String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&4&lANNOUNCEMENT&8] &r");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(prefix + message);
            SoundUtils.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        if (sender instanceof Player) {
            plugin.getLogger().info("[Announcement] " + sender.getName() + ": " + rawMessage);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Return empty list so it doesn't suggest player names for the announcement text
        return Collections.emptyList();
    }
}