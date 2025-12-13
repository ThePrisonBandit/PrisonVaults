package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Rank;
import me.theprisonbandit.prisonVaults.utils.SoundUtils; // Import
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound; // Import
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GangListener implements Listener {
    private final PrisonVaults plugin;

    public GangListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- GUI CLICK HANDLER ---
    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (e.getCurrentItem() == null) return;
        Player player = (Player) e.getWhoClicked();

        // 1. MAIN GANG MANAGER (Settings & Disband)
        if (title.equals(ChatColor.DARK_GRAY + "Gang Manager")) {
            e.setCancelled(true);

            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            ItemStack item = e.getCurrentItem();

            // Settings
            if (item.getType() == Material.NAME_TAG) {
                startChatInput(player, "NAME");
            } else if (item.getType() == Material.PAPER) {
                startChatInput(player, "DESC");
            } else if (item.getType() == Material.RED_DYE) {
                openColorGUI(player);
            }

            // Disband Button
            else if (item.getType() == Material.TNT) {
                // Security Check: Only Owner can disband
                if (!gang.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the Gang Owner can disband the gang!");
                    SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    player.closeInventory();
                    return;
                }

                // Execute Disband
                plugin.gangManager.disbandGang(gang);
                player.closeInventory();

                // NEW: Disband Sound
                SoundUtils.playSound(player, Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.5f);
            }

            // OPEN MEMBERS GUI (Clicking the Leader Head)
            else if (item.getType() == Material.PLAYER_HEAD) {
                openMembersGUI(player, gang);
            }
            // NEW: Generic Click Sound
            else {
                SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }

        // 2. MEMBER LIST GUI (Promote/Kick/Demote)
        else if (title.equals(ChatColor.DARK_GRAY + "Gang Members")) {
            e.setCancelled(true);

            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            ItemStack item = e.getCurrentItem();

            // Back Button
            if (item.getType() == Material.ARROW) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type /gang manager to return to the main menu.");
                SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }

            // Handle Member Actions
            if (item.getType() == Material.PLAYER_HEAD) {
                handleMemberClick(e, gang, player, item);
                // Refresh the GUI to show changes (unless kicked, which closes it)
                if (player.getOpenInventory().getTitle().equals(ChatColor.DARK_GRAY + "Gang Members")) {
                    openMembersGUI(player, gang);
                }
            }
        }

        // 3. COLOR SELECTOR LOGIC
        else if (title.equals(ChatColor.DARK_GRAY + "Gang Color Selector")) {
            e.setCancelled(true);
            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            String colorCode = getColorFromItem(e.getCurrentItem().getType());

            if (colorCode != null) {
                gang.setColor(colorCode);
                plugin.gangManager.saveGangs();
                player.sendMessage(ChatColor.GREEN + "Gang color updated!");
                player.closeInventory();

                refreshGangScoreboards(gang);

                // NEW: Success Sound
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }

        // 4. INBOX LOGIC
        else if (title.equals(ChatColor.DARK_BLUE + "Inbox")) {
            e.setCancelled(true);
            if (e.getCurrentItem().getType() == Material.BARRIER) {
                plugin.mailManager.clearInbox(e.getWhoClicked().getUniqueId());
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(ChatColor.RED + "Inbox cleared.");

                // NEW: Clear Sound
                SoundUtils.playSound((Player) e.getWhoClicked(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
            }
        }
    }

    // --- CHAT INPUT LOGIC ONLY ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (plugin.gangManager.chatInputMode.containsKey(id)) {
            e.setCancelled(true);
            String mode = plugin.gangManager.chatInputMode.remove(id);
            String input = e.getMessage();
            Gang gang = plugin.gangManager.getPlayerGang(id);

            if (gang != null) {
                if (mode.equals("NAME")) gang.setName(input);
                if (mode.equals("DESC")) gang.setDescription(input);

                plugin.gangManager.saveGangs();
                e.getPlayer().sendMessage(ChatColor.GREEN + "Setting updated!");
                refreshGangScoreboards(gang);

                // NEW: Success Sound
                SoundUtils.playSound(e.getPlayer(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    // --- HELPERS ---

    private void openMembersGUI(Player player, Gang gang) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Gang Members");
        int slot = 0;
        for (UUID memberId : gang.getMembers().keySet()) {
            // SKIP LEADER (They are shown in the main menu)
            if (memberId.equals(gang.getOwner())) continue;

            if (slot >= 54) break;

            Rank r = gang.getMembers().get(memberId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
            meta.setDisplayName(ChatColor.GOLD + Bukkit.getOfflinePlayer(memberId).getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Rank: " + r.name());
            lore.add(ChatColor.GRAY + "Left-Click: Promote");
            lore.add(ChatColor.GRAY + "Right-Click: Demote");
            lore.add(ChatColor.RED + "Shift-Click: Kick");
            meta.setLore(lore);

            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Go Back");
        back.setItemMeta(backMeta);
        inv.setItem(49, back); // Bottom Center

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void refreshGangScoreboards(Gang gang) {
        for (UUID memberId : gang.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                plugin.scoreboardManager.setScoreboard(p);
            }
        }
    }

    private void openColorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Gang Color Selector");

        inv.setItem(10, createItem(Material.RED_WOOL, ChatColor.RED + "Red"));
        inv.setItem(11, createItem(Material.ORANGE_WOOL, ChatColor.GOLD + "Gold"));
        inv.setItem(12, createItem(Material.YELLOW_WOOL, ChatColor.YELLOW + "Yellow"));
        inv.setItem(13, createItem(Material.LIME_WOOL, ChatColor.GREEN + "Lime"));
        inv.setItem(14, createItem(Material.LIGHT_BLUE_WOOL, ChatColor.AQUA + "Aqua"));
        inv.setItem(15, createItem(Material.BLUE_WOOL, ChatColor.BLUE + "Blue"));
        inv.setItem(16, createItem(Material.PINK_WOOL, ChatColor.LIGHT_PURPLE + "Pink"));
        inv.setItem(22, createItem(Material.WHITE_WOOL, ChatColor.WHITE + "White"));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private String getColorFromItem(Material mat) {
        switch (mat) {
            case RED_WOOL: return "&c";
            case ORANGE_WOOL: return "&6";
            case YELLOW_WOOL: return "&e";
            case LIME_WOOL: return "&a";
            case LIGHT_BLUE_WOOL: return "&b";
            case BLUE_WOOL: return "&9";
            case PINK_WOOL: return "&d";
            case WHITE_WOOL: return "&f";
            default: return null;
        }
    }

    private void handleMemberClick(InventoryClickEvent e, Gang gang, Player player, ItemStack item) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        UUID targetId = meta.getOwningPlayer().getUniqueId();

        if (targetId.equals(player.getUniqueId())) return;

        Rank current = gang.getMembers().get(targetId);

        if (e.getClick() == ClickType.LEFT) {
            if (current == Rank.THUG) setRank(gang, targetId, Rank.ELITE);
            else if (current == Rank.MEMBER) setRank(gang, targetId, Rank.THUG);
            else if (current == Rank.ELITE) setRank(gang, targetId, Rank.CO_LEADER);
            player.sendMessage(ChatColor.GREEN + "Promoted.");

            // NEW: Promote Sound
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
        }
        else if (e.getClick() == ClickType.RIGHT) {
            if (current == Rank.CO_LEADER) setRank(gang, targetId, Rank.ELITE);
            else if (current == Rank.ELITE) setRank(gang, targetId, Rank.THUG);
            else if (current == Rank.THUG) setRank(gang, targetId, Rank.MEMBER);
            player.sendMessage(ChatColor.YELLOW + "Demoted.");

            // NEW: Demote Sound
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        }
        else if (e.getClick() == ClickType.SHIFT_LEFT) {
            // --- UPDATED KICK LOGIC ---

            // 1. Prevent kicking the leader (just in case)
            if (current == Rank.LEADER) {
                player.sendMessage(ChatColor.RED + "You cannot kick the leader!");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            // 2. Use the new Manager method
            plugin.gangManager.kickMember(gang, targetId);

            player.sendMessage(ChatColor.RED + "Kicked member.");
            // NEW: Kick Sound
            SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

            player.closeInventory(); // Close menu to refresh state
        }
    }

    private void startChatInput(Player p, String mode) {
        p.closeInventory();
        plugin.gangManager.chatInputMode.put(p.getUniqueId(), mode);
        p.sendMessage(ChatColor.GREEN + "Type the new value in chat now...");
        SoundUtils.playSound(p, Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);
    }

    private void setRank(Gang g, UUID u, Rank r) {
        g.getMembers().put(u, r);
        plugin.gangManager.saveGangs();
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}