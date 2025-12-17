package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.util.StringUtil; // Added

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PermsCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public PermsCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "This command is for Operators only.");
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String type = args[0].toLowerCase();

        // --- GROUP MANAGEMENT ---
        if (type.equals("group")) {
            String groupName = args[1];

            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("create")) {
                    plugin.permissionManager.createGroup(groupName);
                    sender.sendMessage(ChatColor.GREEN + "Group '" + groupName + "' created.");
                    return true;
                }
                if (args[2].equalsIgnoreCase("delete") || args[2].equalsIgnoreCase("remove")) {
                    plugin.permissionManager.deleteGroup(groupName);
                    sender.sendMessage(ChatColor.YELLOW + "Group '" + groupName + "' deleted.");
                    return true;
                }
            }

            if (args.length >= 4) {
                String action = args[2].toLowerCase();
                String permission = args[3];

                if (action.equals("add")) {
                    plugin.permissionManager.addPermissionToGroup(groupName, permission);
                    sender.sendMessage(ChatColor.GREEN + "Added '" + permission + "' to group " + groupName);
                } else if (action.equals("remove")) {
                    plugin.permissionManager.removePermissionFromGroup(groupName, permission);
                    sender.sendMessage(ChatColor.YELLOW + "Removed '" + permission + "' from group " + groupName);
                }
                return true;
            }
        }

        // --- USER MANAGEMENT ---
        if (type.equals("user")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("reset")) {
                    plugin.permissionManager.setGroup(target.getUniqueId(), "default");
                    sender.sendMessage(ChatColor.YELLOW + "Reset " + target.getName() + " to default group.");
                    return true;
                }

                if (args.length >= 4 && args[2].equalsIgnoreCase("setgroup")) {
                    String newGroup = args[3];
                    plugin.permissionManager.setGroup(target.getUniqueId(), newGroup);
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s group to " + newGroup);
                    return true;
                }
            }
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> create");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> delete");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> add <permission>");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> remove <permission>");
        sender.sendMessage(ChatColor.GRAY + "/pvperm user <player> setgroup <group>");
        sender.sendMessage(ChatColor.GRAY + "/pvperm user <player> reset");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("group", "user"), completions);
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("user")) {
                return null; // players
            }
            // For group, we just let them type a name, no specific autocomplete unless we fetch existing groups (which we can if we had a method)
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("group")) {
                StringUtil.copyPartialMatches(args[2], Arrays.asList("create", "delete", "add", "remove"), completions);
            } else if (args[0].equalsIgnoreCase("user")) {
                StringUtil.copyPartialMatches(args[2], Arrays.asList("setgroup", "reset"), completions);
            }
        }
        return completions;
    }
}