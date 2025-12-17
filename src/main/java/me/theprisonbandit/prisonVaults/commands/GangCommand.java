package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Rank;
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
public class GangCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public GangCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- CREATE ---
        if (sub.equals("create")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /gang create <Tag> <Name>");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return true;
            }
            if (plugin.gangManager.createGang(player, args[1], args[2]) != null) {
                player.sendMessage(ChatColor.GREEN + "Gang created!");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else {
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            return true;
        }

        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());

        // --- LEAVE ---
        if (sub.equals("leave")) {
            if (gang == null) {
                return error(player, "You are not in a gang.");
            }
            if (gang.getOwner().equals(player.getUniqueId())) {
                return error(player, "Leaders cannot leave! Use /gang manage to disband.");
            }
            plugin.gangManager.leaveGang(player, gang);
            return true;
        }

        // --- INFO ---
        if (sub.equals("info")) {
            if (gang == null) {
                player.sendMessage(ChatColor.RED + "No gang.");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return true;
            }
            player.sendMessage(ChatColor.GRAY + "--- " + gang.getFormattedName() + ChatColor.GRAY + " ---");
            player.sendMessage(ChatColor.YELLOW + "Desc: " + ChatColor.WHITE + gang.getDescription());
            player.sendMessage(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(gang.getOwner()).getName());
            return true;
        }

        // --- INVITE ---
        if (sub.equals("invite")) {
            if (gang == null) return error(player, "No gang.");
            if (gang.getMembers().get(player.getUniqueId()).weight < Rank.ELITE.weight) return error(player, "Rank too low.");
            if (args.length < 2) return error(player, "Usage: /gang invite <player>");

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) return error(player, "Player not found.");

            if (plugin.gangManager.getPlayerGang(target.getUniqueId()) != null) {
                return error(player, "Player is already in a gang.");
            }

            plugin.gangManager.invitePlayer(gang, target);
            player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to " + gang.getName());
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            SoundUtils.playSound(target, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
            return true;
        }

        // --- JOIN ---
        if (sub.equals("join")) {
            if (gang != null) return error(player, "Leave your current gang first.");
            if (args.length < 2) return error(player, "Usage: /gang join <GangName>");

            String gangName = args[1];
            Gang targetGang = plugin.gangManager.getGangByName(gangName);
            if (targetGang == null) targetGang = plugin.gangManager.getGangByTag(gangName);

            if (targetGang == null) return error(player, "Gang not found.");

            if (!plugin.gangManager.hasInvite(player, targetGang.getName())) {
                return error(player, "You have not been invited to join '" + targetGang.getName() + "'.");
            }

            plugin.gangManager.joinGang(player, targetGang);
            SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            return true;
        }

        // --- MANAGE ---
        if (sub.equals("manage")) {
            if (gang == null) return error(player, "No gang.");
            if (gang.getMembers().get(player.getUniqueId()) != Rank.LEADER) return error(player, "Leader only.");

            plugin.gangManager.openMainGangMenu(player);
            SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return true;
        }

        sendHelp(player);
        return true;
    }

    private boolean error(Player p, String msg) {
        p.sendMessage(ChatColor.RED + msg);
        SoundUtils.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.GOLD + "/gang create <Tag> <Name>");
        p.sendMessage(ChatColor.GOLD + "/gang invite <Player>");
        p.sendMessage(ChatColor.GOLD + "/gang join <GangName>");
        p.sendMessage(ChatColor.GOLD + "/gang leave");
        p.sendMessage(ChatColor.GOLD + "/gang info");
        p.sendMessage(ChatColor.GOLD + "/gang manage (Leaders)");
    }

    // --- NEW: TAB COMPLETION ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "invite", "join", "leave", "info", "manage"), completions);
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite")) {
                return null; // Return null to autocomplete online players
            } else if (args[0].equalsIgnoreCase("join")) {
                // Autocomplete gang names the player is invited to (or all gangs)
                List<String> gangNames = new ArrayList<>();
                for (Gang g : plugin.gangManager.getAllGangs()) {
                    gangNames.add(g.getName());
                }
                StringUtil.copyPartialMatches(args[1], gangNames, completions);
            }
        }

        return completions;
    }
}