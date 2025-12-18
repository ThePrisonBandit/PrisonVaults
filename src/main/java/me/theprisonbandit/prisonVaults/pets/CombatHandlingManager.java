package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatHandlingManager {

    private final PrisonVaults plugin;
    private final Map<UUID, AggressionMode> aggressionMap = new HashMap<>();
    private final Map<UUID, CombatStyle> styleMap = new HashMap<>();

    public enum AggressionMode {
        PASSIVE,    // MASTER SWITCH: Pet never attacks, shoots, or explodes.
        AGGRESSIVE  // Pet can attack based on Style (Attack/Defense) and Target Mode (PvE/PvP).
    }

    public enum CombatStyle {
        ATTACK,     // Actively scans for and hunts valid targets.
        DEFENSE     // Only attacks entities that hurt the owner.
    }

    public CombatHandlingManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public AggressionMode getAggression(Player p) {
        return aggressionMap.getOrDefault(p.getUniqueId(), AggressionMode.PASSIVE);
    }

    public CombatStyle getStyle(Player p) {
        return styleMap.getOrDefault(p.getUniqueId(), CombatStyle.DEFENSE);
    }

    public void toggleAggression(Player p) {
        AggressionMode current = getAggression(p);
        AggressionMode next = (current == AggressionMode.PASSIVE) ? AggressionMode.AGGRESSIVE : AggressionMode.PASSIVE;
        aggressionMap.put(p.getUniqueId(), next);

        p.sendMessage(ChatColor.YELLOW + "Pet Aggression: " + (next == AggressionMode.AGGRESSIVE ? ChatColor.RED + "AGGRESSIVE" : ChatColor.GREEN + "PASSIVE"));

        // STRICT: If switching to PASSIVE, immediately stop all anger/combat.
        if (next == AggressionMode.PASSIVE) {
            plugin.petManager.stopPetAttack(p);
            p.sendMessage(ChatColor.GRAY + "Pet is now harmless (No attacks/explosions).");
        }
    }

    public void toggleStyle(Player p) {
        CombatStyle current = getStyle(p);
        CombatStyle next = (current == CombatStyle.ATTACK) ? CombatStyle.DEFENSE : CombatStyle.ATTACK;
        styleMap.put(p.getUniqueId(), next);
        p.sendMessage(ChatColor.YELLOW + "Pet Style: " + (next == CombatStyle.ATTACK ? ChatColor.RED + "ATTACK (Hunt)" : ChatColor.BLUE + "DEFENSE (Protect)"));
    }
}