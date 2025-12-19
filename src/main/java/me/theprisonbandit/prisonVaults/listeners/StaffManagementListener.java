package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.managers.RankManager;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class StaffManagementListener implements Listener {

    private final PrisonVaults plugin;
    private final Map<UUID, UUID> editorTarget = new HashMap<>();
    private final Map<UUID, String> chatInputMode = new HashMap<>();
    private final NamespacedKey uuidKey;

    // Track which page a player is currently viewing
    private final Map<UUID, Integer> pageViewer = new HashMap<>();
    private final Map<UUID, Boolean> typeViewer = new HashMap<>();

    public StaffManagementListener(PrisonVaults plugin) {
        this.plugin = plugin;
        this.uuidKey = new NamespacedKey(plugin, "staff_uuid");
    }

    public void openStaffMenu(Player player) {
        openPlayerList(player, true, 1);
    }

    public void openMemberMenu(Player player) {
        openPlayerList(player, false, 1);
    }

    public void openSpecificEditor(Player admin, OfflinePlayer target) {
        editorTarget.put(admin.getUniqueId(), target.getUniqueId());
        openActionMenu(admin, target);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String rawTitle = event.getView().getTitle();
        String title = ChatColor.stripColor(rawTitle);

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

        // --- BACK BUTTON ---
        if (clicked.getType() == Material.ARROW && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
            if (title.startsWith("Action")) {
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

        // --- PLAYER LIST CLICK ---
        if (title.startsWith("Manage")) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return;

                if (meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
                    String uuidStr = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                    try {
                        UUID targetId = UUID.fromString(uuidStr);
                        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
                        openSpecificEditor(player, target);
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Error reading player data.");
                    }
                } else if (meta instanceof SkullMeta) {
                    OfflinePlayer target = ((SkullMeta) meta).getOwningPlayer();
                    if (target != null) {
                        openSpecificEditor(player, target);
                    }
                }
            }
        }

        // --- ACTION MENU ---
        else if (title.startsWith("Action")) {
            UUID targetUUID = editorTarget.get(player.getUniqueId());
            if (targetUUID == null) { player.closeInventory(); return; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            RankManager.Rank targetRank = plugin.rankManager.getRank(target);

            // Safety Check: Is target protected?
            boolean isProtected = (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER);

            if (clicked.getType() == Material.EMERALD) {
                if (targetRank == RankManager.Rank.MEMBER) {
                    player.sendMessage(ChatColor.RED + "Members cannot be promoted to Staff via this menu.");
                    return;
                }
                promotePlayer(player, target, targetRank);
            }
            else if (clicked.getType() == Material.REDSTONE) {
                if (targetRank == RankManager.Rank.MEMBER) {
                    player.sendMessage(ChatColor.RED + "Player is already a Member.");
                    return;
                }
                demotePlayer(player, target, targetRank);
            }
            // --- PUNISHMENTS ---
            else if (clicked.getType() == Material.IRON_BOOTS) { // KICK
                if (isProtected) {
                    player.sendMessage(ChatColor.RED + "You cannot kick the Owner or Co-Owner.");
                    return;
                }
                if (plugin.rankManager.isStaff(target)) stripStaffStatus(target);
                if (target.isOnline()) {
                    ((Player)target).kickPlayer(ChatColor.RED + "You were kicked by Staff.");
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName());
                } else {
                    player.sendMessage(ChatColor.GREEN + "Offline player processed.");
                }
                openActionMenu(player, target);
            }
            else if (clicked.getType() == Material.BARRIER && clicked.getItemMeta().getDisplayName().contains("Ban Player")) { // BAN
                if (isProtected) {
                    player.sendMessage(ChatColor.RED + "You cannot ban the Owner or Co-Owner.");
                    return;
                }
                if (plugin.rankManager.isStaff(target)) stripStaffStatus(target);
                Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "Banned by Staff", null, player.getName());
                if (target.isOnline()) ((Player)target).kickPlayer(ChatColor.RED + "Banned!");
                player.sendMessage(ChatColor.RED + "Banned " + target.getName());
                openActionMenu(player, target);
            }
            else if (clicked.getType() == Material.BEDROCK) { // BAN-IP
                if (isProtected) {
                    player.sendMessage(ChatColor.RED + "You cannot IP-Ban the Owner or Co-Owner.");
                    return;
                }
                if (plugin.rankManager.isStaff(target)) stripStaffStatus(target);
                String ip = null;
                if (target.isOnline()) ip = target.getPlayer().getAddress().getAddress().getHostAddress();
                else player.sendMessage(ChatColor.RED + "Cannot Ban-IP offline player (IP not known).");

                if (ip != null) {
                    Bukkit.getBanList(BanList.Type.IP).addBan(ip, "IP Banned by Staff", null, player.getName());
                    Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "IP Banned", null, player.getName());
                    if (target.isOnline()) ((Player)target).kickPlayer(ChatColor.RED + "IP Banned!");
                    player.sendMessage(ChatColor.RED + "IP Banned " + target.getName());
                }
                openActionMenu(player, target);
            }
            else if (clicked.getType() == Material.PAPER) { // WARN
                if (isProtected) {
                    player.sendMessage(ChatColor.RED + "You cannot warn the Owner or Co-Owner.");
                    return;
                }
                player.closeInventory();
                chatInputMode.put(player.getUniqueId(), "WARN");
                player.sendMessage(ChatColor.YELLOW + "Type warning in chat...");
            }
        }
    }

    // --- HELPER METHODS ---

    private void promotePlayer(Player admin, OfflinePlayer target, RankManager.Rank currentRank) {
        RankManager.Rank[] ranks = RankManager.Rank.values();
        int currentIdx = -1;
        for(int i=0; i<ranks.length; i++) if(ranks[i] == currentRank) currentIdx = i;

        if (currentIdx > 0) {
            RankManager.Rank newRank = ranks[currentIdx - 1];
            if (newRank == RankManager.Rank.OWNER || newRank == RankManager.Rank.CO_OWNER) {
                admin.sendMessage(ChatColor.RED + "Cannot promote to Owner/Co-Owner via GUI.");
                return;
            }
            plugin.rankManager.setRank(target, newRank);
            syncPermissionGroup(target, newRank);
            admin.sendMessage(ChatColor.GREEN + "Promoted " + target.getName() + " to " + newRank.display);
            if(target.isOnline()) plugin.scoreboardManager.setScoreboard((Player)target);
            openActionMenu(admin, target);
        } else {
            admin.sendMessage(ChatColor.RED + "Cannot promote further.");
        }
    }

    private void demotePlayer(Player admin, OfflinePlayer target, RankManager.Rank currentRank) {
        if (currentRank == RankManager.Rank.OWNER || currentRank == RankManager.Rank.CO_OWNER) {
            admin.sendMessage(ChatColor.RED + "You cannot demote Owners.");
            return;
        }
        RankManager.Rank[] ranks = RankManager.Rank.values();
        int currentIdx = -1;
        for(int i=0; i<ranks.length; i++) if(ranks[i] == currentRank) currentIdx = i;

        if (currentIdx < ranks.length - 1) {
            RankManager.Rank newRank = ranks[currentIdx + 1];
            plugin.rankManager.setRank(target, newRank);
            syncPermissionGroup(target, newRank);
            admin.sendMessage(ChatColor.YELLOW + "Demoted " + target.getName() + " to " + newRank.display);
            if(target.isOnline()) plugin.scoreboardManager.setScoreboard((Player)target);
            openActionMenu(admin, target);
        }
    }

    private void syncPermissionGroup(OfflinePlayer target, RankManager.Rank rank) {
        String groupName = "default";
        switch (rank) {
            case OWNER: groupName = "owner"; break;
            case CO_OWNER: groupName = "coowner"; break;
            case ADMIN: groupName = "admin"; break;
            case MODERATOR: groupName = "moderator"; break;
            case HELPER: groupName = "helper"; break;
            case MEMBER: groupName = "default"; break;
        }
        plugin.permissionManager.setGroup(target.getUniqueId(), groupName);
    }

    private void stripStaffStatus(OfflinePlayer target) {
        plugin.rankManager.setRank(target, RankManager.Rank.MEMBER);
        plugin.permissionManager.setGroup(target.getUniqueId(), "default");
        if (target.isOnline()) plugin.scoreboardManager.setScoreboard(target.getPlayer());
    }

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

    private void openPlayerList(Player player, boolean staffOnly, int page) {
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

                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, p.getUniqueId().toString());

                head.setItemMeta(meta);
                inv.addItem(head);
            }
        }

        if (page > 1) inv.setItem(48, createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        if (end < filtered.size()) inv.setItem(50, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close", null));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void openActionMenu(Player player, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(null, 27, "Action: " + target.getName());
        RankManager.Rank r = plugin.rankManager.getRank(target);

        boolean isProtected = (r == RankManager.Rank.OWNER || r == RankManager.Rank.CO_OWNER);

        // --- PROMOTE / DEMOTE ---
        if (r.weight >= 3) {
            inv.setItem(10, createItem(Material.EMERALD, ChatColor.GREEN + "Promote", "Current: " + r.display));
            inv.setItem(11, createItem(Material.REDSTONE, ChatColor.RED + "Demote", "Current: " + r.display));
        } else {
            inv.setItem(10, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Promote Disabled", "Cannot promote Members"));
            inv.setItem(11, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Demote Disabled", "Cannot demote Members"));
        }

        // --- PUNISHMENT BUTTONS ---
        if (isProtected) {
            // GREY OUT punishment buttons for Owners
            inv.setItem(13, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Warn Disabled", "Cannot warn Owner/Co-Owner"));
            inv.setItem(14, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Kick Disabled", "Cannot kick Owner/Co-Owner"));
            inv.setItem(15, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Ban-IP Disabled", "Cannot IP-Ban Owner/Co-Owner"));
            inv.setItem(16, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Ban Disabled", "Cannot Ban Owner/Co-Owner"));
        } else {
            // Normal buttons
            inv.setItem(13, createItem(Material.PAPER, ChatColor.YELLOW + "Warn", "Send warning"));
            inv.setItem(14, createItem(Material.IRON_BOOTS, ChatColor.GOLD + "Kick", "Kick Player"));
            inv.setItem(15, createItem(Material.BEDROCK, ChatColor.DARK_RED + "Ban-IP", "Ban IP Address"));
            inv.setItem(16, createItem(Material.BARRIER, ChatColor.RED + "Ban Player", "Ban Name"));
        }

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