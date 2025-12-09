package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CreateKitCommand implements CommandExecutor {

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
            sender.sendMessage(ChatColor.RED + "Usage: /createkit <Name> <Color> <Tool:WOOD/IRON...> <Armor:LEATHER/IRON...> <EnchantLvl> <Price>");
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
        sender.sendMessage(ChatColor.GREEN + "Created kit " + name + " successfully!");

        return true;
    }
}