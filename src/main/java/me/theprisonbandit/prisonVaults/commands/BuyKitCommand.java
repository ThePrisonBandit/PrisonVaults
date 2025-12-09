package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.kits.Kit;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BuyKitCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public BuyKitCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        Inventory shop = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "Kit Shop");

        for (Kit kit : plugin.kitManager.getAllKits()) {
            // Only show purchasable kits (Price > 0)
            if (kit.getPrice() > 0) {
                ItemStack icon = kit.getIcon().clone();
                ItemMeta meta = icon.getItemMeta();

                // --- NEW: Force the Display Name to be "KitName Kit" ---
                // This hides "Red God Pickaxe" and shows "God Kit" instead
                meta.setDisplayName(ChatColor.AQUA + kit.getName() + " Kit");

                // Add price info lore
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(ChatColor.GRAY + "----------------");
                lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GREEN + "$" + NumberUtils.format(kit.getPrice()));
                lore.add(ChatColor.GRAY + "Click to buy!");
                meta.setLore(lore);

                icon.setItemMeta(meta);
                shop.addItem(icon);
            }
        }

        player.openInventory(shop);
        return true;
    }
}