package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final PrisonVaults plugin;
    // Changed access modifier slightly or kept private but added getter/clearer
    public final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final File file;
    private FileConfiguration config;

    public CooldownManager(PrisonVaults plugin) {
        this.plugin = plugin;
        // Create/Load the file
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        // Load data immediately on startup
        loadCooldowns();
    }

    // --- NEW: RESET METHOD ---
    public void resetAllCooldowns() {
        this.cooldowns.clear();
        // Clear the file config as well
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // -------------------------

    /**
     * Set a cooldown for a specific player and command key.
     */
    public void setCooldown(UUID playerUUID, String key, long seconds) {
        long expiry = System.currentTimeMillis() + (seconds * 1000);
        cooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(key, expiry);
    }

    /**
     * Check if a player is currently on cooldown.
     */
    public boolean isOnCooldown(UUID playerUUID, String key) {
        long remaining = getRemainingTime(playerUUID, key);
        return remaining > 0;
    }

    /**
     * Get the remaining seconds left on a cooldown.
     */
    public long getRemainingTime(UUID playerUUID, String key) {
        if (!cooldowns.containsKey(playerUUID) || !cooldowns.get(playerUUID).containsKey(key)) {
            return 0;
        }

        long expiry = cooldowns.get(playerUUID).get(key);
        long current = System.currentTimeMillis();

        if (current >= expiry) {
            cooldowns.get(playerUUID).remove(key);
            return 0;
        }

        return (expiry - current) / 1000;
    }

    /**
     * Save all active cooldowns to cooldowns.yml
     * Call this in onDisable()!
     */
    public void saveCooldowns() {
        // Clear old config to prevent ghost data
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            UUID uuid = entry.getKey();
            for (Map.Entry<String, Long> cooldown : entry.getValue().entrySet()) {
                String key = cooldown.getKey();
                long expiry = cooldown.getValue();

                // Only save if the cooldown is still active in the future
                if (expiry > System.currentTimeMillis()) {
                    config.set(uuid.toString() + "." + key, expiry);
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save cooldowns.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Load cooldowns from file into memory
     */
    private void loadCooldowns() {
        if (!file.exists()) return;

        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection section = config.getConfigurationSection(uuidStr);
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        long expiry = section.getLong(key);
                        // Only load if it hasn't expired yet
                        if (expiry > System.currentTimeMillis()) {
                            cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, expiry);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid UUIDs in file
            }
        }
    }
}