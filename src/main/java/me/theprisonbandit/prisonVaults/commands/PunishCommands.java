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

public class PunishCommands implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public PunishCommands(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = label.toLowerCase();

        if (cmd.equals("pvwarn") && !sender.hasPermission("prisonvaults.warn")) {
            sender.sendMessage(ChatColor.RED + "No permission (Requires Moderator+).");
            return true;
        }
        if (cmd.equals("pvkick") && !sender.hasPermission("prisonvaults.kick")) {
            sender.sendMessage(ChatColor.RED + "No permission (Requires Moderator+).");
            return true;
        }
        if (cmd.equals("pvban") && !sender.hasPermission("prisonvaults.ban")) {
            sender.sendMessage(ChatColor.RED + "No permission (Requires Admin+).");
            return true;
        }
        if (cmd.equals("pvpardon") && !sender.hasPermission("prisonvaults.pardon")) {
            sender.sendMessage(ChatColor.RED + "No permission (Requires Admin+).");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = (args.length > 1) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Punished by Staff";
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (cmd.equals("pvpardon")) {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
            sender.sendMessage(ChatColor.GREEN + "Unbanned " + targetName + ".");
            plugin.staffMailManager.sendToAllStaff("System", sender.getName() + " unbanned " + targetName);
            return true;
        }

        RankManager.Rank targetRank = plugin.rankManager.getRank(target);
        if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "ERROR: " + ChatColor.RED + "You cannot " + cmd.replace("pv", "") + " an Owner or Co-Owner!");
            if (sender instanceof Player) {
                SoundUtils.playSound((Player) sender, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return true;
        }

        if (cmd.equals("pvwarn")) {
            plugin.staffMailManager.sendToAllStaff("System", sender.getName() + " warned " + target.getName() + " (" + reason + ")");
            sender.sendMessage(ChatColor.GREEN + "Warned " + target.getName());

            if (target.isOnline()) {
                Player onlineTarget = target.getPlayer();
                onlineTarget.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.YELLOW + reason);
                onlineTarget.sendTitle(ChatColor.RED + "WARNING", ChatColor.YELLOW + reason, 10, 70, 20);
                SoundUtils.playSound(onlineTarget, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            } else {
                plugin.mailManager.sendMail("Server", target.getUniqueId(), ChatColor.RED + "WARNING: " + reason);
            }
        }

        else if (cmd.equals("pvkick")) {
            if (target.isOnline()) {
                ((Player) target).kickPlayer(ChatColor.RED + "Kicked: " + ChatColor.WHITE + reason);
                Bukkit.broadcastMessage(ChatColor.GOLD + target.getName() + " was kicked by " + sender.getName() + ".");
                plugin.staffMailManager.sendToAllStaff("System", sender.getName() + " kicked " + target.getName() + " (" + reason + ")");
            } else {
                sender.sendMessage(ChatColor.RED + targetName + " is not online.");
            }
        }

        else if (cmd.equals("pvban")) {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());
            if (target.isOnline()) {
                ((Player) target).kickPlayer(ChatColor.RED + "Banned: " + ChatColor.WHITE + reason);
            }
            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + target.getName() + " was BANNED by " + sender.getName() + "!");
            plugin.staffMailManager.sendToAllStaff("System", sender.getName() + " banned " + target.getName() + " (" + reason + ")");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null; // Players
        } else if (args.length >= 2) {
            return StringUtil.copyPartialMatches(args[args.length-1], Arrays.asList("Hacking", "Griefing", "Spam", "Disrespect", "Abuse"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}