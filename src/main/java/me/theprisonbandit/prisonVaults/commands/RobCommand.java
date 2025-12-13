package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

public class RobCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    // Settings
    // Note: Success Chance is now in config.yml (rob.success-chance)
    private static final double MIN_ROB = 100.0;
    private static final double MAX_ROB = 50000.0;
    private static final long COOLDOWN_SECONDS = 7200; // 2 Hours

    public RobCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player robber = (Player) sender;

        if (!robber.hasPermission("prisonvaults.rob")) {
            robber.sendMessage(ChatColor.RED + "You do not have permission to use /rob.");
            SoundUtils.playSound(robber, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        if (plugin.cooldownManager.isOnCooldown(robber.getUniqueId(), "rob")) {
            long remaining = plugin.cooldownManager.getRemainingTime(robber.getUniqueId(), "rob");
            robber.sendMessage(ChatColor.RED + "You must wait " + formatTime(remaining) + " before robbing again.");
            SoundUtils.playSound(robber, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        if (args.length < 1) {
            robber.sendMessage(ChatColor.RED + "Usage: /rob <player>");
            return true;
        }

        Player victim = Bukkit.getPlayer(args[0]);

        if (victim == null || !victim.isOnline() || victim.getUniqueId().equals(robber.getUniqueId())) {
            robber.sendMessage(ChatColor.RED + "Invalid target.");
            SoundUtils.playSound(robber, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        plugin.cooldownManager.setCooldown(robber.getUniqueId(), "rob", COOLDOWN_SECONDS);

        // --- NEW: CONFIGURABLE CHANCE ---
        // Defaults to 0.05 (5%) if not found in config
        double successChance = plugin.getConfig().getDouble("rob.success-chance", 0.05);

        // FAILED ATTEMPT
        if (Math.random() > successChance) {
            robber.sendMessage(ChatColor.RED + "§lROB FAILED! §cYou were caught!");
            victim.sendMessage(ChatColor.YELLOW + "§lALERT! §e" + robber.getName() + " tried to rob you but failed!");

            // Robber hears failure
            SoundUtils.playSound(robber, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            // Victim hears alarm
            SoundUtils.playSound(victim, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
            return true;
        }

        // SUCCESSFUL ROB LOGIC
        double victimBalance = plugin.getBalance(victim);
        if (victimBalance < MIN_ROB) {
            robber.sendMessage(ChatColor.RED + "Target is too poor.");
            return true;
        }

        double amountToRob = ThreadLocalRandom.current().nextDouble(MIN_ROB, MAX_ROB);
        if (amountToRob > victimBalance) amountToRob = victimBalance;

        plugin.removeMoney(victim, amountToRob);
        plugin.addMoney(robber, amountToRob);

        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.updateScoreboard(robber);
            plugin.scoreboardManager.updateScoreboard(victim);
        }

        String formattedAmount = String.format("%.2f", amountToRob);
        robber.sendMessage(ChatColor.GREEN + "§lROBBERY SUCCESSFUL! §aStole §2$" + formattedAmount);
        victim.sendMessage(ChatColor.RED + "§lYOU WERE ROBBED! §cLost §4$" + formattedAmount);

        // Robber hears Money Sound
        SoundUtils.playSound(robber, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        // Victim hears Alarm
        SoundUtils.playSound(victim, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);

        return true;
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return h + "h " + m + "m " + (seconds % 60) + "s";
    }
}