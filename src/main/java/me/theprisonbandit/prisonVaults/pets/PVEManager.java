package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PVEManager {

    private final PrisonVaults plugin;
    private final Map<UUID, Mode> playerModes = new HashMap<>();

    public enum Mode {
        PVP, // Attacks Players and Pets ONLY (Ignores Mobs)
        PVE  // Attacks Mobs ONLY (Ignores Players and Pets)
    }

    public PVEManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public Mode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), Mode.PVE);
    }

    public void setMode(Player player, Mode mode) {
        playerModes.put(player.getUniqueId(), mode);
    }

    public void toggleMode(Player player) {
        Mode current = getMode(player);
        Mode next = (current == Mode.PVE) ? Mode.PVP : Mode.PVE;
        setMode(player, next);
        player.sendMessage(ChatColor.YELLOW + "Pet Target Mode: " + (next == Mode.PVP ? ChatColor.RED + "PvP (Players/Pets)" : ChatColor.GREEN + "PvE (Mobs Only)"));
    }

    /**
     * Determines if a Pet owned by 'owner' is allowed to attack 'target' based on current Mode.
     */
    public boolean canPetAttack(Player owner, Entity target) {
        // Rule 1: Never attack the owner
        if (target.equals(owner)) return false;

        Mode mode = getMode(owner);
        boolean isPet = plugin.petManager.isPet(target);
        boolean isPlayer = target instanceof Player;

        // PvE Mode Rules (Strict: Mobs only)
        if (mode == Mode.PVE) {
            // Cannot attack Players
            if (isPlayer) return false;

            // Cannot attack other Pets
            if (isPet) return false;

            // Allow Mobs/Monsters/Animals
            return true;
        }

        // PvP Mode Rules (Strict: Players & Pets only)
        if (mode == Mode.PVP) {
            // Can attack Players
            if (isPlayer) return true;

            // Can attack other Pets
            if (isPet) return true;

            // STRICTLY IGNORE Mobs (Zombies, etc.) in PvP Mode
            return false;
        }

        return false;
    }
}