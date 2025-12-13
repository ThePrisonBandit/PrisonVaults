package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class StaffManagerCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public StaffManagerCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // Operator Only
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "This command is for Server Operators only.");
            return true;
        }

        openMainMenu(player);
        return true;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Staff Management");

        inv.setItem(11, createGuiItem(Material.GOLDEN_HELMET, ChatColor.GOLD + "Manage Staff", "View and edit Staff members"));
        inv.setItem(15, createGuiItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "Manage Members", "View and punish regular members"));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private ItemStack createGuiItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> l = new ArrayList<>();
        l.add(ChatColor.GRAY + lore);
        meta.setLore(l);
        item.setItemMeta(meta);
        return item;
    }
}