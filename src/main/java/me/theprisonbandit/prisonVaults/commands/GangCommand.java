package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GangCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public GangCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- CREATE ---
        if (sub.equals("create")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /gang create <Tag> <Name>");
                return true;
            }
            if (plugin.gangManager.createGang(player, args[1], args[2]) != null) {
                player.sendMessage(ChatColor.GREEN + "Gang created!");
            }
            return true;
        }

        Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());

        // --- INFO ---
        if (sub.equals("info")) {
            if (gang == null) { player.sendMessage(ChatColor.RED + "No gang."); return true; }
            player.sendMessage(ChatColor.GRAY + "--- " + gang.getFormattedName() + ChatColor.GRAY + " ---");
            player.sendMessage(ChatColor.YELLOW + "Desc: " + ChatColor.WHITE + gang.getDescription());
            player.sendMessage(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(gang.getOwner()).getName());
            return true;
        }

        // --- INVITE (UPDATED) ---
        if (sub.equals("invite")) {
            if (gang == null) return error(player, "No gang.");
            if (gang.getMembers().get(player.getUniqueId()).weight < Rank.ELITE.weight) return error(player, "Rank too low.");
            if (args.length < 2) return error(player, "Usage: /gang invite <player>");

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) return error(player, "Player not found.");

            if (plugin.gangManager.getPlayerGang(target.getUniqueId()) != null) {
                return error(player, "Player is already in a gang.");
            }

            // Use new Manager logic
            plugin.gangManager.invitePlayer(gang, target);
            player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to " + gang.getName());
            return true;
        }

        // --- JOIN (UPDATED) ---
        if (sub.equals("join")) {
            if (gang != null) return error(player, "Leave your current gang first.");
            if (args.length < 2) return error(player, "Usage: /gang join <GangName>");

            String gangName = args[1];
            Gang targetGang = plugin.gangManager.getGangByName(gangName); // Uses Name now

            // Fallback: try tag if name failed
            if (targetGang == null) targetGang = plugin.gangManager.getGangByTag(gangName);

            if (targetGang == null) return error(player, "Gang not found.");

            // Check invite in manager
            if (!plugin.gangManager.hasInvite(player, targetGang.getName())) {
                return error(player, "You have not been invited to join '" + targetGang.getName() + "'.");
            }

            // Join logic
            plugin.gangManager.joinGang(player, targetGang);
            return true;
        }

        // --- MANAGE ---
        if (sub.equals("manage")) {
            if (gang == null) return error(player, "No gang.");
            if (gang.getMembers().get(player.getUniqueId()) != Rank.LEADER) return error(player, "Leader only.");
            openManagerGUI(player, gang);
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void openManagerGUI(Player player, Gang gang) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Gang Manager");
        inv.setItem(1, createItem(Material.NAME_TAG, ChatColor.YELLOW + "Change Name", "Current: " + gang.getName()));
        inv.setItem(2, createItem(Material.PAPER, ChatColor.YELLOW + "Change Description", gang.getDescription()));
        inv.setItem(3, createItem(Material.RED_DYE, ChatColor.YELLOW + "Change Color", "Current: " + gang.getColor()));

        ItemStack membersBtn = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) membersBtn.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(gang.getOwner()));
        skullMeta.setDisplayName(ChatColor.GOLD + "Manage Members");
        skullMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to view/manage members."));
        membersBtn.setItemMeta(skullMeta);
        inv.setItem(4, membersBtn);

        ItemStack disband = new ItemStack(Material.TNT);
        ItemMeta meta1 = disband.getItemMeta();
        meta1.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "DISBAND GANG");
        meta1.setLore(Arrays.asList(ChatColor.GRAY + "Click to permanently delete", ChatColor.GRAY + "your gang."));
        disband.setItemMeta(meta1);
        inv.setItem(5, disband);

        player.openInventory(inv);
    }

    public void openMembersGUI(Player player, Gang gang) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Gang Members");
        int slot = 0;
        for (UUID memberId : gang.getMembers().keySet()) {
            if (memberId.equals(gang.getOwner())) continue;
            if (slot >= 54) break;

            Rank r = gang.getMembers().get(memberId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
            meta.setDisplayName(ChatColor.GOLD + Bukkit.getOfflinePlayer(memberId).getName());
            meta.setLore(Arrays.asList(ChatColor.WHITE + "Rank: " + r.name(), ChatColor.GRAY + "L-Click: Promote", ChatColor.GRAY + "R-Click: Demote", ChatColor.RED + "Shift-Click: Kick"));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Go Back");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private boolean error(Player p, String msg) {
        p.sendMessage(ChatColor.RED + msg);
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.GOLD + "/gang create <Tag> <Name>");
        p.sendMessage(ChatColor.GOLD + "/gang invite <Player>");
        p.sendMessage(ChatColor.GOLD + "/gang join <GangName>");
        p.sendMessage(ChatColor.GOLD + "/gang info");
        p.sendMessage(ChatColor.GOLD + "/gang manage (Leaders)");
    }
}