package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnnounceCommand implements CommandExecutor {

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

        // 1. Join args into a single string
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        String rawMessage = builder.toString().trim();

        // 2. Translate color codes (e.g. &c for red)
        String message = ChatColor.translateAlternateColorCodes('&', rawMessage);

        // 3. Create a prefix
        String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&4&lANNOUNCEMENT&8] &r");

        // 4. Send to all online players with sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(prefix + message);
            // High pitch "Pling" sound to grab attention
            SoundUtils.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        // 5. Feedback for Console
        if (sender instanceof Player) {
            plugin.getLogger().info("[Announcement] " + sender.getName() + ": " + rawMessage);
        }

        return true;
    }
}