package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + "$" + String.format("%.2f", needed) +
                    ChatColor.RED + " more to reach rank " + ChatColor.BLUE + nextRank);
            return true;
        }

        plugin.removeMoney(player, cost);
        plugin.setPlayerRank(player, nextRank);
        // Add this line to refresh the sidebar:
        plugin.scoreboardManager.updateScoreboard(player);
        Bukkit.broadcastMessage(ChatColor.GOLD + "PrisonVaults >> " + ChatColor.WHITE + player.getName() +
                ChatColor.GREEN + " ranked up to " + ChatColor.BLUE + nextRank + "!");
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
