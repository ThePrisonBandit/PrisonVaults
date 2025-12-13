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
import java.util.ArrayList;
import java.util.List;

public class RankupCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public RankupCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        String currentRank = plugin.getPlayerRank(player);
        String nextRank = getNextRank(currentRank);

        if (nextRank == null) {
            player.sendMessage(ChatColor.GREEN + "You are at the max rank!");
            return true;
        }

        double cost = plugin.rankLadder.get(nextRank);
        double balance = plugin.getBalance(player);

        if (balance < cost) {
            double needed = cost - balance;
            player.sendMessage(ChatColor.RED + "You need $" + String.format("%.2f", needed) + " more.");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        plugin.removeMoney(player, cost);
        plugin.setPlayerRank(player, nextRank);
        plugin.scoreboardManager.updateScoreboard(player);

        Bukkit.broadcastMessage(ChatColor.GOLD + "PrisonVaults >> " + player.getName() + " ranked up to " + nextRank + "!");

        // Success Sound
        SoundUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        return true;
    }

    private String getNextRank(String current) {
        List<String> ranks = new ArrayList<>(plugin.rankLadder.keySet());
        int index = ranks.indexOf(current);
        if (index != -1 && index < ranks.size() - 1) {
            return ranks.get(index + 1);
        }
        return null;
    }
}