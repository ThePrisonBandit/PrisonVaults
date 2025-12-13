package me.theprisonbandit.prisonVaults.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {
    // Plays a sound to a specific player
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    // Plays a sound to both the sender (admin) and the target (player being modified)
    public static void playDualSound(Player sender, Player target, Sound sound, float volume, float pitch) {
        playSound(sender, sound, volume, pitch);
        if (target != null && !target.equals(sender) && target.isOnline()) {
            playSound(target, sound, volume, pitch);
        }
    }
}
