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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class StaffManagementListener implements Listener {

    private final PrisonVaults plugin;
    // Store which player is being managed by whom
    private final Map<UUID, UUID> editorTarget = new HashMap<>();
    private final Map<UUID, String> chatInputMode = new HashMap<>();

    public StaffManagementListener(PrisonVaults plugin) {
        this.plugin = plugin;
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

        // --- MAIN MENU ---
        if (title.equals(ChatColor.DARK_RED + "Staff Management")) {
            if (clicked.getType() == Material.GOLDEN_HELMET) {
                openPlayerList(player, true, 1); // Staff List
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                openPlayerList(player, false, 1); // Member List
            }
        }

        // --- PLAYER LISTS (Pagination) ---
        else if (title.startsWith("Manage")) {
            boolean isStaffList = title.contains("Staff");
            int page = 1;

            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null) {
                    editorTarget.put(player.getUniqueId(), target.getUniqueId());
                    openActionMenu(player, target);
                }
            }
        }

        // --- ACTION MENU ---
        else if (title.startsWith("Action: ")) {
            UUID targetUUID = editorTarget.get(player.getUniqueId());
            if (targetUUID == null) { player.closeInventory(); return; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            RankManager.Rank targetRank = plugin.rankManager.getRank(target);

            // 1. PROMOTION
            if (clicked.getType() == Material.EMERALD) {
                if (targetRank.ordinal() < RankManager.Rank.ADMIN.ordinal()) {
                    int nextOrd = targetRank.ordinal() - 1;
                    if (nextOrd >= 0) {
                        RankManager.Rank newRank = RankManager.Rank.values()[nextOrd];
                        if (newRank == RankManager.Rank.OWNER || newRank == RankManager.Rank.CO_OWNER) {
                            player.sendMessage(ChatColor.RED + "Cannot promote to Owner/Co-Owner via GUI.");
                            return;
                        }
                        plugin.rankManager.setRank(target, newRank);
                        player.sendMessage(ChatColor.GREEN + "Promoted " + target.getName() + " to " + newRank.display);
                        openActionMenu(player, target);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Cannot promote further.");
                }
            }

            // 2. DEMOTION
            else if (clicked.getType() == Material.REDSTONE) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "You cannot demote Owners.");
                    return;
                }
                int nextOrd = targetRank.ordinal() + 1;
                if (nextOrd < RankManager.Rank.values().length) {
                    RankManager.Rank newRank = RankManager.Rank.values()[nextOrd];
                    plugin.rankManager.setRank(target, newRank);
                    player.sendMessage(ChatColor.YELLOW + "Demoted " + target.getName() + " to " + newRank.display);
                    openActionMenu(player, target);
                }
            }

            // 3. KICK (Protected)
            else if (clicked.getType() == Material.IRON_BOOTS) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "You cannot kick Owners or Co-Owners.");
                    SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (target.isOnline()) {
                    ((Player)target).kickPlayer(ChatColor.RED + "Kicked by Staff.");
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName());
                } else {
                    player.sendMessage(ChatColor.RED + "Player is offline.");
                }
            }

            // 4. BAN (Protected)
            else if (clicked.getType() == Material.BARRIER) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "You cannot ban Owners or Co-Owners.");
                    SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), "Banned by Operator", null, player.getName());
                if (target.isOnline()) ((Player)target).kickPlayer(ChatColor.RED + "Banned!");
                player.sendMessage(ChatColor.RED + "Banned " + target.getName());
            }

            // 5. WARN (Protected)
            else if (clicked.getType() == Material.PAPER) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "You cannot warn Owners or Co-Owners.");
                    SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                player.closeInventory();
                chatInputMode.put(player.getUniqueId(), "WARN");
                player.sendMessage(ChatColor.YELLOW + "Type warning details in chat: <Type> <Severity> <Reason>");
                player.sendMessage(ChatColor.GRAY + "Example: Hacking High Using Xray");
            }
        }
    }

    // --- CHAT INPUT ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!chatInputMode.containsKey(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String msg = event.getMessage();

        UUID targetUUID = editorTarget.get(player.getUniqueId());
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        // Notify Staff
        player.sendMessage(ChatColor.GREEN + "Warned " + target.getName() + ": " + msg);

        // Notify Target if online
        if (target.isOnline()) {
            ((Player)target).sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.YELLOW + msg);
            SoundUtils.playSound((Player)target, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }

        // Send Staff Mail
        plugin.staffMailManager.sendStaffMail(target, "WARNING", msg);

        chatInputMode.remove(player.getUniqueId());
    }

    // --- HELPER: OPEN LIST ---
    private void openPlayerList(Player player, boolean staffOnly, int page) {
        String title = staffOnly ? "Manage Staff - Page " + page : "Manage Members - Page " + page;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<OfflinePlayer> allPlayers = Arrays.asList(Bukkit.getOfflinePlayers());
        List<OfflinePlayer> filtered = allPlayers.stream()
                .filter(p -> {
                    RankManager.Rank r = plugin.rankManager.getRank(p);
                    return staffOnly ? r.weight >= 3 : r.weight < 3;
                })
                .collect(Collectors.toList());

        int index = 0;
        for (OfflinePlayer p : filtered) {
            if (index >= 45) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.setDisplayName(ChatColor.YELLOW + p.getName());

            RankManager.Rank r = plugin.rankManager.getRank(p);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rank: " + r.color + r.display);
            lore.add(ChatColor.GRAY + "Status: " + (p.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));
            lore.add(ChatColor.YELLOW + "Click to Manage");
            meta.setLore(lore);
            head.setItemMeta(meta);

            inv.addItem(head);
            index++;
        }

        player.openInventory(inv);
    }

    // --- HELPER: OPEN ACTION MENU ---
    private void openActionMenu(Player player, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(null, 27, "Action: " + target.getName());

        RankManager.Rank r = plugin.rankManager.getRank(target);

        // Items
        inv.setItem(10, createItem(Material.EMERALD, ChatColor.GREEN + "Promote", "Current: " + r.display));
        inv.setItem(11, createItem(Material.REDSTONE, ChatColor.RED + "Demote", "Current: " + r.display));
        inv.setItem(13, createItem(Material.PAPER, ChatColor.YELLOW + "Warn", "Issue a formal warning"));
        inv.setItem(15, createItem(Material.IRON_BOOTS, ChatColor.GOLD + "Kick", "Kick from server"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.DARK_RED + "Ban", "Ban from server"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + lore));
        item.setItemMeta(meta);
        return item;
    }
}