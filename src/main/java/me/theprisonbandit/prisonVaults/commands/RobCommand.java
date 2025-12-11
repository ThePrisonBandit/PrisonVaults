package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RobCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public RobCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // Settings
    private static final double ROB_CHANCE = 0.0001; // 0.01% Chance
    private static final double MIN_ROB = 100.0;
    private static final double MAX_ROB = 50000.0;
    private static final long COOLDOWN_SECONDS = 7200; // 2 Hours

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can rob people.");
            return true;
        }

        Player robber = (Player) sender;

        // 1. Permission Check
        if (!robber.hasPermission("prisonvaults.rob")) {
            robber.sendMessage(ChatColor.RED + "You do not have permission to use /rob.");
            return true;
        }

        // --- 2. COOLDOWN CHECK (NEW) ---
        if (plugin.cooldownManager.isOnCooldown(robber.getUniqueId(), "rob")) {
            long remaining = plugin.cooldownManager.getRemainingTime(robber.getUniqueId(), "rob");
            robber.sendMessage(ChatColor.RED + "You must wait " + formatTime(remaining) + " before robbing again.");
            return true;
        }

        // 3. Syntax Check
        if (args.length < 1) {
            robber.sendMessage(ChatColor.RED + "Usage: /rob <player>");
            return true;
        }

        Player victim = Bukkit.getPlayer(args[0]);

        // 4. Target Validation
        if (victim == null || !victim.isOnline()) {
            robber.sendMessage(ChatColor.RED + "Player not found or is offline.");
            return true;
        }

        if (victim.getUniqueId().equals(robber.getUniqueId())) {
            robber.sendMessage(ChatColor.RED + "You cannot rob yourself.");
            return true;
        }

        // --- APPLY COOLDOWN HERE ---
        // We apply it on ATTEMPT, so they can't spam the 0.01% chance.
        plugin.cooldownManager.setCooldown(robber.getUniqueId(), "rob", COOLDOWN_SECONDS);

        // 5. Calculate Success Chance
        if (Math.random() > ROB_CHANCE) {
            // FAILED ATTEMPT
            robber.sendMessage(ChatColor.RED + "§lROB FAILED! §cYou tried to rob " + victim.getName() + " but got caught by the guards!");
            victim.sendMessage(ChatColor.YELLOW + "§lALERT! §e" + robber.getName() + " tried to rob you but failed!");
            return true;
        }

        // 6. SUCCESSFUL ROB LOGIC
        double victimBalance = plugin.getBalance(victim);

        if (victimBalance < MIN_ROB) {
            robber.sendMessage(ChatColor.RED + victim.getName() + " does not have enough money to be robbed (Min: $" + MIN_ROB + ").");
            return true;
        }

        double amountToRob = ThreadLocalRandom.current().nextDouble(MIN_ROB, MAX_ROB);

        if (amountToRob > victimBalance) {
            amountToRob = victimBalance;
        }

        // 7. Execute Transaction
        plugin.removeMoney(victim, amountToRob);
        plugin.addMoney(robber, amountToRob);

        // 8. Update Scoreboards
        if (plugin.scoreboardManager != null) {
            plugin.scoreboardManager.updateScoreboard(robber);
            plugin.scoreboardManager.updateScoreboard(victim);
        }

        // 9. Messages
        String formattedAmount = String.format("%.2f", amountToRob);
        robber.sendMessage(ChatColor.GREEN + "§lROBBERY SUCCESSFUL! §aYou stole §2$" + formattedAmount + " §afrom " + victim.getName() + "!");
        victim.sendMessage(ChatColor.RED + "§lYOU WERE ROBBED! §c" + robber.getName() + " stole §4$" + formattedAmount + " §cfrom you!");

        return true;
    }

    // Helper to make "7100 seconds" look like "1h 58m 20s"
    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(s).append("s");

        return sb.toString().trim();
    }
}