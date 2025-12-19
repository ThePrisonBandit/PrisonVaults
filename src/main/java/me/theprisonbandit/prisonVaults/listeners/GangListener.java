package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Rank;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GangListener implements Listener {
    private final PrisonVaults plugin;

    public GangListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (e.getCurrentItem() == null) return;
        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // --- GLOBAL NAVIGATION BUTTONS ---
        if (clicked.getType() == Material.BARRIER && clicked.hasItemMeta() && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Close")) {
            player.closeInventory();
            return;
        }

        // --- MAIN GANG MANAGER ---
        if (title.equals(ChatColor.DARK_GRAY + "Gang Manager")) {
            e.setCancelled(true);
            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            if (clicked.getType() == Material.NAME_TAG) {
                startChatInput(player, "NAME");
            } else if (clicked.getType() == Material.OAK_SIGN) {
                startChatInput(player, "TAG");
            } else if (clicked.getType() == Material.PAPER) {
                startChatInput(player, "DESC");
            } else if (clicked.getType() == Material.RED_DYE) {
                openColorGUI(player);
            } else if (clicked.getType() == Material.IRON_BARS) {
                plugin.gangManager.openBanManagerGUI(player, gang);
            } else if (clicked.getType() == Material.TNT) {
                if (!gang.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the Gang Owner can disband the gang!");
                    SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }
                player.closeInventory();
                plugin.gangManager.chatInputMode.put(player.getUniqueId(), "CONFIRM_DISBAND");
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.RED + "You are about to disband your gang!");
                player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "confirm" + ChatColor.YELLOW + " to proceed or " + ChatColor.RED + "no" + ChatColor.YELLOW + " to cancel.");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                openMembersGUI(player, gang);
            }
        }

        // --- GANG BANS ---
        else if (title.equals(ChatColor.DARK_GRAY + "Gang Bans")) {
            e.setCancelled(true);
            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
                plugin.gangManager.openMainGangMenu(player);
                return;
            }

            if (clicked.getType() == Material.ANVIL) {
                startChatInput(player, "BAN_PLAYER");
                return;
            }

            if (clicked.getType() == Material.PLAYER_HEAD) {
                // Ensure Left Click Only
                if (e.getClick().isLeftClick()) {
                    ItemMeta meta = clicked.getItemMeta();
                    UUID targetId = null;

                    // 1. Try to get UUID from Data Container (Reliable)
                    NamespacedKey key = plugin.gangManager.getBannedKey();
                    if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String uuidStr = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        try {
                            targetId = UUID.fromString(uuidStr);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    // 2. Fallback to Skull Owner (Less reliable for offline players)
                    else if (meta instanceof SkullMeta) {
                        OfflinePlayer offP = ((SkullMeta) meta).getOwningPlayer();
                        if (offP != null) targetId = offP.getUniqueId();
                    }

                    if (targetId != null) {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

                        // 1. Unban logic (Remove Data)
                        gang.removeBan(targetId);
                        plugin.gangManager.saveGangs();

                        // 2. Feedback to the person clicking
                        player.sendMessage(ChatColor.GREEN + "Unbanned " + (target.getName() != null ? target.getName() : "Unknown Player") + ".");

                        // 3. Notify the Owner (If online)
                        Player owner = Bukkit.getPlayer(gang.getOwner());
                        if (owner != null && owner.isOnline()) {
                            String gName = ChatColor.translateAlternateColorCodes('&', gang.getColor() + gang.getName());
                            String targetName = (target.getName() != null) ? target.getName() : "Unknown Player";

                            // Send message regardless of who unbanned (as requested)
                            owner.sendMessage(ChatColor.GREEN + targetName + " has been unbanned from " + gName + "!");
                        }

                        // 4. Refresh GUI (Removes the head)
                        plugin.gangManager.openBanManagerGUI(player, gang);
                        SoundUtils.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 2.0f);
                    }
                }
            }
        }

        // --- MEMBER MANAGEMENT ---
        else if (title.equals(ChatColor.DARK_GRAY + "Gang Members")) {
            e.setCancelled(true);
            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
                plugin.gangManager.openMainGangMenu(player);
                return;
            }

            if (clicked.getType() == Material.PLAYER_HEAD) {
                handleMemberClick(e, gang, player, clicked);
                openMembersGUI(player, gang);
            }
        }

        // --- COLOR SELECTOR ---
        else if (title.equals(ChatColor.DARK_GRAY + "Gang Color Selector")) {
            e.setCancelled(true);
            Gang gang = plugin.gangManager.getPlayerGang(player.getUniqueId());
            if (gang == null) return;

            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Back")) {
                plugin.gangManager.openMainGangMenu(player);
                return;
            }

            String colorCode = getColorFromItem(clicked.getType());
            if (colorCode != null) {
                gang.setColor(colorCode);
                plugin.gangManager.saveGangs();
                player.sendMessage(ChatColor.GREEN + "Gang color updated!");
                refreshGangScoreboards(gang);
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }

        // --- INBOX ---
        else if (title.equals(ChatColor.DARK_BLUE + "Inbox")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER && clicked.hasItemMeta() && ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).contains("Clear")) {
                plugin.mailManager.clearInbox(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Inbox cleared.");
                SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
            }
        }
    }

    // --- CHAT INPUT LOGIC ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (plugin.gangManager.chatInputMode.containsKey(id)) {
            e.setCancelled(true);
            String mode = plugin.gangManager.chatInputMode.remove(id);
            String input = e.getMessage();
            Gang gang = plugin.gangManager.getPlayerGang(id);

            if (gang != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {

                    // --- BAN PLAYER (Offline Compatible) ---
                    if (mode.equals("BAN_PLAYER")) {
                        String targetName = input;
                        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

                        if (target != null && (target.hasPlayedBefore() || target.isOnline())) {
                            if (gang.getOwner().equals(target.getUniqueId())) {
                                e.getPlayer().sendMessage(ChatColor.RED + "You cannot ban yourself!");
                            } else {
                                gang.addBan(target.getUniqueId());

                                if (gang.getMembers().containsKey(target.getUniqueId())) {
                                    plugin.gangManager.kickMember(gang, target.getUniqueId());
                                    e.getPlayer().sendMessage(ChatColor.YELLOW + "Player was in the gang and has been kicked.");
                                }

                                plugin.gangManager.saveGangs();
                                e.getPlayer().sendMessage(ChatColor.RED + "Banned " + target.getName() + " from the gang.");
                            }
                        } else {
                            e.getPlayer().sendMessage(ChatColor.RED + "Player not found or never played.");
                        }
                        plugin.gangManager.openBanManagerGUI(e.getPlayer(), gang);
                        return;
                    }

                    // --- DISBAND CONFIRMATION ---
                    if (mode.equals("CONFIRM_DISBAND")) {
                        if (input.equalsIgnoreCase("confirm")) {
                            String gName = gang.getName();
                            String gColor = gang.getColor();
                            for (UUID memberId : gang.getMembers().keySet()) {
                                Player member = Bukkit.getPlayer(memberId);
                                if (member != null && member.isOnline()) {
                                    if (!member.getUniqueId().equals(id)) {
                                        member.sendMessage(ChatColor.RED + "Your gang " +
                                                ChatColor.translateAlternateColorCodes('&', gColor + gName) +
                                                ChatColor.RED + " has been disbanded!");
                                        member.sendMessage(ChatColor.RED + "You are no longer in a gang.");
                                    }
                                }
                            }
                            plugin.gangManager.disbandGang(gang);
                            e.getPlayer().sendMessage(ChatColor.GREEN + "Gang Disbanded Successfully.");
                            SoundUtils.playSound(e.getPlayer(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        } else {
                            e.getPlayer().sendMessage(ChatColor.RED + "Disband Cancelled.");
                            plugin.gangManager.openMainGangMenu(e.getPlayer());
                        }
                        return;
                    }

                    // --- OTHER INPUTS ---
                    if (mode.equals("NAME")) {
                        gang.setName(input);
                        e.getPlayer().sendMessage(ChatColor.GREEN + "Gang Name updated!");
                    } else if (mode.equals("TAG")) {
                        if (input.length() > 5) {
                            e.getPlayer().sendMessage(ChatColor.RED + "Tag too long! Max 5 characters.");
                        } else {
                            gang.setTag(input);
                            e.getPlayer().sendMessage(ChatColor.GREEN + "Gang Tag updated!");
                        }
                    } else if (mode.equals("DESC")) {
                        gang.setDescription(input);
                        e.getPlayer().sendMessage(ChatColor.GREEN + "Description updated!");
                    }

                    plugin.gangManager.saveGangs();
                    refreshGangScoreboards(gang);
                    SoundUtils.playSound(e.getPlayer(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                    plugin.gangManager.openMainGangMenu(e.getPlayer());
                });
            }
        }
    }

    // --- HELPER METHODS ---

    private void startChatInput(Player p, String mode) {
        p.closeInventory();
        plugin.gangManager.chatInputMode.put(p.getUniqueId(), mode);
        p.sendMessage(ChatColor.GREEN + "Type the new " + mode.toLowerCase() + " in chat now...");
        SoundUtils.playSound(p, Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);
    }

    // REPLACED BY GangManager.openBanManagerGUI, but leaving this local one to prevent breaking old calls
    // though the code above now calls plugin.gangManager.openBanManagerGUI
    private void openBanManagerGUI(Player player, Gang gang) {
        plugin.gangManager.openBanManagerGUI(player, gang);
    }

    private void openMembersGUI(Player player, Gang gang) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Gang Members");
        int slot = 0;
        for (UUID memberId : gang.getMembers().keySet()) {
            if (memberId.equals(gang.getOwner())) continue;
            if (slot >= 45) break;

            Rank r = gang.getMembers().get(memberId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
            meta.setDisplayName(ChatColor.GOLD + Bukkit.getOfflinePlayer(memberId).getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Rank: " + r.display);
            lore.add(ChatColor.GRAY + "Left-Click: Promote");
            lore.add(ChatColor.GRAY + "Right-Click: Demote");
            lore.add(ChatColor.RED + "Shift-Click: Kick");
            meta.setLore(lore);

            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        back.setItemMeta(backMeta);
        inv.setItem(45, back);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(49, close);

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

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        back.setItemMeta(backMeta);
        inv.setItem(18, back);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(26, close);

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

        // --- PROMOTE LOGIC ---
        if (e.getClick() == ClickType.LEFT) {
            if (current == Rank.MEMBER) setRank(gang, targetId, Rank.HUSTLER);
            else if (current == Rank.HUSTLER) setRank(gang, targetId, Rank.BRUTE);
            else if (current == Rank.BRUTE) setRank(gang, targetId, Rank.THUG);
            else if (current == Rank.THUG) setRank(gang, targetId, Rank.SHOT_CALLER);
            else if (current == Rank.SHOT_CALLER) setRank(gang, targetId, Rank.ELITE);
            else if (current == Rank.ELITE) setRank(gang, targetId, Rank.CO_LEADER);
            else player.sendMessage(ChatColor.RED + "Cannot promote further!");

            player.sendMessage(ChatColor.GREEN + "Promoted to " + gang.getMembers().get(targetId).display);
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);

            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.GREEN + "You have been promoted to " + gang.getMembers().get(targetId).display + "!");
            }

        }
        // --- DEMOTE LOGIC ---
        else if (e.getClick() == ClickType.RIGHT) {
            if (current == Rank.CO_LEADER) setRank(gang, targetId, Rank.ELITE);
            else if (current == Rank.ELITE) setRank(gang, targetId, Rank.SHOT_CALLER);
            else if (current == Rank.SHOT_CALLER) setRank(gang, targetId, Rank.THUG);
            else if (current == Rank.THUG) setRank(gang, targetId, Rank.BRUTE);
            else if (current == Rank.BRUTE) setRank(gang, targetId, Rank.HUSTLER);
            else if (current == Rank.HUSTLER) setRank(gang, targetId, Rank.MEMBER);
            else player.sendMessage(ChatColor.RED + "Cannot demote further!");

            player.sendMessage(ChatColor.YELLOW + "Demoted to " + gang.getMembers().get(targetId).display);
            SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);

            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.RED + "You have been demoted to " + gang.getMembers().get(targetId).display + ".");
            }

        }
        // --- KICK LOGIC ---
        else if (e.getClick() == ClickType.SHIFT_LEFT) {
            if (current == Rank.LEADER) {
                player.sendMessage(ChatColor.RED + "You cannot kick the leader!");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            plugin.gangManager.kickMember(gang, targetId);

            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                String gName = gang.getName();
                String gColor = gang.getColor();
                targetPlayer.sendMessage(ChatColor.RED + "You were kicked from " +
                        ChatColor.translateAlternateColorCodes('&', gColor + gName) + ChatColor.RED + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Target is offline, but they have been kicked from the gang.");
            }

            player.sendMessage(ChatColor.RED + "Kicked member.");
            SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        refreshGangScoreboards(gang);
    }

    private void setRank(Gang g, UUID u, Rank r) {
        g.getMembers().put(u, r);
        plugin.gangManager.saveGangs();
    }

    private ItemStack createItem(Material mat, String name) {
        return createItem(mat, name, null);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(Collections.singletonList(ChatColor.GRAY + lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}