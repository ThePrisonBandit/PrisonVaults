package me.theprisonbandit.prisonVaults.tasks;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AnimationTask extends BukkitRunnable {

    private final PrisonVaults plugin;
    private int step = 0;

    public AnimationTask(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // INCREMENT BY 2 or 3 to make the colors cycle faster!
        step += 3;

        // Reset loop (100 is usually the gradient length)
        if (step > 100) step = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.scoreboardManager.updateNameAnimation(player, step);
        }
    }
}