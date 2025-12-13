package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PickpocketListener implements Listener {

    private final PrisonVaults plugin;
    private static final long COOLDOWN_SECONDS = 7200; // 2 Hours
    // Chance is now in Config (pickpocket.success-chance)
    private static final String PICKPOCKET_TITLE_PREFIX = ChatColor.DARK_RED + "Stealing: ";

    public PickpocketListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- 1. OPEN PICKPOCKET MENU ---
    @EventHandler
    public void onShiftRightClick(PlayerInteractEntityEvent event) {
        // Bugfix: Prevent Double Execution
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player)) return;

        Player thief = event.getPlayer();
        Player victim = (Player) event.getRightClicked();

        if (!thief.isSneaking()) return;
        if (!thief.hasPermission("prisonvaults.pickpocket")) return;
        if (thief.getUniqueId().equals(victim.getUniqueId())) return;

        // Cooldown Check
        if (plugin.cooldownManager.isOnCooldown(thief.getUniqueId(), "pickpocket")) {
            long remaining = plugin.cooldownManager.getRemainingTime(thief.getUniqueId(), "pickpocket");
            thief.sendMessage(ChatColor.RED + "You must wait " + formatTime(remaining) + " before pickpocketing again.");
            SoundUtils.playSound(thief, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // Find Non-Empty Vaults
        FileConfiguration victimData = plugin.getPlayerData(victim.getUniqueId());
        int maxVaults = plugin.getMaxVaults(victim);
        List<Integer> validVaults = new ArrayList<>();

        for (int i = 1; i <= maxVaults; i++) {
            if (victimData.contains("vaults." + i)) {
                List<ItemStack> items = (List<ItemStack>) victimData.getList("vaults." + i);
                if (items != null && !items.isEmpty()) {
                    boolean hasItem = items.stream().anyMatch(item -> item != null && item.getType() != Material.AIR);
                    if (hasItem) validVaults.add(i);
                }
            }
        }

        if (validVaults.isEmpty()) {
            thief.sendMessage(ChatColor.RED + victim.getName() + " has no items in their vaults to steal!");
            SoundUtils.playSound(thief, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        int randomVaultNum = validVaults.get(ThreadLocalRandom.current().nextInt(validVaults.size()));
        String title = PICKPOCKET_TITLE_PREFIX + victim.getName() + " #" + randomVaultNum;
        plugin.openVault(thief, randomVaultNum, victim, title);

        plugin.cooldownManager.setCooldown(thief.getUniqueId(), "pickpocket", COOLDOWN_SECONDS);
        thief.sendMessage(ChatColor.GRAY + "§oYou quietly pry open one of " + victim.getName() + "'s vaults...");

        SoundUtils.playSound(thief, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.5f);
    }

    // --- 2. HANDLE STEAL ATTEMPT ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(PICKPOCKET_TITLE_PREFIX)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player thief = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (event.getClickedInventory() == thief.getInventory()) return;
        if (!event.isLeftClick()) return;

        try {
            String cleanTitle = ChatColor.stripColor(title).replace("Stealing: ", "");
            String[] parts = cleanTitle.split(" #");
            String victimName = parts[0];
            int vaultNum = Integer.parseInt(parts[1]);

            Player victim = plugin.getServer().getPlayer(victimName);

            // --- NEW: CONFIGURABLE CHANCE ---
            // Default 0.01 (1%)
            double successChance = plugin.getConfig().getDouble("pickpocket.success-chance", 0.01);

            // 1. FAIL LOGIC
            if (Math.random() > successChance) {
                thief.closeInventory();
                thief.sendMessage(ChatColor.RED + "§lBUSTED! §cYou fumbled the lock and " + victimName + " heard you!");

                if (victim != null && victim.isOnline()) {
                    victim.sendMessage(ChatColor.RED + "§lALERT! §c" + thief.getName() + " tried to pickpocket item from your Vault #" + vaultNum + "!");
                }
                SoundUtils.playDualSound(thief, victim, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // 2. SUCCESS LOGIC
            thief.getInventory().addItem(clickedItem);
            event.getClickedInventory().setItem(event.getSlot(), null);

            if (victim != null && victim.isOnline()) {
                plugin.saveVault(victim, vaultNum, event.getClickedInventory());
                victim.sendMessage(ChatColor.RED + "§lYOU WERE ROBBED! §c" + thief.getName() + " stole an item from Vault #" + vaultNum + "!");
                SoundUtils.playSound(victim, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }

            thief.closeInventory();
            thief.sendMessage(ChatColor.GREEN + "§lSUCCESS! §aYou managed to steal a " + clickedItem.getType().name() + "!");
            SoundUtils.playSound(thief, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

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