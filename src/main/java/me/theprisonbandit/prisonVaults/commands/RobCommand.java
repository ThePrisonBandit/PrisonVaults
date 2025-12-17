package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RobCommand implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;
    private static final double MIN_ROB = 100.0;
    private static final double MAX_ROB = 50000.0;
    private static final long COOLDOWN_SECONDS = 7200;

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
        double successChance = plugin.getConfig().getDouble("rob.success-chance", 0.05);

        if (Math.random() > successChance) {
            robber.sendMessage(ChatColor.RED + "§lROB FAILED! §cYou were caught!");
            victim.sendMessage(ChatColor.YELLOW + "§lALERT! §e" + robber.getName() + " tried to rob you but failed!");
            SoundUtils.playSound(robber, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            SoundUtils.playSound(victim, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
            return true;
        }

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

        SoundUtils.playSound(robber, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        SoundUtils.playSound(victim, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);

        return true;
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return h + "h " + m + "m " + (seconds % 60) + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> validTargets = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equals(sender.getName())) { // Don't rob yourself
                    validTargets.add(p.getName());
                }
            }
            return validTargets;
        }
        return Collections.emptyList();
    }
}