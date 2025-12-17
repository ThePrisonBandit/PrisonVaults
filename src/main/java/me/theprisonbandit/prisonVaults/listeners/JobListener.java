package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.commands.ShopCommand;
import me.theprisonbandit.prisonVaults.managers.JobManager;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class JobListener implements Listener {

    private final PrisonVaults plugin;

    // Workstation Definitions
    private final Set<Material> COOKING_STATIONS = EnumSet.of(
            Material.FURNACE, Material.SMOKER, Material.CAMPFIRE, Material.SOUL_CAMPFIRE
    );
    private final Set<Material> SMITHING_STATIONS = EnumSet.of(
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.GRINDSTONE,
            Material.BLAST_FURNACE, Material.FURNACE
    );

    public JobListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- A. WORKSTATION INTERACTION (Right-Click Stations) ---
    @EventHandler
    public void onWorkInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        String job = plugin.jobManager.getJob(player);
        ItemStack hand = event.getItem();

        // Cooking: Must hold Raw Food
        if (job.equalsIgnoreCase("Cooking") && COOKING_STATIONS.contains(block.getType())) {
            if (hand != null && JobManager.RAW_FOODS.contains(hand.getType())) {
                performWork(player, "job_cook_action", 5.0, 5.0, Sound.BLOCK_FIRE_EXTINGUISH);
            }
        }
        // Smithing: Must hold Materials
        else if (job.equalsIgnoreCase("Blacksmith") && SMITHING_STATIONS.contains(block.getType())) {
            if (hand != null && JobManager.SMITHING_MATERIALS.contains(hand.getType())) {
                performWork(player, "job_smith_action", 8.0, 8.0, Sound.BLOCK_ANVIL_USE);
            }
        }
    }

    // --- B. CRAFTING QUEST (Workbench) ---
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;

        String job = plugin.jobManager.getJob(player);
        Material type = result.getType();

        // Cooking
        if (job.equalsIgnoreCase("Cooking") && JobManager.COOKING_CRAFTS.contains(type)) {
            performWork(player, "job_craft_action", 8.0, 10.0, Sound.ENTITY_VILLAGER_WORK_FARMER);
        }
        // Smithing
        else if (job.equalsIgnoreCase("Blacksmith") && JobManager.SMITHING_CRAFTS.contains(type)) {
            performWork(player, "job_craft_action", 15.0, 20.0, Sound.BLOCK_SMITHING_TABLE_USE);
        }
    }

    // --- C. SMELTING QUEST (Furnace Extraction) ---
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        String job = plugin.jobManager.getJob(player);
        Material type = event.getItemType();
        int amount = event.getItemAmount();

        if (!canWork(player)) return;

        // 1. Cooking Smelting (Cooked Food)
        if (job.equalsIgnoreCase("Cooking") && isCookedFood(type)) {
            plugin.jobManager.addQuestProgress(player, type, amount, 2.0, 5.0);
        }
        // 2. Blacksmith Smelting (Ingots)
        else if (job.equalsIgnoreCase("Blacksmith") && isSmithingProduct(type)) {
            plugin.jobManager.addQuestProgress(player, type, amount, 5.0, 10.0);
        }
    }

    // --- HANDLE SELL GUI CLOSE (Bulk Restocking) ---
    @EventHandler
    public void onSellGuiClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        // Identify the "Sell GUI" by its title prefix
        if (!title.startsWith(ChatColor.DARK_GREEN + "Restock:") && !title.startsWith(ChatColor.DARK_GRAY + "Restock:")) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // CHECK: Explicitly check for both Mess Hall and Smithy
        String type;
        if (title.contains("Mess Hall")) {
            type = "cooking";
        } else if (title.contains("Smithy")) {
            type = "smithing";
        } else {
            return;
        }

        int itemsAdded = 0;
        double totalValue = 0;

        // Loop through all items dropped in the bin
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                // Validate if item is allowed
                if (plugin.jobManager.isAllowedInShop(type, item.getType())) {

                    // Add to Shop
                    plugin.jobManager.addItemToShop(type, item, player);
                    itemsAdded += item.getAmount();

                    // Pay the player immediately (Bulk Payout)
                    double unitPrice = plugin.jobManager.getBasePrice(item.getType());
                    totalValue += (unitPrice * item.getAmount());

                } else {
                    // Invalid Item -> Give back to player explicitly
                    player.getInventory().addItem(item);
                    player.sendMessage(ChatColor.RED + "Returned " + item.getType().name() + " (Not allowed in this shop).");
                }
            }
        }

        // CRITICAL FIX: Clear the inventory so items don't bounce back to the player on close
        inv.clear();

        if (itemsAdded > 0) {
            plugin.addMoney(player, totalValue);
            player.sendMessage(ChatColor.GREEN + "Stocked " + itemsAdded + " items for $" + NumberUtils.format(totalValue) + "!");
            SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    // --- D. GLOBAL SHOP LISTENER (Click Logic) ---
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isGlobalCooking = title.startsWith(JobManager.TITLE_COOKING_SHOP);
        boolean isGlobalSmithy = title.startsWith(JobManager.TITLE_SMITHY_SHOP);

        if (!isGlobalCooking && !isGlobalSmithy) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 1. Pagination
        if (clickedItem.getType() == Material.ARROW && clickedItem.hasItemMeta()) {
            if (clickedItem.getItemMeta().getLore() != null && !clickedItem.getItemMeta().getLore().isEmpty()) {
                String pageLine = clickedItem.getItemMeta().getLore().get(0);
                try {
                    int page = Integer.parseInt(ChatColor.stripColor(pageLine).replace("Page: ", "").trim());
                    plugin.jobManager.openShop(player, isGlobalCooking ? "cooking" : "smithing", page);
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        // 2. Buying (Clicking an item in the shop)
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE || clickedItem.getType() == Material.BOOK) return;

            // Prevent buying own items? (Optional, currently allowed)
            double balance = plugin.getBalance(player);

            double basePrice = plugin.jobManager.getBasePrice(clickedItem.getType());
            // Attempt to get specific price from Lore
            double lorePrice = plugin.jobManager.getPriceFromItemLore(clickedItem);
            double finalPrice = (lorePrice > 0) ? lorePrice : (basePrice * clickedItem.getAmount());

            if (finalPrice <= 0) return;

            if (balance < finalPrice) {
                player.sendMessage(ChatColor.RED + "Need $" + NumberUtils.format(finalPrice));
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            // Transaction
            plugin.removeMoney(player, finalPrice);

            // Give Item (Strip Lore)
            ItemStack given = clickedItem.clone();
            given.setItemMeta(null); // Remove the "Sold by..." lore
            player.getInventory().addItem(given);

            // Remove from Shop Data
            plugin.jobManager.removeItemFromShop(isGlobalCooking ? "cooking" : "smithing", clickedItem);

            // Refresh Page
            int currentPage = getCurrentPage(title);
            plugin.jobManager.openShop(player, isGlobalCooking ? "cooking" : "smithing", currentPage);

            player.sendMessage(ChatColor.GREEN + "Purchased listing for $" + NumberUtils.format(finalPrice));
            SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
    }

    // --- HELPERS ---
    private boolean isCookedFood(Material mat) {
        return mat == Material.COOKED_BEEF || mat == Material.COOKED_PORKCHOP ||
                mat == Material.COOKED_CHICKEN || mat == Material.COOKED_MUTTON ||
                mat == Material.COOKED_RABBIT || mat == Material.COOKED_COD ||
                mat == Material.COOKED_SALMON || mat == Material.BAKED_POTATO;
    }

    private boolean isSmithingProduct(Material mat) {
        return mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT ||
                mat == Material.COPPER_INGOT || mat == Material.NETHERITE_SCRAP;
    }

    private boolean canWork(Player player) {
        if (plugin.getJobScheduleManager().isJobOpen()) {
            return true;
        }

        if (!plugin.cooldownManager.isOnCooldown(player.getUniqueId(), "job_closed_msg")) {
            plugin.jobScheduleManager.sendClosedMessage(player);
            plugin.cooldownManager.setCooldown(player.getUniqueId(), "job_closed_msg", 5);
        }
        return false;
    }

    private void performWork(Player player, String cdKey, double money, double xp, Sound sound) {
        if (plugin.cooldownManager.isOnCooldown(player.getUniqueId(), cdKey)) return;
        if (!canWork(player)) return;

        plugin.cooldownManager.setCooldown(player.getUniqueId(), cdKey, 2);
        plugin.addMoney(player, money);
        plugin.jobManager.addXp(player, xp);
        SoundUtils.playSound(player, sound, 0.5f, 1.0f);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "+$" + money + " | +" + (int)xp + " XP"));
    }

    private int getCurrentPage(String title) {
        try { return Integer.parseInt(title.split("Page ")[1].trim()); } catch (Exception e) { return 1; }
    }
}