package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.managers.RankManager;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil; // Added

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetStaffCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public SetStaffCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "This command is for Server Operators only.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setstaff <player> <rank>");
            sender.sendMessage(ChatColor.GRAY + "Valid Ranks: " + getRankList());
            return true;
        }

        String targetName = args[0];
        String rankName = args[1].toUpperCase();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Warning: " + targetName + " has never joined this server.");
        }

        try {
            RankManager.Rank rank = RankManager.Rank.valueOf(rankName);
            plugin.rankManager.setRank(target, rank);

            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s rank to " + rank.display);

            if (target.isOnline()) {
                Player p = target.getPlayer();
                p.sendMessage(ChatColor.GREEN + "Your staff rank has been updated to " + rank.display);
                plugin.scoreboardManager.setScoreboard(p);
                SoundUtils.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return Collections.emptyList();

        if (args.length == 1) {
            return null; // Players
        } else if (args.length == 2) {
            List<String> ranks = new ArrayList<>();
            for (RankManager.Rank r : RankManager.Rank.values()) {
                ranks.add(r.name());
            }
            return StringUtil.copyPartialMatches(args[1], ranks, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}