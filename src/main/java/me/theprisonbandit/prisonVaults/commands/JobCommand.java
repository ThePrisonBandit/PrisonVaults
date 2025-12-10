package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {

    private final PrisonVaults plugin;

    public JobCommand(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /job join <job> OR /job quit");
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /job join Cooking");
                return true;
            }
            plugin.jobManager.joinJob(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("quit")) {
            plugin.jobManager.quitJob(player);
            return true;
        }

        return true;
    }
}