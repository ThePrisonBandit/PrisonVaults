package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.managers.RankManager;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class StaffManagementListener implements Listener {

    private final PrisonVaults plugin;
    private final Map<UUID, UUID> editorTarget = new HashMap<>();
    private final Map<UUID, String> chatInputMode = new HashMap<>();

    // Track which page a player is currently viewing
    private final Map<UUID, Integer> pageViewer = new HashMap<>();
    private final Map<UUID, Boolean> typeViewer = new HashMap<>();

    public StaffManagementListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- NEW PUBLIC METHOD FOR COMMAND ACCESS ---
    public void openSpecificEditor(Player admin, OfflinePlayer target) {
        // 1. Set the target in the map so button clicks know who we are editing
        editorTarget.put(admin.getUniqueId(), target.getUniqueId());

        // 2. Open the Action Menu directly
        openActionMenu(admin, target);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Staff") && !title.contains("Members") && !title.contains("Action")) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // --- GLOBAL CLOSE ---
        if (clicked.getType() == Material.BARRIER && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Close")) {
            player.closeInventory();
            return;
        }

        // --- BACK BUTTON LOGIC ---
        if (clicked.getType() == Material.ARROW && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
            if (title.startsWith("Manage")) {
                openStaffMainMenu(player);
            } else if (title.startsWith("Action")) {
                // If they came from a specific command, maybe default to Main Menu?
                // Or try to return to list. Safe bet is Main Menu or List.
                boolean wasStaff = typeViewer.getOrDefault(player.getUniqueId(), true);
                int lastPage = pageViewer.getOrDefault(player.getUniqueId(), 1);
                openPlayerList(player, wasStaff, lastPage);
            }
            return;
        }

        // --- PAGINATION ---
        if (clicked.getType() == Material.ARROW) {
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (name.contains("Next")) {
                boolean isStaff = typeViewer.getOrDefault(player.getUniqueId(), true);
                int currentPage = pageViewer.getOrDefault(player.getUniqueId(), 1);
                openPlayerList(player, isStaff, currentPage + 1);
                return;
            }
            if (name.contains("Previous")) {
                boolean isStaff = typeViewer.getOrDefault(player.getUniqueId(), true);
                int currentPage = pageViewer.getOrDefault(player.getUniqueId(), 1);
                if (currentPage > 1) openPlayerList(player, isStaff, currentPage - 1);
                return;
            }
        }

        // --- MAIN MENU ---
        if (title.equals(ChatColor.DARK_RED + "Staff Management")) {
            if (clicked.getType() == Material.GOLDEN_HELMET) {
                openPlayerList(player, true, 1);
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                openPlayerList(player, false, 1);
            }
        }

        // --- PLAYER LIST CLICK ---
        else if (title.startsWith("Manage")) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null) {
                    openSpecificEditor(player, target);
                }
            }
        }

        // --- ACTION MENU (THE LOGIC) ---
        else if (title.startsWith("Action: ")) {
            UUID targetUUID = editorTarget.get(player.getUniqueId());
            if (targetUUID == null) { player.closeInventory(); return; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            RankManager.Rank targetRank = plugin.rankManager.getRank(target);

            // 1. PROMOTION
            if (clicked.getType() == Material.EMERALD) {
                if (targetRank.ordinal() > 0) { // Assuming lower ordinal = higher rank (Owner=0 or similar, check enum order)
                    // Based on your Rank enum: OWNER(10), CO_OWNER(9)... MEMBER(1).
                    // The Enum values() order is usually definition order.
                    // Let's rely on your previous 'weight' logic or assume standard definition order:
                    // OWNER, CO_OWNER, ADMIN, MOD, HELPER, MEMBER.

                    int currentIdx = -1;
                    RankManager.Rank[] ranks = RankManager.Rank.values();
                    for(int i=0; i<ranks.length; i++) if(ranks[i] == targetRank) currentIdx = i;

                    if (currentIdx > 0) { // Can go up
                        RankManager.Rank newRank = ranks[currentIdx - 1];
                        if (newRank == RankManager.Rank.OWNER || newRank == RankManager.Rank.CO_OWNER) {
                            player.sendMessage(ChatColor.RED + "Cannot promote to Owner/Co-Owner via GUI.");
                            return;
                        }
                        plugin.rankManager.setRank(target, newRank);
                        player.sendMessage(ChatColor.GREEN + "Promoted " + target.getName() + " to " + newRank.display);
                        if(target.isOnline()) plugin.scoreboardManager.setScoreboard((Player)target);
                        openActionMenu(player, target);
                    } else {
                        player.sendMessage(ChatColor.RED + "Cannot promote further.");
                    }
                }
            }

            // 2. DEMOTION
            else if (clicked.getType() == Material.REDSTONE) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "You cannot demote Owners.");
                    return;
                }

                int currentIdx = -1;
                RankManager.Rank[] ranks = RankManager.Rank.values();
                for(int i=0; i<ranks.length; i++) if(ranks[i] == targetRank) currentIdx = i;

                if (currentIdx < ranks.length - 1) { // Can go down
                    RankManager.Rank newRank = ranks[currentIdx + 1];
                    plugin.rankManager.setRank(target, newRank);
                    player.sendMessage(ChatColor.YELLOW + "Demoted " + target.getName() + " to " + newRank.display);
                    if(target.isOnline()) plugin.scoreboardManager.setScoreboard((Player)target);
                    openActionMenu(player, target);
                }
            }

            // 3. KICK (Offline Support)
            else if (clicked.getType() == Material.IRON_BOOTS) {
                if (plugin.rankManager.isStaff(target)) {
                    stripStaffStatus(target);
                }

                if (target.isOnline()) {
                    ((Player)target).kickPlayer(ChatColor.RED + "You were kicked by Staff.");
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName());
                } else {
                    player.sendMessage(ChatColor.GREEN + "Offline player processed (Rank removed if staff).");
                }
                openActionMenu(player, target);
            }

            // 4. BAN (Offline Support)
            else if (clicked.getType() == Material.BARRIER && !clicked.getItemMeta().getDisplayName().contains("Close")) {
                if (plugin.rankManager.isStaff(target)) {
                    stripStaffStatus(target);
                }

                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), "Banned by Staff", null, player.getName());
                if (target.isOnline()) ((Player)target).kickPlayer(ChatColor.RED + "Banned!");

                player.sendMessage(ChatColor.RED + "Banned " + target.getName());
                openActionMenu(player, target);
            }

            // 5. WARN
            else if (clicked.getType() == Material.PAPER) {
                player.closeInventory();
                chatInputMode.put(player.getUniqueId(), "WARN");
                player.sendMessage(ChatColor.YELLOW + "Type warning in chat...");
            }
        }
    }

    private void stripStaffStatus(OfflinePlayer target) {
        plugin.rankManager.setRank(target, RankManager.Rank.MEMBER);
        plugin.permissionManager.setGroup(target.getUniqueId(), "default");
        if (target.isOnline()) plugin.scoreboardManager.setScoreboard(target.getPlayer());
    }

    // --- CHAT HANDLING ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!chatInputMode.containsKey(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID targetUUID = editorTarget.get(player.getUniqueId());

        if (targetUUID == null) {
            chatInputMode.remove(player.getUniqueId());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String msg = event.getMessage();

        player.sendMessage(ChatColor.GREEN + "Warned " + target.getName());
        plugin.staffMailManager.sendStaffMail(target, "WARNING", msg);
        if (target.isOnline()) {
            ((Player)target).sendMessage(ChatColor.RED + "WARNING: " + msg);
            SoundUtils.playSound((Player)target, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }

        chatInputMode.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> openActionMenu(player, target));
    }

    // --- GUI OPENERS ---
    public void openStaffMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Staff Management");
        inv.setItem(11, createItem(Material.GOLDEN_HELMET, ChatColor.GOLD + "Manage Staff", "View Staff List"));
        inv.setItem(15, createItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Manage Members", "View Member List"));
        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close", null));
        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public void openPlayerList(Player player, boolean staffOnly, int page) {
        pageViewer.put(player.getUniqueId(), page);
        typeViewer.put(player.getUniqueId(), staffOnly);

        String title = staffOnly ? "Manage Staff - Page " + page : "Manage Members - Page " + page;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<OfflinePlayer> allPlayers = Arrays.asList(Bukkit.getOfflinePlayers());
        List<OfflinePlayer> filtered = allPlayers.stream()
                .filter(p -> {
                    RankManager.Rank r = plugin.rankManager.getRank(p);
                    return staffOnly ? r.weight >= 3 : r.weight < 3;
                })
                .collect(Collectors.toList());

        // Pagination
        int itemsPerPage = 45;
        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filtered.size());

        if (start < filtered.size()) {
            for (int i = start; i < end; i++) {
                OfflinePlayer p = filtered.get(i);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(p);
                meta.setDisplayName(ChatColor.YELLOW + (p.getName() != null ? p.getName() : "Unknown"));
                RankManager.Rank r = plugin.rankManager.getRank(p);
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Rank: " + r.color + r.display));
                head.setItemMeta(meta);
                inv.addItem(head);
            }
        }

        if (page > 1) inv.setItem(48, createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        if (end < filtered.size()) inv.setItem(50, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        inv.setItem(45, createItem(Material.ARROW, ChatColor.RED + "Back", null));
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close", null));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void openActionMenu(Player player, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(null, 27, "Action: " + target.getName());
        RankManager.Rank r = plugin.rankManager.getRank(target);

        inv.setItem(10, createItem(Material.EMERALD, ChatColor.GREEN + "Promote", "Current: " + r.display));
        inv.setItem(11, createItem(Material.REDSTONE, ChatColor.RED + "Demote", "Current: " + r.display));
        inv.setItem(13, createItem(Material.PAPER, ChatColor.YELLOW + "Warn", "Send warning"));
        inv.setItem(15, createItem(Material.IRON_BOOTS, ChatColor.GOLD + "Kick/Remove", "Kick & Strip Staff"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.DARK_RED + "Ban", "Ban & Strip Staff"));

        inv.setItem(18, createItem(Material.ARROW, ChatColor.RED + "Back", "Return to List"));
        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close", null));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(Collections.singletonList(ChatColor.GRAY + lore));
        item.setItemMeta(meta);
        return item;
    }
}