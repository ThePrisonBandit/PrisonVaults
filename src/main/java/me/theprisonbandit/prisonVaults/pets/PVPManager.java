package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PVPManager {

    private final PrisonVaults plugin;
    private final Set<UUID> pvpEnabledPlayers = new HashSet<>();

    public PVPManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // Toggle PVP for a player
    public void togglePvp(Player player) {
        if (isPvpEnabled(player)) {
            pvpEnabledPlayers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "PVP Mode Disabled. You are now safe from other players.");
        } else {
            pvpEnabledPlayers.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "PVP Mode Enabled. You can now attack and be attacked.");
        }
    }

    // Check if player has PVP enabled
    public boolean isPvpEnabled(Player player) {
        return pvpEnabledPlayers.contains(player.getUniqueId());
    }

    // Check if two players can fight (Both must have PVP enabled)
    public boolean canAttack(Player attacker, Player victim) {
        return isPvpEnabled(attacker) && isPvpEnabled(victim);
    }
}