package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
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
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public ShopCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String action = args[0].toLowerCase();
        String shopType = args[1].toLowerCase();

        if (action.equals("buy")) {
            int page = (args.length > 2) ? parsePage(args[2]) : 1;

            if (shopType.equals("messhall")) {
                plugin.jobManager.openShop(player, "cooking", page);
            }
            else if (shopType.equals("smithy")) {
                plugin.jobManager.openShop(player, "smithing", page);
            }
            else {
                player.sendMessage(ChatColor.RED + "Unknown shop. Options: messhall, smithy");
            }
            return true;
        }

        else if (action.equals("sell")) {
            String currentJob = plugin.jobManager.getJob(player);

            if (shopType.equals("messhall")) {
                if (!currentJob.equalsIgnoreCase("Cooking") && !player.hasPermission("prisonvaults.admin")) {
                    player.sendMessage(ChatColor.RED + "You must be a Cook to restock the Mess Hall!");
                    return true;
                }
                plugin.jobManager.openSellGui(player, "Cooking");
            }
            else if (shopType.equals("smithy")) {
                if (!currentJob.equalsIgnoreCase("Blacksmith") && !player.hasPermission("prisonvaults.admin")) {
                    player.sendMessage(ChatColor.RED + "You must be a Blacksmith to restock the Smithy!");
                    return true;
                }
                plugin.jobManager.openSellGui(player, "Blacksmith");
            }
            else {
                player.sendMessage(ChatColor.RED + "Unknown shop. Options: messhall, smithy");
            }
            return true;
        }
        else {
            sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.RED + "Usage: /pvshop <buy|sell> <messhall|smithy> [page]");
    }

    private int parsePage(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // --- NEW: TAB COMPLETION ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("buy", "sell"), completions);
        }
        else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("messhall", "smithy"), completions);
        }

        return completions;
    }
}