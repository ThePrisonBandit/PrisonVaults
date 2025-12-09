package me.theprisonbandit.prisonVaults.kits;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

        // Inventory check
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Your inventory is full!");
            return;
        }

        for (ItemStack item : kit.getItems()) {
            player.getInventory().addItem(item.clone());
        }
        player.sendMessage(ChatColor.GREEN + "Received kit: " + ChatColor.YELLOW + kit.getName());
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

        // 1. Generate Tools
        items.add(createItem(toolTier, "PICKAXE", name, color, enchantLevel));
        items.add(createItem(toolTier, "SHOVEL", name, color, enchantLevel));

        // 2. Generate Armor
        items.add(createItem(armorTier, "HELMET", name, color, enchantLevel));
        items.add(createItem(armorTier, "CHESTPLATE", name, color, enchantLevel));
        items.add(createItem(armorTier, "LEGGINGS", name, color, enchantLevel));
        items.add(createItem(armorTier, "BOOTS", name, color, enchantLevel));

        // 3. Food (Golden Carrots default)
        items.add(new ItemStack(Material.GOLDEN_CARROT, 64));

        // 4. Create Kit Object
        ItemStack icon = items.get(0).clone(); // Use Pickaxe as icon
        Kit kit = new Kit(name, items, icon, price, "prisonvaults.kit." + name.toLowerCase());

        kits.put(name.toLowerCase(), kit);
        saveKits();
    }

    private ItemStack createItem(String tier, String type, String kitName, ChatColor color, int enchantLvl) {
        // Map Tier Strings to Material (e.g., "WOOD" -> "WOODEN_PICKAXE")
        String matPrefix = tier.toUpperCase();
        if (matPrefix.equals("WOOD")) matPrefix = "WOODEN";
        if (matPrefix.equals("GOLD")) matPrefix = "GOLDEN";

        Material mat = Material.matchMaterial(matPrefix + "_" + type);
        if (mat == null) mat = Material.STONE_PICKAXE; // Fallback

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // Color Name
        String displayName = color + kitName + " " + capitalize(type);
        meta.setDisplayName(displayName);

        // Glow
        meta.addEnchant(Enchantment.UNBREAKING, enchantLvl, true); // Actual enchant
        meta.addEnchant(Enchantment.FORTUNE, enchantLvl, true);    // Actual enchant
        if (type.contains("HELMET") || type.contains("CHEST") || type.contains("LEG") || type.contains("BOOTS")) {
            meta.removeEnchant(Enchantment.FORTUNE);
            meta.addEnchant(Enchantment.PROTECTION, enchantLvl, true);
        }

        // Visual Glow only flag (optional, but requested "glowing")
        // Enchants already make it glow, but this hides the text if you want
        // meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    public void generateRankPresets() {
        // Only generate if they don't exist
        for (char rank = 'A'; rank <= 'Z'; rank++) {
            String kitName = "Rank" + rank;
            if (!kits.containsKey(kitName.toLowerCase())) {
                // Logic: Rank A = Wood/Leather, Rank Z = Netherite
                // This is a simple progression scaler
                String tool = (rank < 'E') ? "WOOD" : (rank < 'J') ? "STONE" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                String armor = (rank < 'E') ? "LEATHER" : (rank < 'J') ? "CHAINMAIL" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                int enchant = (rank - 'A') / 5 + 1; // Increases every 5 ranks

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

        // Ensure defaults exist
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