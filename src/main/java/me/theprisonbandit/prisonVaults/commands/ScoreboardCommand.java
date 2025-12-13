package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public ScoreboardCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /pvscoreboard <show|hide>");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("show")) {
            if (plugin.scoreboardManager.isScoreboardVisible(player)) {
                player.sendMessage(ChatColor.YELLOW + "Your scoreboard is already visible.");
                return true;
            }
            plugin.scoreboardManager.setScoreboardVisible(player, true);
            player.sendMessage(ChatColor.GREEN + "Scoreboard enabled.");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            return true;
        }

        if (sub.equals("hide")) {
            if (!plugin.scoreboardManager.isScoreboardVisible(player)) {
                player.sendMessage(ChatColor.YELLOW + "Your scoreboard is already hidden.");
                return true;
            }
            plugin.scoreboardManager.setScoreboardVisible(player, false);
            player.sendMessage(ChatColor.RED + "Scoreboard disabled.");
            SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /pvscoreboard <show|hide>");
        return true;
    }
}