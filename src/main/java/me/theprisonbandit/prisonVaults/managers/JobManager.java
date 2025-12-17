package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JobManager {

    private final PrisonVaults plugin;
    private static final int XP_PER_LEVEL = 100;
    private static final int MAX_LEVEL = 50;

    // --- CONSTANTS ---
    public static final String TITLE_COOKING_SHOP = ChatColor.DARK_GREEN + "Mess Hall Shop";
    public static final String TITLE_SMITHY_SHOP = ChatColor.DARK_GRAY + "The Forge Shop";

    // --- MATERIAL SETS ---
    public static final Set<Material> RAW_FOODS = EnumSet.of(
            Material.BEEF, Material.PORKCHOP, Material.CHICKEN,
            Material.COD, Material.SALMON, Material.MUSHROOM_STEW,
            Material.RABBIT, Material.POTATO, Material.KELP, Material.MUTTON
    );
    // NEW: Cooked Foods (So you can sell the result of your job!)
    public static final Set<Material> COOKED_FOODS = EnumSet.of(
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_COD, Material.COOKED_SALMON, Material.COOKED_MUTTON,
            Material.COOKED_RABBIT, Material.BAKED_POTATO, Material.DRIED_KELP
    );
    public static final Set<Material> COOKING_CRAFTS = EnumSet.of(
            Material.BREAD, Material.CAKE, Material.COOKIE,
            Material.PUMPKIN_PIE, Material.GOLDEN_CARROT,
            Material.SUGAR, Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.BEETROOT_SOUP
    );
    public static final Set<Material> SMITHING_MATERIALS = EnumSet.of(
            Material.RAW_IRON, Material.RAW_COPPER, Material.RAW_GOLD,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.DIAMOND, Material.NETHERITE_SCRAP, Material.NETHERITE_INGOT,
            Material.COAL, Material.CHARCOAL, Material.FLINT
    );
    public static final Set<Material> SMITHING_CRAFTS = EnumSet.of(
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.SHIELD
    );

    // --- STORAGE ---
    private final List<ItemStack> cookingStock = new ArrayList<>();
    private final List<ItemStack> smithingStock = new ArrayList<>();
    private final File shopFile;
    private FileConfiguration shopConfig;
    private final Map<Material, Double> itemPrices = new HashMap<>();

    public JobManager(PrisonVaults plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!shopFile.exists()) {
            try { shopFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        loadPrices();
        loadShops();
    }

    // --- QUEST HELPER ---
    public void addQuestProgress(Player player, Material mat, int amount, double moneyReward, double xpReward) {
        double totalMoney = moneyReward * amount;
        double totalXp = xpReward * amount;
        plugin.addMoney(player, totalMoney);
        addXp(player, totalXp);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "+$" + NumberUtils.format(totalMoney) + " | +" + (int)totalXp + " XP"));
        SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }

    // --- INITIALIZATION ---
    private void loadPrices() {
        itemPrices.clear();
        // Raw Food
        for (Material mat : RAW_FOODS) itemPrices.put(mat, 10.0);
        // Cooked Food
        for (Material mat : COOKED_FOODS) itemPrices.put(mat, 20.0);
        // Crafts
        for (Material mat : COOKING_CRAFTS) itemPrices.put(mat, 25.0);

        // Specific Overrides
        itemPrices.put(Material.CAKE, 100.0);
        itemPrices.put(Material.PUMPKIN_PIE, 40.0);
        itemPrices.put(Material.GOLDEN_CARROT, 50.0);
        itemPrices.put(Material.RABBIT_STEW, 30.0);
        itemPrices.put(Material.SUGAR, 5.0);

        // Smithing Materials
        for (Material mat : SMITHING_MATERIALS) itemPrices.put(mat, 15.0);
        // Smithing Crafts
        for (Material mat : SMITHING_CRAFTS) itemPrices.put(mat, 100.0);

        // Specific Overrides
        itemPrices.put(Material.COAL, 5.0);
        itemPrices.put(Material.CHARCOAL, 5.0);
        itemPrices.put(Material.FLINT, 5.0);
        itemPrices.put(Material.DIAMOND, 100.0);
        itemPrices.put(Material.NETHERITE_SCRAP, 250.0);
        itemPrices.put(Material.NETHERITE_INGOT, 1000.0);
        itemPrices.put(Material.DIAMOND_SWORD, 400.0);
        itemPrices.put(Material.DIAMOND_PICKAXE, 600.0);
        itemPrices.put(Material.DIAMOND_AXE, 600.0);
        itemPrices.put(Material.DIAMOND_SHOVEL, 250.0);
        itemPrices.put(Material.DIAMOND_HOE, 250.0);
        itemPrices.put(Material.DIAMOND_HELMET, 500.0);
        itemPrices.put(Material.DIAMOND_CHESTPLATE, 800.0);
        itemPrices.put(Material.DIAMOND_LEGGINGS, 700.0);
        itemPrices.put(Material.DIAMOND_BOOTS, 400.0);
    }

    // --- SHOP DATA IO ---
    public void loadShops() {
        if (!shopFile.exists()) return;
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        cookingStock.clear();
        if (shopConfig.contains("cooking")) {
            List<?> list = shopConfig.getList("cooking");
            if (list != null) for (Object o : list) if (o instanceof ItemStack) cookingStock.add((ItemStack) o);
        }
        smithingStock.clear();
        if (shopConfig.contains("smithing")) {
            List<?> list = shopConfig.getList("smithing");
            if (list != null) for (Object o : list) if (o instanceof ItemStack) smithingStock.add((ItemStack) o);
        }
    }

    public void saveShops() {
        if (shopConfig == null) shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        shopConfig.set("cooking", cookingStock);
        shopConfig.set("smithing", smithingStock);
        try { shopConfig.save(shopFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // --- GUI MANAGEMENT ---

    // OPEN SELL GUI (Restock)
    public void openSellGui(Player player, String jobName) {
        if (!plugin.getJobScheduleManager().isJobOpen()) {
            plugin.getJobScheduleManager().sendClosedMessage(player);
            return;
        }

        String title = "";
        if (jobName.equalsIgnoreCase("Cooking")) {
            title = ChatColor.DARK_GREEN + "Restock: Mess Hall";
        } else if (jobName.equalsIgnoreCase("Blacksmith")) {
            title = ChatColor.DARK_GRAY + "Restock: Smithy";
        } else {
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, title);
        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GRAY + "Put items in this inventory to add them to the public shop.");
    }

    // OPEN BUY GUI (Shop)
    public void openShop(Player player, String type, int page) {
        if (!plugin.getJobScheduleManager().isJobOpen()) {
            plugin.getJobScheduleManager().sendClosedMessage(player);
            return;
        }

        List<ItemStack> stock = type.equalsIgnoreCase("cooking") ? cookingStock : smithingStock;
        String title = type.equalsIgnoreCase("cooking") ? TITLE_COOKING_SHOP : TITLE_SMITHY_SHOP;

        int totalItems = stock.size();
        int itemsPerPage = 45;
        int maxPage = Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(null, 54, title + " - Page " + page);
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            inv.addItem(stock.get(i));
        }

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);

        if (page > 1) inv.setItem(45, createNavButton(Material.ARROW, ChatColor.YELLOW + "Previous Page", page - 1));
        if (page < maxPage) inv.setItem(53, createNavButton(Material.ARROW, ChatColor.YELLOW + "Next Page", page + 1));
        inv.setItem(49, createNavButton(Material.BOOK, ChatColor.GOLD + "Shop Info", page));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private ItemStack createNavButton(Material mat, String name, int pageData) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Page: " + pageData);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // --- STOCK MANAGEMENT ---
    public void addItemToShop(String type, ItemStack item, Player seller) {
        List<ItemStack> stock = type.equalsIgnoreCase("cooking") ? cookingStock : smithingStock;
        ItemStack soldItem = item.clone();
        ItemMeta meta = soldItem.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        double price = getBasePrice(item.getType()) * item.getAmount();
        lore.add(ChatColor.DARK_GRAY + "----------------");
        lore.add(ChatColor.GRAY + "Sold by: " + ChatColor.AQUA + seller.getName());
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + NumberUtils.format(price));
        lore.add(ChatColor.YELLOW + "Click to Buy");
        meta.setLore(lore);
        soldItem.setItemMeta(meta);
        stock.add(0, soldItem);
        saveShops();
    }

    public void removeItemFromShop(String type, ItemStack item) {
        List<ItemStack> stock = type.equalsIgnoreCase("cooking") ? cookingStock : smithingStock;
        stock.remove(item);
        saveShops();
    }

    // --- HELPER METHODS ---
    public boolean isAllowedInShop(String type, Material mat) {
        if (!itemPrices.containsKey(mat)) return false;

        if (type.equalsIgnoreCase("cooking")) {
            return RAW_FOODS.contains(mat) || COOKING_CRAFTS.contains(mat) || COOKED_FOODS.contains(mat);
        }
        else if (type.equalsIgnoreCase("smithing")) {
            return SMITHING_MATERIALS.contains(mat) || SMITHING_CRAFTS.contains(mat);
        }
        return false;
    }

    public double getBasePrice(Material mat) {
        return itemPrices.getOrDefault(mat, 0.0);
    }

    public double getPriceFromItemLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return 0;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains("Price: $")) {
                String val = ChatColor.stripColor(line).replace("Price: $", "").replace(",", "");
                try { return Double.parseDouble(val); } catch (Exception e) {}
            }
        }
        return 0;
    }

    // --- JOB DATA HANDLING ---
    public String getJob(OfflinePlayer player) {
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        return data.getString("job.current", "None");
    }
    public String getJob(Player player) { return getJob((OfflinePlayer) player); }

    public void joinJob(Player player, String jobName) {
        if (!plugin.getJobScheduleManager().isJobOpen()) {
            plugin.getJobScheduleManager().sendClosedMessage(player);
            return;
        }

        String formattedName = jobName.substring(0, 1).toUpperCase() + jobName.substring(1).toLowerCase();
        File f = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        data.set("job.current", formattedName);
        if (!data.contains("job." + formattedName + ".level")) {
            data.set("job." + formattedName + ".level", 1);
            data.set("job." + formattedName + ".xp", 0.0);
        }
        try { data.save(f); } catch (IOException e) {}
        player.sendMessage(ChatColor.GREEN + "You joined the " + formattedName + " job!");
        plugin.scoreboardManager.updateScoreboard(player);
        SoundUtils.playSound(player, Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.0f);
    }

    public void quitJob(Player player) {
        File f = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        data.set("job.current", "None");
        try { data.save(f); } catch (IOException e) {}
        player.sendMessage(ChatColor.YELLOW + "You quit your job.");
        plugin.scoreboardManager.updateScoreboard(player);
    }

    public int getLevel(OfflinePlayer player) {
        String job = getJob(player);
        if (job.equals("None")) return 0;
        return plugin.getPlayerData(player.getUniqueId()).getInt("job." + job + ".level", 1);
    }

    public double getXp(OfflinePlayer player) {
        String job = getJob(player);
        if (job.equals("None")) return 0.0;
        return plugin.getPlayerData(player.getUniqueId()).getDouble("job." + job + ".xp", 0.0);
    }

    public String getPromotionTitle(String job, int level) {
        if (job.equalsIgnoreCase("Cooking")) {
            if (level < 5) return "Novice";
            if (level < 10) return "Apprentice";
            if (level < 15) return "Sous Chef";
            if (level < 20) return "Head Chef";
            if (level < 25) return "Executive Chef";
            if (level < 30) return "Culinary Master";
            if (level < 50) return "Legendary Cook";
            return "God of Food";
        } else if (job.equalsIgnoreCase("Blacksmith")) {
            if (level < 5) return "Apprentice";
            if (level < 10) return "Journeyman";
            if (level < 15) return "Weaponsmith";
            if (level < 20) return "Armorsmith";
            if (level < 25) return "Master Smith";
            if (level < 30) return "Forge Lord";
            if (level < 50) return "Vulcan's Chosen";
            return "God of the Forge";
        }
        return "Employee";
    }

    public void addXp(Player player, double amount) {
        String job = getJob(player);
        if (job.equals("None")) return;
        int currentLevel = getLevel(player);
        if (currentLevel >= MAX_LEVEL) {
            setXp(player, job, 0);
            return;
        }
        double currentXp = getXp(player);
        double newXp = currentXp + amount;
        if (newXp >= XP_PER_LEVEL) {
            newXp -= XP_PER_LEVEL;
            int newLevel = currentLevel + 1;
            setLevel(player, job, newLevel);
            player.sendMessage(ChatColor.GOLD + "§lLEVEL UP! §eYou are now Level " + newLevel + "!");
            SoundUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            if (newLevel % 5 == 0) {
                String title = getPromotionTitle(job, newLevel);
                player.sendMessage(ChatColor.AQUA + "§lPROMOTION! §bYou have been promoted to " + title + "!");
                SoundUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
        setXp(player, job, newXp);
        plugin.scoreboardManager.updateScoreboard(player);
    }

    public void addQuestProgress(Player player, Material mat, int amount) {
        addXp(player, amount * 2.0);
    }

    public void setLevel(Player p, String job, int lvl) {
        File f = plugin.getPlayerDataFile(p.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("job." + job + ".level", lvl);
        try { d.save(f); } catch (IOException e) {}
    }

    public void setXp(Player p, String job, double xp) {
        File f = plugin.getPlayerDataFile(p.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("job." + job + ".xp", xp);
        try { d.save(f); } catch (IOException e) {}
    }

    public String getBio(OfflinePlayer p) { return plugin.getPlayerData(p.getUniqueId()).getString("bio", "No bio set."); }
    public void setBio(OfflinePlayer p, String b) {
        File f = plugin.getPlayerDataFile(p.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("bio", b);
        try { d.save(f); } catch(Exception e){}
    }

    public JobRank getJobRank(OfflinePlayer p) { return JobRank.EMPLOYEE; }
    public String getJobRankName(OfflinePlayer p) {
        String job = getJob(p);
        if (job.equals("None")) return "N/A";
        int level = getLevel(p);
        return getPromotionTitle(job, level);
    }
    public enum JobRank { EMPLOYEE }
}