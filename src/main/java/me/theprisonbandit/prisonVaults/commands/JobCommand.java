package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added Import
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil; // Added Import

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Added "implements TabCompleter"
public class JobCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public JobCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /job <join|quit|info|promote>");
            return true;
        }

        String sub = args[0].toLowerCase();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (sub.equals("join") && player != null) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /job join <Cooking|Blacksmith>");
                return true;
            }
            plugin.jobManager.joinJob(player, args[1]);
            return true;
        }

        if (sub.equals("quit") && player != null) {
            plugin.jobManager.quitJob(player);
            return true;
        }

        if (sub.equals("info") && player != null) {
            String job = plugin.jobManager.getJob(player);
            if (job.equalsIgnoreCase("None")) {
                player.sendMessage(ChatColor.RED + "You are currently unemployed.");
                return true;
            }

            int level = plugin.jobManager.getLevel(player);
            double xp = plugin.jobManager.getXp(player);
            String title = plugin.jobManager.getPromotionTitle(job, level);
            int nextPromoLevel = ((level / 5) + 1) * 5;

            String day = plugin.jobScheduleManager.getCurrentDayName();
            boolean isOpen = plugin.jobScheduleManager.isWorkDay();

            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
            player.sendMessage(ChatColor.GOLD + "       " + job + " Job Information");
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
            player.sendMessage(ChatColor.YELLOW + "Current Title: " + ChatColor.AQUA + title);
            player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level + ChatColor.GRAY + " / 50");
            player.sendMessage(ChatColor.YELLOW + "XP Progress: " + ChatColor.WHITE + (int)xp + ChatColor.GRAY + " / 100 XP");

            player.sendMessage(ChatColor.YELLOW + "Current Day: " + ChatColor.WHITE + day +
                    (isOpen ? ChatColor.GREEN + " (Work Open)" : ChatColor.RED + " (Weekend - Off)"));

            if (level < 50) {
                player.sendMessage(ChatColor.YELLOW + "Next Promotion: " + ChatColor.GREEN + "Level " + nextPromoLevel);
            }
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
            SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            return true;
        }

        if (sub.equals("promote")) {
            if (!sender.hasPermission("prisonvaults.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /job promote <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player offline.");
                return true;
            }

            String job = plugin.jobManager.getJob(target);
            if (job.equals("None")) {
                sender.sendMessage(ChatColor.RED + target.getName() + " does not have a job.");
                return true;
            }

            int currentLevel = plugin.jobManager.getLevel(target);
            int nextLevel = ((currentLevel / 5) + 1) * 5;

            if (nextLevel > 50) {
                sender.sendMessage(ChatColor.RED + "Player is already at max promotion rank.");
                return true;
            }

            plugin.jobManager.setLevel(target, job, nextLevel);
            plugin.jobManager.setXp(target, job, 0);
            plugin.scoreboardManager.updateScoreboard(target);

            String newTitle = plugin.jobManager.getPromotionTitle(job, nextLevel);
            sender.sendMessage(ChatColor.GREEN + "Promoted " + target.getName() + " to Level " + nextLevel + " (" + newTitle + ").");

            target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ADMIN PROMOTION!");
            target.sendMessage(ChatColor.AQUA + "You have been promoted to " + newTitle + "!");
            SoundUtils.playSound(target, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            return true;
        }

        return true;
    }

    // --- NEW: TAB COMPLETION ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "quit", "info"));
            if (sender.hasPermission("prisonvaults.admin")) subs.add("promote");
            StringUtil.copyPartialMatches(args[0], subs, completions);
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("Cooking", "Blacksmith"), completions);
            } else if (args[0].equalsIgnoreCase("promote")) {
                return null; // Players
            }
        }

        return completions;
    }
}