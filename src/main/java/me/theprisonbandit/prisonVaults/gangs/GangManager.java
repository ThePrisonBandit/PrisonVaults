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

    public final Map<UUID, String> chatInputMode = new HashMap<>();

    private File gangsFile;
    private FileConfiguration gangsConfig;

    public GangManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadGangs();
    }

    // --- Core Logic ---

    public Collection<Gang> getAllGangs() {
        return gangsById.values();
    }

    /**
     * Checks if a gang name is already taken (Case Insensitive)
     */
    public boolean gangNameExists(String name) {
        for (Gang g : gangsById.values()) {
            if (g.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Checks if a gang tag is already taken (Case Insensitive)
     */
    public boolean gangTagExists(String tag) {
        for (Gang g : gangsById.values()) {
            if (g.getTag().equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    public Gang createGang(Player owner, String tag, String name) {
        // 1. Check if player is already in a gang
        if (getPlayerGang(owner.getUniqueId()) != null) {
            owner.sendMessage(ChatColor.RED + "You are already in a gang!");
            return null;
        }

        // 2. NEW: Check if Name is taken
        if (gangNameExists(name)) {
            owner.sendMessage(ChatColor.RED + "A gang with that name already exists!");
            return null;
        }

        // 3. NEW: Check if Tag is taken
        if (gangTagExists(tag)) {
            owner.sendMessage(ChatColor.RED + "A gang with that tag already exists!");
            return null;
        }

        UUID id = UUID.randomUUID();
        Gang gang = new Gang(id, name, tag, owner.getUniqueId());

        gangsById.put(id, gang);
        playerGangCache.put(owner.getUniqueId(), gang);
        saveGangs();

        // Update Scoreboard immediately
        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.setScoreboard(owner);
        }

        return gang;
    }

    public void disbandGang(Gang gang) {
        // Notify and update scoreboard for all members
        for (UUID memberId : gang.getMembers().keySet()) {
            playerGangCache.remove(memberId);
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(ChatColor.RED + "Your gang has been disbanded.");
                if (plugin.scoreboardManager != null) {
                    plugin.scoreboardManager.setScoreboard(p);
                }
            }
        }
        gangsById.remove(gang.getId());

        // Remove from config file specifically to ensure it doesn't come back
        if (gangsConfig.contains("gangs." + gang.getId())) {
            gangsConfig.set("gangs." + gang.getId(), null);
        }

        saveGangs();
    }

    public Gang getPlayerGang(UUID playerId) {
        return playerGangCache.get(playerId);
    }

    public Gang getGangByTag(String tag) {
        for (Gang g : gangsById.values()) {
            if (g.getTag().equalsIgnoreCase(tag)) return g;
        }
        return null;
    }

    // --- File I/O ---

    public void saveGangs() {
        for (Gang gang : gangsById.values()) {
            String path = "gangs." + gang.getId();
            gangsConfig.set(path + ".name", gang.getName());
            gangsConfig.set(path + ".tag", gang.getTag());
            gangsConfig.set(path + ".desc", gang.getDescription());
            gangsConfig.set(path + ".color", gang.getColor());
            gangsConfig.set(path + ".owner", gang.getOwner().toString());

            // Save Members & Ranks
            List<String> memberList = new ArrayList<>();
            for (Map.Entry<UUID, Rank> entry : gang.getMembers().entrySet()) {
                memberList.add(entry.getKey() + ":" + entry.getValue().name());
            }
            gangsConfig.set(path + ".members", memberList);
        }

        try { gangsConfig.save(gangsFile); } catch (IOException e) { e.printStackTrace(); }
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
                String name = sec.getString("name");
                String tag = sec.getString("tag");
                String ownerStr = sec.getString("owner");

                if (name != null && tag != null && ownerStr != null) {
                    UUID owner = UUID.fromString(ownerStr);
                    Gang gang = new Gang(id, name, tag, owner);
                    gang.setDescription(sec.getString("desc"));
                    gang.setColor(sec.getString("color"));

                    List<String> rawMembers = sec.getStringList("members");
                    gang.getMembers().clear();

                    for (String entry : rawMembers) {
                        String[] parts = entry.split(":");
                        if (parts.length >= 2) {
                            UUID memberId = UUID.fromString(parts[0]);
                            Rank rank = Rank.valueOf(parts[1]);
                            gang.getMembers().put(memberId, rank);
                            playerGangCache.put(memberId, gang);
                        }
                    }
                    gangsById.put(id, gang);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load gang: " + key);
                e.printStackTrace();
            }
        }
    }
}