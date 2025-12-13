package me.theprisonbandit.prisonVaults.kits;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final PrisonVaults plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private File kitsFile;
    private FileConfiguration kitsConfig;

    public KitManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadKits();
    }

    // --- MAIN LOGIC ---

    public void giveKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit doesn't exist.");
            return;
        }

        // List to hold items that need to go into the inventory (not equipped)
        List<ItemStack> itemsToGive = new ArrayList<>();
        boolean armorEquipped = false;

        for (ItemStack item : kit.getItems()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack toGive = item.clone();
            String type = toGive.getType().name();
            boolean equipped = false;

            // --- AUTO EQUIP LOGIC ---
            if (type.endsWith("_HELMET") && player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(toGive);
                equipped = true;
            }
            else if (type.endsWith("_CHESTPLATE") && player.getInventory().getChestplate() == null) {
                player.getInventory().setChestplate(toGive);
                equipped = true;
            }
            else if (type.endsWith("_LEGGINGS") && player.getInventory().getLeggings() == null) {
                player.getInventory().setLeggings(toGive);
                equipped = true;
            }
            else if (type.endsWith("_BOOTS") && player.getInventory().getBoots() == null) {
                player.getInventory().setBoots(toGive);
                equipped = true;
            }

            if (equipped) {
                armorEquipped = true;
            } else {
                itemsToGive.add(toGive);
            }
        }

        // Add remaining items to inventory or drop them if full
        boolean inventoryFull = false;
        if (!itemsToGive.isEmpty()) {
            for (ItemStack item : itemsToGive) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    inventoryFull = true;
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
        }

        if (inventoryFull) {
            player.sendMessage(ChatColor.YELLOW + "Inventory full! Some kit items were dropped on the ground.");
        }

        player.sendMessage(ChatColor.GREEN + "Received kit: " + ChatColor.YELLOW + kit.getName());

        // Optional Sound
        SoundUtils.playSound(player, Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    // --- CREATION LOGIC ---

    public void createProceduralKit(String name, String colorName, String toolTier, String armorTier, int enchantLevel, double price) {
        List<ItemStack> items = new ArrayList<>();
        ChatColor color = getColor(colorName);

        // 1. Generate Tools & Weapons (Shivs, Cleavers, etc.)
        items.add(createItem(toolTier, "SWORD", name, color, enchantLevel)); // Shiv
        items.add(createItem(toolTier, "AXE", name, color, enchantLevel));   // Cleaver
        items.add(createItem(toolTier, "PICKAXE", name, color, enchantLevel));
        items.add(createItem(toolTier, "SHOVEL", name, color, enchantLevel)); // Digging Shovel

        // 2. Generate Armor (Combat Armor)
        items.add(createItem(armorTier, "HELMET", name, color, enchantLevel));
        items.add(createItem(armorTier, "CHESTPLATE", name, color, enchantLevel));
        items.add(createItem(armorTier, "LEGGINGS", name, color, enchantLevel));
        items.add(createItem(armorTier, "BOOTS", name, color, enchantLevel));

        // 3. Abilities & Food
        items.add(createAbilityItem("MedKit", 2));
        items.add(createAbilityItem("SwiftFeet", 1));
        items.add(new ItemStack(Material.GOLDEN_CARROT, 64));

        // 4. Create Kit Object
        ItemStack icon = items.get(0).clone(); // Use first item (Sword/Shiv) as icon
        Kit kit = new Kit(name, items, icon, price, "prisonvaults.kit." + name.toLowerCase());

        kits.put(name.toLowerCase(), kit);
        saveKits();
    }

    private ItemStack createItem(String tier, String type, String kitName, ChatColor color, int enchantLvl) {
        String matPrefix = tier.toUpperCase();
        if (matPrefix.equals("WOOD")) matPrefix = "WOODEN";
        if (matPrefix.equals("GOLD")) matPrefix = "GOLDEN";

        Material mat = Material.matchMaterial(matPrefix + "_" + type);
        // Better fallbacks for different types
        if (mat == null) {
            if (type.equals("SWORD")) mat = Material.STONE_SWORD;
            else if (type.equals("AXE")) mat = Material.STONE_AXE;
            else if (type.equals("SHOVEL")) mat = Material.STONE_SHOVEL;
            else mat = Material.STONE_PICKAXE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // --- CUSTOM NAMING & LORE ---
        String displayName = "";
        List<String> lore = new ArrayList<>();

        switch (type) {
            case "SWORD":
                displayName = kitName + " Shiv";
                lore.add(ChatColor.GRAY + "Right-Click to Shank.");
                lore.add(ChatColor.RED + "Deals bleed damage over time.");
                break;
            case "AXE":
                displayName = kitName + " Cleaver";
                lore.add(ChatColor.RED + "Always deals Critical Hits.");
                break;
            case "SHOVEL":
                displayName = kitName + " Digging Shovel";
                lore.add(ChatColor.GOLD + "Digs a 3x3 area.");
                break;
            case "PICKAXE":
                displayName = kitName + " Pickaxe";
                lore.add(ChatColor.GRAY + "Standard issue mining tool.");
                break;
            case "HELMET":
                displayName = kitName + " Combat Helmet";
                lore.add(ChatColor.BLUE + "Head protection.");
                break;
            case "CHESTPLATE":
                displayName = kitName + " Body Armor";
                lore.add(ChatColor.BLUE + "Heavy torso protection.");
                break;
            case "LEGGINGS":
                displayName = kitName + " Combat Pants";
                lore.add(ChatColor.BLUE + "Tactical legwear.");
                break;
            case "BOOTS":
                displayName = kitName + " Combat Boots";
                lore.add(ChatColor.BLUE + "Reinforced footwear.");
                break;
        }

        meta.setDisplayName(color + displayName);

        // --- ENCHANTS ---
        meta.addEnchant(Enchantment.UNBREAKING, enchantLvl, true);

        if (type.equals("PICKAXE") || type.equals("SHOVEL") || type.equals("AXE")) {
            meta.addEnchant(Enchantment.FORTUNE, enchantLvl, true);
            meta.addEnchant(Enchantment.EFFICIENCY, enchantLvl, true);
        }

        if (type.contains("HELMET") || type.contains("CHEST") || type.contains("LEG") || type.contains("BOOTS")) {
            meta.removeEnchant(Enchantment.FORTUNE); // Remove default fortune
            meta.addEnchant(Enchantment.PROTECTION, enchantLvl, true);
        }

        if (type.equals("SWORD") || type.equals("AXE")) {
            meta.addEnchant(Enchantment.SHARPNESS, enchantLvl, true);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAbilityItem(String ability, int amount) {
        ItemStack item;
        String name;
        List<String> lore = new ArrayList<>();

        if (ability.equalsIgnoreCase("MedKit")) {
            item = new ItemStack(Material.PAPER, amount);
            name = ChatColor.RED + "" + ChatColor.BOLD + "MedKit";
            lore.add(ChatColor.GRAY + "Right-Click to Heal 4 Hearts.");
        } else {
            item = new ItemStack(Material.FEATHER, amount);
            name = ChatColor.AQUA + "" + ChatColor.BOLD + "SwiftFeet";
            lore.add(ChatColor.GRAY + "Right-Click for Speed II (10s).");
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return item;
    }

    public void generateRankPresets() {
        // Only generate if they don't exist
        for (char rank = 'A'; rank <= 'Z'; rank++) {
            String kitName = "Rank" + rank;
            if (!kits.containsKey(kitName.toLowerCase())) {
                String tool = (rank < 'E') ? "WOOD" : (rank < 'J') ? "STONE" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                String armor = (rank < 'E') ? "LEATHER" : (rank < 'J') ? "CHAINMAIL" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                int enchant = (rank - 'A') / 5 + 1;

                createProceduralKit(kitName, "GREEN", tool, armor, enchant, 0.0);
            }
        }
        // Starter Kit
        if (!kits.containsKey("starter")) {
            createProceduralKit("Starter", "YELLOW", "WOOD", "LEATHER", 1, 0.0);
        }
    }

    // --- FILE I/O ---

    private void saveKits() {
        for (Kit kit : kits.values()) {
            String path = "kits." + kit.getName().toLowerCase();
            kitsConfig.set(path + ".name", kit.getName());
            kitsConfig.set(path + ".price", kit.getPrice());
            kitsConfig.set(path + ".perm", kit.getPermission());
            kitsConfig.set(path + ".icon", kit.getIcon());
            kitsConfig.set(path + ".items", kit.getItems());
        }
        try { kitsConfig.save(kitsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadKits() {
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);

        if (kitsConfig.contains("kits")) {
            for (String key : kitsConfig.getConfigurationSection("kits").getKeys(false)) {
                ConfigurationSection sec = kitsConfig.getConfigurationSection("kits." + key);
                String name = sec.getString("name");
                double price = sec.getDouble("price");
                String perm = sec.getString("perm");
                ItemStack icon = sec.getItemStack("icon");
                List<ItemStack> items = (List<ItemStack>) sec.getList("items");

                kits.put(name.toLowerCase(), new Kit(name, items, icon, price, perm));
            }
        }
        generateRankPresets();
    }

    // --- UTILS ---
    private ChatColor getColor(String name) {
        try { return ChatColor.valueOf(name.toUpperCase()); }
        catch (Exception e) { return ChatColor.WHITE; }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}