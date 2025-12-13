package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public static final String TITLE_MESSHALL_BUY = ChatColor.BLUE + "Mess Hall Shop (Admin)";
    public static final String TITLE_SMITHY_BUY = ChatColor.BLUE + "Smithy Shop (Admin)";

    public ShopCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /pvshop <messhall|smithy|buy>");
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. Global Market (Player Listings)
        if (sub.equals("messhall")) {
            int page = 1;
            if (args.length > 1) {
                try { page = Integer.parseInt(args[1]); } catch (NumberFormatException e) {}
            }
            plugin.jobManager.openShop(player, "cooking", page);
            return true;
        }

        if (sub.equals("smithy")) {
            int page = 1;
            if (args.length > 1) {
                try { page = Integer.parseInt(args[1]); } catch (NumberFormatException e) {}
            }
            plugin.jobManager.openShop(player, "smithing", page);
            return true;
        }

        // 2. Admin Shop (Ingredients)
        if (sub.equals("buy")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /pvshop buy <messhall|smithy>");
                return true;
            }
            String shopType = args[1].toLowerCase();
            if (shopType.equals("messhall")) {
                openMessHallAdminShop(player);
                return true;
            }
            if (shopType.equals("smithy")) {
                openSmithyAdminShop(player);
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Unknown shop. Try: messhall, smithy, or buy");
        return true;
    }

    private void openMessHallAdminShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MESSHALL_BUY);

        // Cooking Ingredients
        inv.addItem(createBuyItem(Material.BREAD, 15.0));
        inv.addItem(createBuyItem(Material.BAKED_POTATO, 12.0));
        inv.addItem(createBuyItem(Material.COOKED_BEEF, 20.0));
        inv.addItem(createBuyItem(Material.COOKED_PORKCHOP, 20.0));
        inv.addItem(createBuyItem(Material.COOKED_CHICKEN, 20.0));
        inv.addItem(createBuyItem(Material.COOKED_MUTTON, 20.0));
        inv.addItem(createBuyItem(Material.COOKED_RABBIT, 25.0));
        inv.addItem(createBuyItem(Material.COOKED_COD, 15.0));
        inv.addItem(createBuyItem(Material.COOKED_SALMON, 15.0));
        inv.addItem(createBuyItem(Material.MUSHROOM_STEW, 18.0));
        inv.addItem(createBuyItem(Material.RABBIT_STEW, 30.0));
        inv.addItem(createBuyItem(Material.BEETROOT_SOUP, 18.0));
        inv.addItem(createBuyItem(Material.PUMPKIN_PIE, 40.0));
        inv.addItem(createBuyItem(Material.CAKE, 100.0));
        inv.addItem(createBuyItem(Material.COOKIE, 10.0));
        inv.addItem(createBuyItem(Material.GOLDEN_CARROT, 50.0));
        inv.addItem(createBuyItem(Material.SUGAR, 5.0));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private void openSmithyAdminShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SMITHY_BUY);

        // Smithing Materials
        inv.addItem(createBuyItem(Material.COAL, 5.0));
        inv.addItem(createBuyItem(Material.RAW_IRON, 15.0));
        inv.addItem(createBuyItem(Material.RAW_COPPER, 10.0));
        inv.addItem(createBuyItem(Material.RAW_GOLD, 20.0));
        inv.addItem(createBuyItem(Material.IRON_INGOT, 30.0));
        inv.addItem(createBuyItem(Material.GOLD_INGOT, 40.0));
        inv.addItem(createBuyItem(Material.DIAMOND, 100.0));
        inv.addItem(createBuyItem(Material.NETHERITE_SCRAP, 250.0));

        // Tools & Armor
        inv.addItem(createBuyItem(Material.IRON_SWORD, 100.0));
        inv.addItem(createBuyItem(Material.IRON_PICKAXE, 150.0));
        inv.addItem(createBuyItem(Material.IRON_AXE, 150.0));
        inv.addItem(createBuyItem(Material.IRON_SHOVEL, 60.0));
        inv.addItem(createBuyItem(Material.IRON_HOE, 60.0));
        inv.addItem(createBuyItem(Material.IRON_HELMET, 120.0));
        inv.addItem(createBuyItem(Material.IRON_CHESTPLATE, 200.0));
        inv.addItem(createBuyItem(Material.IRON_LEGGINGS, 180.0));
        inv.addItem(createBuyItem(Material.IRON_BOOTS, 100.0));

        inv.addItem(createBuyItem(Material.DIAMOND_SWORD, 400.0));
        inv.addItem(createBuyItem(Material.DIAMOND_PICKAXE, 600.0));
        inv.addItem(createBuyItem(Material.DIAMOND_AXE, 600.0));
        inv.addItem(createBuyItem(Material.DIAMOND_SHOVEL, 250.0));
        inv.addItem(createBuyItem(Material.DIAMOND_HOE, 250.0));
        inv.addItem(createBuyItem(Material.DIAMOND_HELMET, 500.0));
        inv.addItem(createBuyItem(Material.DIAMOND_CHESTPLATE, 800.0));
        inv.addItem(createBuyItem(Material.DIAMOND_LEGGINGS, 700.0));
        inv.addItem(createBuyItem(Material.DIAMOND_BOOTS, 400.0));

        inv.addItem(createBuyItem(Material.SHIELD, 75.0));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private ItemStack createBuyItem(Material mat, double price) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.RED + "$" + NumberUtils.format(price));
        lore.add(ChatColor.DARK_GRAY + "----------------");
        lore.add(ChatColor.YELLOW + "Left-Click: " + ChatColor.WHITE + "Buy 1");
        lore.add(ChatColor.YELLOW + "Shift-Click: " + ChatColor.WHITE + "Buy 64");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}