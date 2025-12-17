package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompassManager {

    private final PrisonVaults plugin;
    private final Map<UUID, BossBar> compassBars = new HashMap<>();
    private final String COMPASS_STR = "N  .  .  .  NE .  .  .  E  .  .  .  SE .  .  .  S  .  .  .  SW .  .  .  W  .  .  .  NW .  .  .  ";

    public CompassManager(PrisonVaults plugin) {
        this.plugin = plugin;
        startCompassTask();
    }

    public void toggleCompass(Player player) {
        if (compassBars.containsKey(player.getUniqueId())) {
            // Disable
            BossBar bar = compassBars.remove(player.getUniqueId());
            bar.removeAll();
            player.sendMessage(ChatColor.YELLOW + "Compass HUD: " + ChatColor.RED + "OFF");
        } else {
            // Enable
            BossBar bar = Bukkit.createBossBar("Compass", BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            bar.setVisible(true);
            compassBars.put(player.getUniqueId(), bar);
            player.sendMessage(ChatColor.YELLOW + "Compass HUD: " + ChatColor.GREEN + "ON");
        }
    }

    // Removes a specific player's compass (used on quit/toggle)
    public void removeCompass(Player player) {
        if (compassBars.containsKey(player.getUniqueId())) {
            compassBars.get(player.getUniqueId()).removeAll();
            compassBars.remove(player.getUniqueId());
        }
    }

    // --- NEW METHOD: Removes ALL compasses (used on server disable) ---
    public void removeAll() {
        for (BossBar bar : compassBars.values()) {
            bar.removeAll();
        }
        compassBars.clear();
    }

    private void startCompassTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, BossBar> entry : compassBars.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) {
                        continue;
                    }
                    updateBar(entry.getValue(), p);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L); // Update every 0.1 seconds (smooth)
    }

    private void updateBar(BossBar bar, Player p) {
        float yaw = p.getLocation().getYaw();
        // Normalize yaw to 0-360
        yaw = (yaw % 360 + 360) % 360;

        // The compass string represents 360 degrees.
        // We repeat the string 3 times to handle the "wrapping" easily.
        String fullScroll = COMPASS_STR + COMPASS_STR + COMPASS_STR;

        // Calculate index based on yaw (ratio of string length)
        int totalLen = COMPASS_STR.length();
        int index = (int) ((yaw / 360.0) * totalLen);

        // Add offset to center the display (approx half the view width)
        int viewWidth = 12; // Characters visible at once
        int start = totalLen + index - (viewWidth / 2);

        String visible = fullScroll.substring(start, start + viewWidth);

        bar.setTitle(ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + visible + ChatColor.DARK_GRAY + "]");
        bar.setProgress(1.0);
    }
}