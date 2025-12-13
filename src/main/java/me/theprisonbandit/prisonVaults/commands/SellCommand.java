package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import me.theprisonbandit.prisonVaults.utils.SoundUtils; // Import
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            sellAll(player);
        } else {
            sellHand(player);
        }
        return true;
    }

    private void sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding anything.");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        double price = plugin.getItemPrice(item.getType());
        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold.");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        double totalValue = price * item.getAmount();

        player.getInventory().setItemInMainHand(null);
        plugin.addMoney(player, totalValue);
        plugin.scoreboardManager.updateScoreboard(player);

        player.sendMessage(ChatColor.GREEN + "Sold " + item.getType() + " for $" + NumberUtils.format(totalValue));
        SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void sellAll(Player player) {
        double totalProfit = 0.0;
        int totalItems = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                double price = plugin.getItemPrice(item.getType());
                if (price > 0) {
                    totalProfit += (price * item.getAmount());
                    totalItems += item.getAmount();
                    player.getInventory().setItem(i, null);
                }
            }
        }

        if (totalProfit > 0) {
            plugin.addMoney(player, totalProfit);
            plugin.scoreboardManager.updateScoreboard(player);
            player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " items for $" + NumberUtils.format(totalProfit));
            SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(ChatColor.RED + "No sellable items.");
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }
}