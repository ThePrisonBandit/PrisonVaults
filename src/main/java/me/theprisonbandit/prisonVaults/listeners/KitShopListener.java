package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.kits.Kit;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitShopListener implements Listener {

    private final PrisonVaults plugin;

    public KitShopListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + "Kit Shop")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        ItemStack clicked = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        // --- NEW: CLOSE BUTTON LOGIC ---
        // If you add a Barrier named "Close" to your Kit GUI, this will handle it.
        if (clicked.getType() == Material.BARRIER && itemName.contains("Close")) {
            player.closeInventory();
            return;
        }

        Kit targetKit = null;
        for (Kit kit : plugin.kitManager.getAllKits()) {
            if (clicked.getType() == kit.getIcon().getType()) {
                String expectedName = kit.getName() + " Kit";
                if (itemName.equals(expectedName)) {
                    targetKit = kit;
                    break;
                }
            }
        }

        if (targetKit == null) return;

        double balance = plugin.getBalance(player);
        if (balance >= targetKit.getPrice()) {
            plugin.removeMoney(player, targetKit.getPrice());
            plugin.kitManager.giveKit(player, targetKit.getName());

            player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.YELLOW + targetKit.getName() + " Kit" + ChatColor.GREEN + "!");
            SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

            // CHANGED: Removed player.closeInventory() to keep shop open
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this kit!");
            SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
        }
    }
}