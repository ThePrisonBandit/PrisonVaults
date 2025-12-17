package me.theprisonbandit.prisonVaults.gangs;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GangManager {

    private final PrisonVaults plugin;
    private final Map<UUID, Gang> gangsById = new HashMap<>();
    private final Map<UUID, Gang> playerGangCache = new HashMap<>();
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();
    public final Map<UUID, String> chatInputMode = new HashMap<>();

    private File gangsFile;
    private FileConfiguration gangsConfig;

    public GangManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadGangs();
    }

    // --- CENTRALIZED MENU OPENER ---
    public void openMainGangMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Gang Manager");

        inv.setItem(10, createItem(Material.NAME_TAG, ChatColor.GREEN + "Rename Gang", "Change your gang's name"));
        inv.setItem(11, createItem(Material.OAK_SIGN, ChatColor.GOLD + "Edit Tag", "Change your gang's tag"));
        inv.setItem(12, createItem(Material.PAPER, ChatColor.YELLOW + "Edit Description", "Change gang description"));
        inv.setItem(14, createItem(Material.RED_DYE, ChatColor.LIGHT_PURPLE + "Gang Color", "Change gang name color"));
        inv.setItem(16, createItem(Material.PLAYER_HEAD, ChatColor.AQUA + "Members", "Manage gang members"));

        // NEW: Ban Management Button (Iron Bars)
        inv.setItem(8, createItem(Material.IRON_BARS, ChatColor.DARK_RED + "Banned Players", "Manage gang bans"));

        inv.setItem(22, createItem(Material.TNT, ChatColor.RED + "Disband Gang", "Delete the gang forever"));
        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close", "Close Menu"));

        player.openInventory(inv);
        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
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

    public boolean gangExists(String name) {
        return getGangByName(name) != null;
    }

    public Gang getGang(String name) {
        return getGangByName(name);
    }

    public void kickMember(Gang gang, UUID memberId) {
        gang.getMembers().remove(memberId);
        playerGangCache.remove(memberId);
        saveGangs();
        Player target = Bukkit.getPlayer(memberId);
        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.RED + "You have been kicked from the gang.");
            plugin.scoreboardManager.setScoreboard(target);
        }
    }

    public void invitePlayer(Gang gang, Player target) {
        // NEW: Check if banned
        if (gang.isBanned(target.getUniqueId())) {
            Player owner = Bukkit.getPlayer(gang.getOwner());
            if (owner != null) owner.sendMessage(ChatColor.RED + "That player is banned from your gang!");
            return;
        }

        pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(gang.getName());
        target.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        target.sendMessage(ChatColor.GREEN + "Invited to join " + ChatColor.GOLD + gang.getName());
        target.sendMessage(ChatColor.YELLOW + "/gang join " + gang.getName());
        target.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }

    public boolean hasInvite(Player player, String gangName) {
        return pendingInvites.containsKey(player.getUniqueId()) &&
                pendingInvites.get(player.getUniqueId()).contains(gangName);
    }

    public void joinGang(Player player, Gang gang) {
        // NEW: Double check ban
        if (gang.isBanned(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are banned from this gang.");
            return;
        }

        if (pendingInvites.containsKey(player.getUniqueId())) {
            pendingInvites.get(player.getUniqueId()).remove(gang.getName());
        }
        gang.getMembers().put(player.getUniqueId(), Rank.MEMBER);
        playerGangCache.put(player.getUniqueId(), gang);
        saveGangs();

        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.setScoreboard(player);
            Player owner = Bukkit.getPlayer(gang.getOwner());
            if (owner != null) plugin.scoreboardManager.setScoreboard(owner);
        }
        player.sendMessage(ChatColor.GREEN + "Joined " + gang.getName() + "!");
    }

    public Gang createGang(Player owner, String tag, String name) {
        if (getPlayerGang(owner.getUniqueId()) != null) { owner.sendMessage(ChatColor.RED + "Already in gang."); return null; }
        if (gangNameExists(name) || gangTagExists(tag)) { owner.sendMessage(ChatColor.RED + "Name/Tag taken."); return null; }
        UUID id = UUID.randomUUID();
        Gang gang = new Gang(id, name, tag, owner.getUniqueId());
        gangsById.put(id, gang);
        playerGangCache.put(owner.getUniqueId(), gang);
        saveGangs();
        plugin.scoreboardManager.setScoreboard(owner);
        return gang;
    }

    public void disbandGang(Gang gang) {
        for (UUID memberId : gang.getMembers().keySet()) {
            playerGangCache.remove(memberId);
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(ChatColor.RED + "Gang disbanded.");
                plugin.scoreboardManager.setScoreboard(p);
            }
        }
        gangsById.remove(gang.getId());
        if (gangsConfig.contains("gangs." + gang.getId())) gangsConfig.set("gangs." + gang.getId(), null);
        saveGangs();
    }

    public Gang getPlayerGang(UUID playerId) { return playerGangCache.get(playerId); }
    public Gang getGangByName(String name) { for (Gang g : gangsById.values()) if (g.getName().equalsIgnoreCase(name)) return g; return null; }
    public Gang getGangByTag(String tag) { for (Gang g : gangsById.values()) if (g.getTag().equalsIgnoreCase(tag)) return g; return null; }
    public boolean gangNameExists(String name) { return getGangByName(name) != null; }
    public boolean gangTagExists(String tag) { return getGangByTag(tag) != null; }
    public Collection<Gang> getAllGangs() { return gangsById.values(); }

    public void saveGangs() {
        for (Gang gang : gangsById.values()) {
            String path = "gangs." + gang.getId();
            gangsConfig.set(path + ".name", gang.getName());
            gangsConfig.set(path + ".tag", gang.getTag());
            gangsConfig.set(path + ".desc", gang.getDescription());
            gangsConfig.set(path + ".color", gang.getColor());
            gangsConfig.set(path + ".owner", gang.getOwner().toString());

            // Save Members
            List<String> memberList = new ArrayList<>();
            for (Map.Entry<UUID, Rank> entry : gang.getMembers().entrySet()) {
                memberList.add(entry.getKey() + ":" + entry.getValue().name());
            }
            gangsConfig.set(path + ".members", memberList);

            // NEW: Save Bans
            List<String> bannedList = new ArrayList<>();
            for (UUID uuid : gang.getBannedPlayers()) {
                bannedList.add(uuid.toString());
            }
            gangsConfig.set(path + ".banned", bannedList);
        }
        try { gangsConfig.save(gangsFile); } catch (IOException e) {}
    }

    private void loadGangs() {
        gangsFile = new File(plugin.getDataFolder(), "gangs.yml");
        if (!gangsFile.exists()) plugin.saveResource("gangs.yml", false);
        gangsConfig = YamlConfiguration.loadConfiguration(gangsFile);
        if (!gangsConfig.contains("gangs")) return;

        for (String key : gangsConfig.getConfigurationSection("gangs").getKeys(false)) {
            try {
                ConfigurationSection sec = gangsConfig.getConfigurationSection("gangs." + key);
                UUID id = UUID.fromString(key);
                Gang gang = new Gang(id, sec.getString("name"), sec.getString("tag"), UUID.fromString(sec.getString("owner")));
                gang.setDescription(sec.getString("desc"));
                gang.setColor(sec.getString("color"));

                // Load Members
                for (String entry : sec.getStringList("members")) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) {
                        UUID mid = UUID.fromString(parts[0]);
                        gang.getMembers().put(mid, Rank.valueOf(parts[1]));
                        playerGangCache.put(mid, gang);
                    }
                }

                // NEW: Load Bans
                if (sec.contains("banned")) {
                    for (String s : sec.getStringList("banned")) {
                        gang.addBan(UUID.fromString(s));
                    }
                }

                gangsById.put(id, gang);
            } catch (Exception e) {}
        }
    }

    // --- LEAVE GANG LOGIC ---
    public void leaveGang(Player player, Gang gang) {
        // 1. Remove from Gang Data
        gang.getMembers().remove(player.getUniqueId());
        playerGangCache.remove(player.getUniqueId());

        // 2. Wipe Player Data (Remove gang association from their profile if stored there)
        FileConfiguration data = plugin.getPlayerData(player.getUniqueId());
        data.set("gang", null);
        try {
            data.save(plugin.getPlayerDataFile(player.getUniqueId()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Notify the Player
        player.sendMessage(ChatColor.YELLOW + "You left " + gang.getColor() + gang.getName() + ChatColor.YELLOW + "~!");
        SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

        // 4. Notify the Leader (if online)
        Player leader = Bukkit.getPlayer(gang.getOwner());
        if (leader != null && leader.isOnline()) {
            leader.sendMessage(ChatColor.RED + "Notification: " + ChatColor.WHITE + player.getName() +
                    ChatColor.RED + " left the " + gang.getColor() + gang.getName() + ChatColor.RED + "~!");
            SoundUtils.playSound(leader, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }

        // 5. Save Changes
        saveGangs();
    }
}