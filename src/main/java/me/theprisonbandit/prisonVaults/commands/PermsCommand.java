package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PermsCommand implements CommandExecutor {

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

        String type = args[0].toLowerCase(); // "user" or "group"

        // --- GROUP MANAGEMENT ---
        if (type.equals("group")) {
            String groupName = args[1];
            if (args.length < 4) {
                if (args.length == 3 && args[2].equalsIgnoreCase("create")) {
                    plugin.permissionManager.createGroup(groupName);
                    sender.sendMessage(ChatColor.GREEN + "Group '" + groupName + "' created.");
                    return true;
                }
                sendHelp(sender);
                return true;
            }

            String action = args[2].toLowerCase(); // "add" or "remove"
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

        // --- USER MANAGEMENT ---
        if (type.equals("user")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (args.length < 4) {
                sendHelp(sender);
                return true;
            }

            // /pvperm user <player> setgroup <group>
            if (args[2].equalsIgnoreCase("setgroup")) {
                String newGroup = args[3];
                if (!plugin.permissionManager.getGroups().contains(newGroup)) {
                    sender.sendMessage(ChatColor.RED + "Group does not exist. Create it first.");
                    return true;
                }
                plugin.permissionManager.setGroup(target.getUniqueId(), newGroup);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s group to " + newGroup);
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> create");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> add <permission>");
        sender.sendMessage(ChatColor.GRAY + "/pvperm group <name> remove <permission>");
        sender.sendMessage(ChatColor.GRAY + "/pvperm user <player> setgroup <group>");
    }
}