package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class ProfileListener implements Listener {

    // Added Plugin instance to access GangManager
    private final PrisonVaults plugin;

    public ProfileListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProfileClick(InventoryClickEvent event) {
        // 1. Check if the menu is the Profile GUI
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
                    player.sendMessage("   " + line);
                }
            }

            // --- NEW: FETCH GANG RANK FOR CHAT ---
            if (clickedItem.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                // Get the player who owns the skull (The profile owner)
                if (meta.getOwningPlayer() != null) {
                    UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                    Gang gang = plugin.gangManager.getPlayerGang(targetUUID);

                    if (gang != null) {
                        // Get the rank of the target player
                        Rank rank = gang.getMembers().get(targetUUID);
                        if (rank != null) {
                            player.sendMessage("   " + ChatColor.YELLOW + "Gang Rank: " + ChatColor.WHITE + rank.display);
                        }
                    }
                }
            }
            // -------------------------------------

            player.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------");
        }
    }
}