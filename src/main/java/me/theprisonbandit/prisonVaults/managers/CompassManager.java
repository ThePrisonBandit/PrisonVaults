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
    private final Map<UUID, BossBar> activeBars = new HashMap<>();

    // The visual strip. We repeat it 3 times to make the "scrolling" window logic easier.
    // N = North, E = East, S = South, W = West
    private static final String COMPASS_STRIP = "N  |  NE  |  E  |  SE  |  S  |  SW  |  W  |  NW  |  N  |  NE  |  E  |  SE  |  S  |  SW  |  W  |  NW  |  N  |  NE  |  E  |  SE  |  S";

    public CompassManager(PrisonVaults plugin) {
        this.plugin = plugin;
        startTask();
    }

    public void toggleCompass(Player player) {
        if (activeBars.containsKey(player.getUniqueId())) {
            removeCompass(player);
            player.sendMessage(ChatColor.YELLOW + "Compass disabled.");
        } else {
            createCompass(player);
            player.sendMessage(ChatColor.GREEN + "Compass enabled.");
        }
    }

    public void createCompass(Player player) {
        if (activeBars.containsKey(player.getUniqueId())) return;

        // Create a Blue BossBar with no progress (just text)
        BossBar bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        bar.setProgress(0.0); // Empty bar so it looks like just UI text
        bar.addPlayer(player);
        activeBars.put(player.getUniqueId(), bar);
    }

    public void removeCompass(Player player) {
        if (activeBars.containsKey(player.getUniqueId())) {
            activeBars.get(player.getUniqueId()).removePlayer(player);
            activeBars.remove(player.getUniqueId());
        }
    }

    public void removeAll() {
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeBars.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        updateBar(p, activeBars.get(uuid));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Updates every 2 ticks (smooth enough)
    }

    private void updateBar(Player p, BossBar bar) {
        float yaw = p.getLocation().getYaw();
        // Normalize yaw to 0-360
        yaw = (yaw % 360 + 360) % 360;

        // The strip represents 360 degrees.
        // We want to show a "window" of text.
        // Let's map 0-360 to an index in our string.

        // Total "Cycle" length in characters roughly representing 360 degrees
        // Our pattern "N  |  NE  |  E..." has a specific repeating length.
        // Each segment "N  |  " is roughly 6 chars. 8 directions * 6 = 48 chars per 360 deg.

        int totalWindowSize = 48;

        // Calculate start index based on yaw
        int index = (int) ((yaw / 360.0) * totalWindowSize);

        // Shift index to center the strip (because our strip is tripled)
        // We start deeper in the string so we don't go out of bounds
        int offset = 48; // Skip the first full rotation worth of chars

        int start = index + offset - 10; // -10 to show characters "behind" the center
        int end = index + offset + 10;   // +10 to show characters "ahead"

        if (start < 0) start = 0;
        if (end > COMPASS_STRIP.length()) end = COMPASS_STRIP.length();

        String visibleText = COMPASS_STRIP.substring(start, end);

        // Highlight the center character (The direction you are facing)
        // This is tricky with plain text, so we just display the strip.
        // We add a little marker in the title to show "Center"

        bar.setTitle(ChatColor.GRAY + "[" + ChatColor.AQUA + visibleText + ChatColor.GRAY + "]");
    }
}