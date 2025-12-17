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
    private final Map<UUID, Map<String, Long>> kitCooldowns = new HashMap<>();

    private File kitsFile;
    private FileConfiguration kitsConfig;

    public KitManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadKits();
    }

    public void resetAllCooldowns() {
        kitCooldowns.clear();
    }

    public boolean isOnCooldown(Player player, String kitName) {
        if (!kitCooldowns.containsKey(player.getUniqueId())) return false;
        Map<String, Long> pCooldowns = kitCooldowns.get(player.getUniqueId());
        if (!pCooldowns.containsKey(kitName.toLowerCase())) return false;
        return System.currentTimeMillis() < pCooldowns.get(kitName.toLowerCase());
    }

    public void setCooldown(Player player, String kitName, long cooldownSeconds) {
        kitCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(kitName.toLowerCase(), System.currentTimeMillis() + (cooldownSeconds * 1000));
    }

    public void giveKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit doesn't exist.");
            return;
        }

        List<ItemStack> itemsToGive = new ArrayList<>();

        for (ItemStack item : kit.getItems()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack toGive = item.clone();
            String type = toGive.getType().name();
            boolean equipped = false;

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

            if (!equipped) {
                itemsToGive.add(toGive);
            }
        }

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
        SoundUtils.playSound(player, Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
    }

    public Kit getKit(String name) { return kits.get(name.toLowerCase()); }
    public Collection<Kit> getAllKits() { return kits.values(); }

    public void createProceduralKit(String name, String colorName, String toolTier, String armorTier, int enchantLevel, double price) {
        List<ItemStack> items = new ArrayList<>();
        ChatColor color = getColor(colorName);

        items.add(createItem(toolTier, "SWORD", name, color, enchantLevel));
        items.add(createItem(toolTier, "AXE", name, color, enchantLevel));
        items.add(createItem(toolTier, "PICKAXE", name, color, enchantLevel));
        items.add(createItem(toolTier, "SHOVEL", name, color, enchantLevel));
        items.add(createItem(armorTier, "HELMET", name, color, enchantLevel));
        items.add(createItem(armorTier, "CHESTPLATE", name, color, enchantLevel));
        items.add(createItem(armorTier, "LEGGINGS", name, color, enchantLevel));
        items.add(createItem(armorTier, "BOOTS", name, color, enchantLevel));
        items.add(createAbilityItem("MedKit", 2));
        items.add(createAbilityItem("SwiftFeet", 1));
        items.add(new ItemStack(Material.GOLDEN_CARROT, 64));

        ItemStack icon = items.get(0).clone();
        Kit kit = new Kit(name, items, icon, price, "prisonvaults.kit." + name.toLowerCase());

        kits.put(name.toLowerCase(), kit);
        saveKits();
    }

    private ItemStack createItem(String tier, String type, String kitName, ChatColor color, int enchantLvl) {
        String matPrefix = tier.toUpperCase();
        if (matPrefix.equals("WOOD")) matPrefix = "WOODEN";
        if (matPrefix.equals("GOLD")) matPrefix = "GOLDEN";

        Material mat = Material.matchMaterial(matPrefix + "_" + type);
        if (mat == null) {
            if (type.equals("SWORD")) mat = Material.STONE_SWORD;
            else if (type.equals("AXE")) mat = Material.STONE_AXE;
            else if (type.equals("SHOVEL")) mat = Material.STONE_SHOVEL;
            else mat = Material.STONE_PICKAXE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String displayName = kitName + " " + type.charAt(0) + type.substring(1).toLowerCase();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Tier: " + tier);

        meta.setDisplayName(color + displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);

        // --- ENCHANTMENT FIX (Unsafe Levels) ---
        // We apply to ItemMeta with ignoreLevelRestriction=true
        // And fallback to ItemStack unsafe if needed.
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, enchantLvl);

        if (type.equals("PICKAXE") || type.equals("SHOVEL") || type.equals("AXE")) {
            item.addUnsafeEnchantment(Enchantment.FORTUNE, enchantLvl);
            item.addUnsafeEnchantment(Enchantment.EFFICIENCY, enchantLvl);
        }

        if (type.contains("HELMET") || type.contains("CHEST") || type.contains("LEG") || type.contains("BOOTS")) {
            item.removeEnchantment(Enchantment.FORTUNE);
            item.addUnsafeEnchantment(Enchantment.PROTECTION, enchantLvl);
        }

        if (type.equals("SWORD") || type.equals("AXE")) {
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, enchantLvl);
        }

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
        for (char rank = 'A'; rank <= 'Z'; rank++) {
            String kitName = "Rank" + rank;
            if (!kits.containsKey(kitName.toLowerCase())) {
                String tool = (rank < 'E') ? "WOOD" : (rank < 'J') ? "STONE" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                String armor = (rank < 'E') ? "LEATHER" : (rank < 'J') ? "CHAINMAIL" : (rank < 'O') ? "IRON" : (rank < 'T') ? "DIAMOND" : "NETHERITE";
                int enchant = (rank - 'A') / 2 + 1; // Increase enchant level scaling
                createProceduralKit(kitName, "GREEN", tool, armor, enchant, 0.0);
            }
        }
        if (!kits.containsKey("starter")) {
            createProceduralKit("Starter", "YELLOW", "WOOD", "LEATHER", 1, 0.0);
        }
    }

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
        if (!kitsFile.exists()) plugin.saveResource("kits.yml", false);
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

    private ChatColor getColor(String name) {
        try { return ChatColor.valueOf(name.toUpperCase()); }
        catch (Exception e) { return ChatColor.WHITE; }
    }
}