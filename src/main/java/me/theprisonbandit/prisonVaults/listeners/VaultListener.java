package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils; // Import
import org.bukkit.ChatColor;
import org.bukkit.Sound; // Import
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class VaultListener implements Listener {

    private final PrisonVaults plugin;

    public VaultListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Basic check to see if the title starts with our Vault format
        String title = event.getView().getTitle();

        // Strip colors to check the raw text
        String rawTitle = ChatColor.stripColor(title);

        if (rawTitle.startsWith("Vault #")) {
            Player player = (Player) event.getPlayer();

            try {
                // Extract the vault number from the title "Vault #1"
                String numberPart = rawTitle.replace("Vault #", "");
                int vaultNumber = Integer.parseInt(numberPart);

                // Save the data
                plugin.saveVault(player, vaultNumber, event.getInventory());

                // NEW: Close Sound
                SoundUtils.playSound(player, Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);

            } catch (NumberFormatException e) {
                // If title was spoofed or weird, just ignore
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Update the scoreboard immediately when they join
        plugin.scoreboardManager.setScoreboard(event.getPlayer());
    }
}