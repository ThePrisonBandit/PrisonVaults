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
        PVP, // Attacks Players, Mobs, and Pets
        PVE  // Attacks Mobs only (Ignores Players and Pets)
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
        player.sendMessage(ChatColor.YELLOW + "Pet Target Mode: " + (next == Mode.PVP ? ChatColor.RED + "PvP (Aggressive)" : ChatColor.GREEN + "PvE (Passive)"));
    }

    /**
     * Determines if a Pet owned by 'owner' is allowed to attack 'target' based on current Mode.
     */
    public boolean canPetAttack(Player owner, Entity target) {
        // Rule 1: Never attack the owner
        if (target.equals(owner)) return false;

        Mode mode = getMode(owner);

        // PvE Mode Rules
        if (mode == Mode.PVE) {
            // Cannot attack Players
            if (target instanceof Player) return false;

            // Cannot attack other Pets
            if (plugin.petManager.isPet(target)) return false;
        }

        // PvP Mode Rules (Target is valid, but we might check PVPManager for region safety later)
        return true;
    }
}