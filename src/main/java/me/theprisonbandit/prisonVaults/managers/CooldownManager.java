package me.theprisonbandit.prisonVaults.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // Map<PlayerUUID, Map<CommandKey, ExpiryTimestamp>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Set a cooldown for a specific player and command key.
     * @param playerUUID The player's UUID
     * @param key The unique key for the cooldown (e.g., "rob", "kit_daily")
     * @param seconds How many seconds the cooldown lasts
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
     * Returns 0 if no cooldown exists or it has expired.
     */
    public long getRemainingTime(UUID playerUUID, String key) {
        if (!cooldowns.containsKey(playerUUID) || !cooldowns.get(playerUUID).containsKey(key)) {
            return 0;
        }

        long expiry = cooldowns.get(playerUUID).get(key);
        long current = System.currentTimeMillis();

        if (current >= expiry) {
            // Cleanup expired entry to save memory
            cooldowns.get(playerUUID).remove(key);
            return 0;
        }

        return (expiry - current) / 1000;
    }
}