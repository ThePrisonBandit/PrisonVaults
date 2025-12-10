package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PickpocketListener implements Listener {

    private final PrisonVaults plugin;
    private static final long COOLDOWN_SECONDS = 7200; // 2 Hours
    private static final double STEAL_CHANCE = 0.0001; // 0.01% Chance
    private static final String PICKPOCKET_TITLE_PREFIX = ChatColor.DARK_RED + "Stealing: ";

    public PickpocketListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- 1. OPEN PICKPOCKET MENU ---
    @EventHandler
    public void onShiftRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        Player thief = event.getPlayer();
        Player victim = (Player) event.getRightClicked();

        // Must be Shift + Right Click
        if (!thief.isSneaking()) return;

        // Permissions
        if (!thief.hasPermission("prisonvaults.pickpocket")) return;

        // Prevent pickpocketing yourself (if lag/glitch allows)
        if (thief.getUniqueId().equals(victim.getUniqueId())) return;

        // Cooldown Check
        if (plugin.cooldownManager.isOnCooldown(thief.getUniqueId(), "pickpocket")) {
            long remaining = plugin.cooldownManager.getRemainingTime(thief.getUniqueId(), "pickpocket");
            thief.sendMessage(ChatColor.RED + "You must wait " + formatTime(remaining) + " before pickpocketing again.");
            return;
        }

        // --- FIND A NON-EMPTY VAULT ---
        FileConfiguration victimData = plugin.getPlayerData(victim.getUniqueId());
        int maxVaults = plugin.getMaxVaults(victim);
        List<Integer> validVaults = new ArrayList<>();

        // Scan all unlocked vaults
        for (int i = 1; i <= maxVaults; i++) {
            if (victimData.contains("vaults." + i)) {
                List<ItemStack> items = (List<ItemStack>) victimData.getList("vaults." + i);
                if (items != null && !items.isEmpty()) {
                    // Check if it's not just empty air
                    boolean hasItem = items.stream().anyMatch(item -> item != null && item.getType() != Material.AIR);
                    if (hasItem) {
                        validVaults.add(i);
                    }
                }
            }
        }

        if (validVaults.isEmpty()) {
            thief.sendMessage(ChatColor.RED + victim.getName() + " has no items in their vaults to steal!");
            return;
        }

        // Pick Random Vault
        int randomVaultNum = validVaults.get(ThreadLocalRandom.current().nextInt(validVaults.size()));

        // Open it (Custom Title allows us to detect it later)
        // Format: "Stealing: <VictimName> #<VaultNum>"
        String title = PICKPOCKET_TITLE_PREFIX + victim.getName() + " #" + randomVaultNum;
        plugin.openVault(thief, randomVaultNum, victim, title); // We need to overload openVault method!

        // Apply Cooldown immediately so they can't spam open
        plugin.cooldownManager.setCooldown(thief.getUniqueId(), "pickpocket", COOLDOWN_SECONDS);
        thief.sendMessage(ChatColor.GRAY + "§oYou quietly pry open one of " + victim.getName() + "'s vaults...");
    }

    // --- 2. HANDLE STEAL ATTEMPT ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(PICKPOCKET_TITLE_PREFIX)) return;

        event.setCancelled(true); // Stop normal movement immediately

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player thief = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // If they clicked air or outside the inventory
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (event.getClickedInventory() == thief.getInventory()) return; // Don't steal your own items

        // Must be Left Click to steal
        if (!event.isLeftClick()) return;

        // --- ROLL THE CHANCE ---
        // Parse Victim Name and Vault Number from title
        // Title format: "Stealing: Name #1"
        try {
            String cleanTitle = ChatColor.stripColor(title).replace("Stealing: ", "");
            String[] parts = cleanTitle.split(" #");
            String victimName = parts[0];
            int vaultNum = Integer.parseInt(parts[1]);

            Player victim = plugin.getServer().getPlayer(victimName);

            // 1. FAIL LOGIC (99.99%)
            if (Math.random() > STEAL_CHANCE) {
                thief.closeInventory();
                thief.sendMessage(ChatColor.RED + "§lBUSTED! §cYou fumbled the lock and " + victimName + " heard you!");

                if (victim != null && victim.isOnline()) {
                    victim.sendMessage(ChatColor.RED + "§lALERT! §c" + thief.getName() + " tried to pickpocket item from your Vault #" + vaultNum + "!");
                }
                return;
            }

            // 2. SUCCESS LOGIC (0.01%)
            // Move item to thief
            thief.getInventory().addItem(clickedItem);

            // Remove from GUI
            event.getClickedInventory().setItem(event.getSlot(), null);

            // SAVE THE VAULT
            if (victim != null && victim.isOnline()) {
                // If online, use main method
                plugin.saveVault(victim, vaultNum, event.getClickedInventory());
                victim.sendMessage(ChatColor.RED + "§lYOU WERE ROBBED! §c" + thief.getName() + " stole an item from Vault #" + vaultNum + "!");
            } else {
                // Offline logic (advanced, but for now we assume online as per shift-click req)
                // Since Shift-Right click requires Entity, they must be online.
            }

            thief.closeInventory();
            thief.sendMessage(ChatColor.GREEN + "§lSUCCESS! §aYou managed to steal a " + clickedItem.getType().name() + "!");

        } catch (Exception e) {
            thief.sendMessage(ChatColor.RED + "Error processing pickpocket.");
            e.printStackTrace();
        }
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%dh %dm %ds", h, m, s);
    }
}