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

        // --- NAVIGATION BUTTONS (Global) ---
        if (clicked.getType() == Material.BARRIER && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Close")) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
            // Logic to determine where "Back" goes
            if (title.startsWith("Manage")) {
                // Back from List -> Main Menu
                openStaffMainMenu(player); // FIXED: Opens menu directly
            } else if (title.startsWith("Action")) {
                // Back from Action -> List
                UUID targetUUID = editorTarget.get(player.getUniqueId());
                if (targetUUID != null) {
                    OfflinePlayer t = Bukkit.getOfflinePlayer(targetUUID);
                    RankManager.Rank r = plugin.rankManager.getRank(t);
                    // If target is staff, go to staff list, else member list
                    openPlayerList(player, r.weight >= 3, 1);
                } else {
                    openStaffMainMenu(player); // Fallback to main menu
                }
            }
            return;
        }

        // --- MAIN MENU ---
        if (title.equals(ChatColor.DARK_RED + "Staff Management")) {
            if (clicked.getType() == Material.GOLDEN_HELMET) {
                openPlayerList(player, true, 1); // Staff List
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                openPlayerList(player, false, 1); // Member List
            }
        }

        // --- PLAYER LISTS ---
        else if (title.startsWith("Manage")) {
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
                        openActionMenu(player, target); // Re-open to refresh
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
                    openActionMenu(player, target); // Re-open to refresh
                }
            }

            // 3. KICK
            else if (clicked.getType() == Material.IRON_BOOTS) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "Protected rank.");
                    return;
                }
                if (target.isOnline()) {
                    ((Player)target).kickPlayer(ChatColor.RED + "Kicked by Staff.");
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName());
                } else {
                    player.sendMessage(ChatColor.RED + "Player is offline.");
                }
                // Keep GUI open
            }

            // 4. BAN
            else if (clicked.getType() == Material.BARRIER && !clicked.getItemMeta().getDisplayName().contains("Close")) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "Protected rank.");
                    return;
                }
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), "Banned by Operator", null, player.getName());
                if (target.isOnline()) ((Player)target).kickPlayer(ChatColor.RED + "Banned!");
                player.sendMessage(ChatColor.RED + "Banned " + target.getName());
                // Keep GUI open
            }

            // 5. WARN (Must Close for Chat)
            else if (clicked.getType() == Material.PAPER) {
                if (targetRank == RankManager.Rank.OWNER || targetRank == RankManager.Rank.CO_OWNER) {
                    player.sendMessage(ChatColor.RED + "Protected rank.");
                    return;
                }
                player.closeInventory(); // CLOSE for input
                chatInputMode.put(player.getUniqueId(), "WARN");
                player.sendMessage(ChatColor.YELLOW + "Type warning details in chat...");
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

        player.sendMessage(ChatColor.GREEN + "Warned " + target.getName() + ": " + msg);

        if (target.isOnline()) {
            ((Player)target).sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.YELLOW + msg);
            SoundUtils.playSound((Player)target, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }

        plugin.staffMailManager.sendStaffMail(target, "WARNING", msg);
        chatInputMode.remove(player.getUniqueId());

        // Optionally re-open GUI after chat? Usually better to leave them in chat to see confirmation.
    }

    // --- NEW: OPEN MAIN MENU METHOD ---
    public void openStaffMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Staff Management");

        inv.setItem(11, createItem(Material.GOLDEN_HELMET, ChatColor.GOLD + "Manage Staff", "View and edit Staff ranks"));
        inv.setItem(15, createItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Manage Members", "View and edit Member ranks"));

        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close", "Close Menu"));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // --- UPDATED: PLAYER LIST WITH BACK/CLOSE ---
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
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Rank: " + r.color + r.display, ChatColor.YELLOW + "Click to Manage"));
            head.setItemMeta(meta);
            inv.addItem(head);
            index++;
        }

        // Navigation
        inv.setItem(45, createItem(Material.ARROW, ChatColor.RED + "Back", "Return to Main Menu"));
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close", "Close Menu"));

        player.openInventory(inv);
    }

    // --- UPDATED: ACTION MENU WITH BACK/CLOSE ---
    private void openActionMenu(Player player, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(null, 27, "Action: " + target.getName());
        RankManager.Rank r = plugin.rankManager.getRank(target);

        inv.setItem(10, createItem(Material.EMERALD, ChatColor.GREEN + "Promote", "Current: " + r.display));
        inv.setItem(11, createItem(Material.REDSTONE, ChatColor.RED + "Demote", "Current: " + r.display));
        inv.setItem(13, createItem(Material.PAPER, ChatColor.YELLOW + "Warn", "Issue a formal warning"));
        inv.setItem(15, createItem(Material.IRON_BOOTS, ChatColor.GOLD + "Kick", "Kick from server"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.DARK_RED + "Ban", "Ban from server"));

        // Navigation
        inv.setItem(18, createItem(Material.ARROW, ChatColor.RED + "Back", "Return to List"));
        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close", "Close Menu"));

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