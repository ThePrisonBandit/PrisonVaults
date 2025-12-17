package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.util.StringUtil; // Added

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateKitCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public CreateKitCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /createkit <Name> <Color> <Material:WOOD/IRON/DIAMOND> <Armor:LEATHER/IRON/DIAMOND> <EnchantLvl> <Price>");
            return true;
        }

        String name = args[0];
        String color = args[1];
        String tool = args[2];
        String armor = args[3];
        int enchant;
        double price;

        try {
            enchant = Integer.parseInt(args[4]);
            price = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Enchant level and Price must be numbers.");
            return true;
        }

        plugin.kitManager.createProceduralKit(name, color, tool, armor, enchant, price);
        sender.sendMessage(ChatColor.GREEN + "Created procedural kit " + name + " (Includes Shiv, Cleaver, Armor & Abilities) successfully!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1: // Name
                completions.add("<Name>");
                break;
            case 2: // Color
                StringUtil.copyPartialMatches(args[1], Arrays.asList("&a", "&b", "&c", "&d", "&e", "&f", "&1", "&2", "&3"), completions);
                break;
            case 3: // Tool Material
                StringUtil.copyPartialMatches(args[2], Arrays.asList("WOOD", "STONE", "IRON", "GOLD", "DIAMOND", "NETHERITE"), completions);
                break;
            case 4: // Armor Material
                StringUtil.copyPartialMatches(args[3], Arrays.asList("LEATHER", "CHAINMAIL", "IRON", "GOLD", "DIAMOND", "NETHERITE"), completions);
                break;
            case 5: // Enchant Level
                StringUtil.copyPartialMatches(args[4], Arrays.asList("1", "2", "3", "4", "5", "10"), completions);
                break;
        }
        return completions;
    }
}