package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils; // <--- NEW IMPORT
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public SellCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        Player player = (Player) sender;

        // Handle "/sell all"
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            sellAll(player);
            return true;
        }

        // Handle "/sell" (Hand)
        sellHand(player);
        return true;
    }

    private void sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding anything to sell.");
            return;
        }

        double price = plugin.getItemPrice(item.getType());

        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold.");
            return;
        }

        int amount = item.getAmount();
        double totalValue = price * amount;

        // Remove item
        player.getInventory().setItemInMainHand(null);

        // Give money & Update Scoreboard
        plugin.addMoney(player, totalValue);
        plugin.scoreboardManager.setScoreboard(player);

        // <--- UPDATED LINE BELOW --->
        player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + amount + "x " + item.getType().toString() +
                ChatColor.GREEN + " for " + ChatColor.GOLD + "$" + NumberUtils.format(totalValue));
    }

    private void sellAll(Player player) {
        double totalProfit = 0.0;
        int totalItems = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item != null && item.getType() != Material.AIR) {
                double price = plugin.getItemPrice(item.getType());

                if (price > 0) {
                    int amount = item.getAmount();
                    totalProfit += (price * amount);
                    totalItems += amount;

                    // Remove item
                    player.getInventory().setItem(i, null);
                }
            }
        }

        if (totalProfit > 0) {
            plugin.addMoney(player, totalProfit);
            plugin.scoreboardManager.setScoreboard(player);

            // <--- UPDATED LINE BELOW --->
            player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + totalItems + " items" +
                    ChatColor.GREEN + " for a total of " + ChatColor.GOLD + "$" + NumberUtils.format(totalProfit));
        } else {
            player.sendMessage(ChatColor.RED + "You have no sellable items in your inventory.");
        }
    }
}