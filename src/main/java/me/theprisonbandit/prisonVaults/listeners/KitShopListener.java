package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.kits.Kit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class KitShopListener implements Listener {

    private final PrisonVaults plugin;

    public KitShopListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + "Kit Shop")) return;
        event.setCancelled(true); // Stop taking items

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();

        // Get the name from the item (e.g., "God Kit")
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Kit targetKit = null;
        for (Kit kit : plugin.kitManager.getAllKits()) {
            // 1. Check if the icon type matches (Pickaxe)
            if (event.getCurrentItem().getType() == kit.getIcon().getType()) {

                // 2. Check if the name matches "KitName Kit" exactly
                String expectedName = kit.getName() + " Kit";

                if (itemName.equals(expectedName)) {
                    targetKit = kit;
                    break;
                }
            }
        }

        if (targetKit == null) return;

        // Money Logic
        double balance = plugin.getBalance(player);
        if (balance >= targetKit.getPrice()) {
            plugin.removeMoney(player, targetKit.getPrice());
            plugin.kitManager.giveKit(player, targetKit.getName());

            player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.YELLOW + targetKit.getName() + " Kit" + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this kit!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
        }
    }
}