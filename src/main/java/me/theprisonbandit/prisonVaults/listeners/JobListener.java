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
import org.bukkit.event.inventory.FurnaceExtractEvent; // NEW: Reliable Smelting Event
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class JobListener implements Listener {

    private final PrisonVaults plugin;

    // Workstation Definitions (Keep local or move to Manager if preferred)
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

        // Handle Shift-Click Crafting (Rough estimation: 1 action reward per click)
        // Cooking
        if (job.equalsIgnoreCase("Cooking") && JobManager.COOKING_CRAFTS.contains(type)) {
            performWork(player, "job_craft_action", 8.0, 10.0, Sound.ENTITY_VILLAGER_WORK_FARMER);
        }
        // Smithing
        else if (job.equalsIgnoreCase("Blacksmith") && JobManager.SMITHING_CRAFTS.contains(type)) {
            performWork(player, "job_craft_action", 15.0, 20.0, Sound.BLOCK_SMITHING_TABLE_USE);
        }
    }

    // --- C. SMELTING QUEST (Furnace Extraction - FIXED) ---
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        String job = plugin.jobManager.getJob(player);
        Material type = event.getItemType();
        int amount = event.getItemAmount();

        // 1. Cooking Smelting (Cooked Food)
        if (job.equalsIgnoreCase("Cooking") && isCookedFood(type)) {
            // Reward: $2 and 5 XP per item
            plugin.jobManager.addQuestProgress(player, type, amount, 2.0, 5.0);
        }
        // 2. Blacksmith Smelting (Ingots)
        else if (job.equalsIgnoreCase("Blacksmith") && isSmithingProduct(type)) {
            // Reward: $5 and 10 XP per item
            plugin.jobManager.addQuestProgress(player, type, amount, 5.0, 10.0);
        }
    }

    // Helper: Check if item is a cooked food result
    private boolean isCookedFood(Material mat) {
        return mat == Material.COOKED_BEEF || mat == Material.COOKED_PORKCHOP ||
                mat == Material.COOKED_CHICKEN || mat == Material.COOKED_MUTTON ||
                mat == Material.COOKED_RABBIT || mat == Material.COOKED_COD ||
                mat == Material.COOKED_SALMON || mat == Material.BAKED_POTATO;
    }

    // Helper: Check if item is a smithing smelt result
    private boolean isSmithingProduct(Material mat) {
        return mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT ||
                mat == Material.COPPER_INGOT || mat == Material.NETHERITE_SCRAP;
    }

    // --- HELPER: GENERIC WORK REWARD ---
    private void performWork(Player player, String cdKey, double money, double xp, Sound sound) {
        if (plugin.cooldownManager.isOnCooldown(player.getUniqueId(), cdKey)) return;
        if (!plugin.jobScheduleManager.isWorkDay()) {
            if (!plugin.cooldownManager.isOnCooldown(player.getUniqueId(), "weekend_msg")) {
                plugin.jobScheduleManager.sendWeekendMessage(player);
                plugin.cooldownManager.setCooldown(player.getUniqueId(), "weekend_msg", 5);
            }
            return;
        }

        plugin.cooldownManager.setCooldown(player.getUniqueId(), cdKey, 2);
        plugin.addMoney(player, money);
        plugin.jobManager.addXp(player, xp);
        SoundUtils.playSound(player, sound, 0.5f, 1.0f);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "+$" + money + " | +" + (int)xp + " XP"));
    }

    // --- D. GLOBAL & ADMIN SHOP LISTENER ---
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isAdminMessHall = title.equals(ShopCommand.TITLE_MESSHALL_BUY);
        boolean isAdminSmithy = title.equals(ShopCommand.TITLE_SMITHY_BUY);
        boolean isGlobalCooking = !isAdminMessHall && title.startsWith(JobManager.TITLE_COOKING_SHOP);
        boolean isGlobalSmithy = !isAdminSmithy && title.startsWith(JobManager.TITLE_SMITHY_SHOP);

        if (!isGlobalCooking && !isGlobalSmithy && !isAdminMessHall && !isAdminSmithy) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 1. Pagination
        if ((isGlobalCooking || isGlobalSmithy) && clickedItem.getType() == Material.ARROW && clickedItem.hasItemMeta()) {
            if (clickedItem.getItemMeta().getLore() != null && !clickedItem.getItemMeta().getLore().isEmpty()) {
                String pageLine = clickedItem.getItemMeta().getLore().get(0);
                try {
                    int page = Integer.parseInt(ChatColor.stripColor(pageLine).replace("Page: ", "").trim());
                    plugin.jobManager.openShop(player, isGlobalCooking ? "cooking" : "smithing", page);
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        // 2. Selling (Global Market)
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (isAdminMessHall || isAdminSmithy) return;

            String shopType = isGlobalCooking ? "cooking" : "smithing";
            String job = plugin.jobManager.getJob(player);

            if ((isGlobalCooking && !job.equalsIgnoreCase("Cooking")) || (isGlobalSmithy && !job.equalsIgnoreCase("Blacksmith"))) {
                player.sendMessage(ChatColor.RED + "Wrong job for this shop!");
                return;
            }

            if (!plugin.jobManager.isAllowedInShop(shopType, clickedItem.getType())) {
                player.sendMessage(ChatColor.RED + "Cannot sell this here.");
                return;
            }

            double unitPrice = plugin.jobManager.getBasePrice(clickedItem.getType());
            if (unitPrice <= 0) return;

            ItemStack toSell = clickedItem.clone();
            player.getInventory().setItem(event.getSlot(), null);

            double totalPayout = unitPrice * toSell.getAmount();
            plugin.addMoney(player, totalPayout);
            plugin.jobManager.addItemToShop(shopType, toSell, player);

            int currentPage = getCurrentPage(title);
            plugin.jobManager.openShop(player, shopType, currentPage);

            player.sendMessage(ChatColor.GREEN + "Listed " + toSell.getAmount() + "x " + toSell.getType().name());
            SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            return;
        }

        // 3. Buying
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE || clickedItem.getType() == Material.BOOK) return;

            double balance = plugin.getBalance(player);

            // Admin Buy (Infinite)
            if (isAdminMessHall || isAdminSmithy) {
                double unitPrice = 0;
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                    for (String line : clickedItem.getItemMeta().getLore()) {
                        if (line.contains("Cost: $")) {
                            try { unitPrice = Double.parseDouble(ChatColor.stripColor(line).replace("Cost: $", "").replace(",", "")); } catch (Exception e) {}
                        }
                    }
                }
                if (unitPrice <= 0) return;

                int amount = event.isShiftClick() ? 64 : 1;
                double totalCost = unitPrice * amount;

                if (balance < totalCost) {
                    player.sendMessage(ChatColor.RED + "Need $" + NumberUtils.format(totalCost));
                    SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }

                plugin.removeMoney(player, totalCost);
                ItemStack result = new ItemStack(clickedItem.getType(), amount);
                player.getInventory().addItem(result);
                SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            }
            // Global Buy (Listing)
            else {
                double basePrice = plugin.jobManager.getBasePrice(clickedItem.getType());
                if (basePrice <= 0) return;

                int buyAmount = clickedItem.getAmount();
                double totalCost = basePrice * buyAmount;

                if (balance < totalCost) {
                    player.sendMessage(ChatColor.RED + "Need $" + NumberUtils.format(totalCost));
                    SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }

                plugin.removeMoney(player, totalCost);

                ItemStack given = clickedItem.clone();
                given.setAmount(buyAmount);
                given.setItemMeta(null);
                player.getInventory().addItem(given);

                plugin.jobManager.removeItemFromShop(isGlobalCooking ? "cooking" : "smithing", clickedItem);

                int currentPage = getCurrentPage(title);
                plugin.jobManager.openShop(player, isGlobalCooking ? "cooking" : "smithing", currentPage);

                player.sendMessage(ChatColor.GREEN + "Purchased listing (" + buyAmount + "x) for $" + NumberUtils.format(totalCost));
                SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            }
        }
    }

    private int getCurrentPage(String title) {
        try { return Integer.parseInt(title.split("Page ")[1].trim()); } catch (Exception e) { return 1; }
    }
}