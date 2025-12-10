package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class JobListener implements Listener {

    private final PrisonVaults plugin;

    public JobListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFurnaceExtract(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        // Progress quest logic
        if (plugin.gangManager != null) { // Actually JobManager, accessing via main
            // Since JobManager is new, we will access via plugin.jobManager
            // Check below for Main class update
            plugin.jobManager.addQuestProgress(player, item.getType(), item.getAmount());
        }
    }
}