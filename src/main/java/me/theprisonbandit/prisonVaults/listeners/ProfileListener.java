package me.theprisonbandit.prisonVaults.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ProfileListener implements Listener {

    @EventHandler
    public void onProfileClick(InventoryClickEvent event) {
        // 1. Check if the menu is the Profile GUI
        // We look for the start of the title string we set in ProfileCommand
        if (!event.getView().getTitle().startsWith(ChatColor.DARK_GRAY + "Profile: ")) {
            return;
        }

        // 2. Always cancel interaction so they can't take items
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // 3. Check if they clicked the Player Head (Slot 13)
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) {
            return;
        }

        // 4. Handle Left Click
        if (event.isLeftClick()) {
            player.closeInventory(); // Close the GUI

            // 5. Send info to chat
            player.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------");

            // Print the Name (The item display name)
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                player.sendMessage("   " + clickedItem.getItemMeta().getDisplayName() + "§r's Profile");
            }

            // Print the Stats (The lore lines)
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                List<String> lore = clickedItem.getItemMeta().getLore();
                for (String line : lore) {
                    // We print the line exactly as it was in the GUI
                    player.sendMessage("   " + line);
                }
            }
            player.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------");
        }
    }
}