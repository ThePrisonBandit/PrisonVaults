package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils; // <--- NEW IMPORT
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddMoneyCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public AddMoneyCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Permission Check
        if (!sender.hasPermission("prisonvaults.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this.");
            return true;
        }

        // 2. Check Arguments
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /addmoney <player> <amount>");
            return true;
        }

        // 3. Get the Target Player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
            return true;
        }

        // 4. Parse the Amount
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount. Please enter a number.");
            return true;
        }

        // 5. Add the money & Update Target Scoreboard
        plugin.addMoney(target, amount);
        plugin.scoreboardManager.setScoreboard(target);

        // 6. Success Messages (UPDATED)
        sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.GOLD + "$" + NumberUtils.format(amount) +
                ChatColor.GREEN + " to " + target.getName());

        target.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + "$" + NumberUtils.format(amount) +
                ChatColor.GREEN + " from an admin!");

        return true;
    }
}