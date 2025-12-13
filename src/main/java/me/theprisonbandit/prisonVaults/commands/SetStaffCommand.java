package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SetStaffCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public SetStaffCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Permission Check (OP Only)
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "This command is for Server Operators only.");
            return true;
        }

        // 2. Validate Args
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setstaff <player> <rank>");
            sender.sendMessage(ChatColor.GRAY + "Valid Ranks: " + getRankList());
            return true;
        }

        String targetName = args[0];
        String rankName = args[1].toUpperCase();

        // 3. Get Target (Offline support)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        // Basic check if player has played before (optional, but good practice)
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Warning: " + targetName + " has never joined this server.");
        }

        // 4. Validate Rank
        try {
            RankManager.Rank rank = RankManager.Rank.valueOf(rankName);

            // 5. Apply Rank
            plugin.rankManager.setRank(target, rank);

            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s rank to " + rank.display);
            if (target.isOnline()) {
                ((Player)target).sendMessage(ChatColor.GREEN + "Your rank has been updated to " + rank.display);
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid Rank: " + rankName);
            sender.sendMessage(ChatColor.GRAY + "Valid Ranks: " + getRankList());
        }

        return true;
    }

    private String getRankList() {
        return Arrays.stream(RankManager.Rank.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}