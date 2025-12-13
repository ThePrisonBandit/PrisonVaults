package me.theprisonbandit.prisonVaults.gangs;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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

    // --- METHODS FOR MAIL COMMANDS ---
    public boolean gangExists(String name) {
        return getGangByName(name) != null;
    }

    public Gang getGang(String name) {
        return getGangByName(name);
    }
    // -------------------------------

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
            List<String> memberList = new ArrayList<>();
            for (Map.Entry<UUID, Rank> entry : gang.getMembers().entrySet()) {
                memberList.add(entry.getKey() + ":" + entry.getValue().name());
            }
            gangsConfig.set(path + ".members", memberList);
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
                for (String entry : sec.getStringList("members")) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) {
                        UUID mid = UUID.fromString(parts[0]);
                        gang.getMembers().put(mid, Rank.valueOf(parts[1]));
                        playerGangCache.put(mid, gang);
                    }
                }
                gangsById.put(id, gang);
            } catch (Exception e) {}
        }
    }
}