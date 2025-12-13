package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddMoneyCommand implements CommandExecutor {

    private final PrisonVaults plugin;
    // 999 Decillion = 9.99 x 10^35
    private static final double MAX_BALANCE = 9.99E35;

    public AddMoneyCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prisonvaults.admin")) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /addmoney <player> <amount>");
            SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
            SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        // --- CAP LOGIC ---
        double currentBal = plugin.getBalance(target);

        if (currentBal >= MAX_BALANCE) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already at the MAX balance (999 Decillion)!");
            SoundUtils.playSound((Player) sender, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        if (currentBal + amount > MAX_BALANCE) {
            amount = MAX_BALANCE - currentBal;
            sender.sendMessage(ChatColor.YELLOW + "(!) Amount capped to reach the limit of 999 Decillion.");
        }
        // -----------------

        plugin.addMoney(target, amount);
        plugin.scoreboardManager.updateScoreboard(target);

        sender.sendMessage(ChatColor.GREEN + "Added " + "$" + NumberUtils.format(amount) + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "Received " + "$" + NumberUtils.format(amount));

        if (sender instanceof Player) {
            SoundUtils.playDualSound((Player) sender, target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            SoundUtils.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        return true;
    }
}