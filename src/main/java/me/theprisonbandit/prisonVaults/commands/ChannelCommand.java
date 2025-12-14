package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.managers.ChatChannelManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class ChannelCommand implements CommandExecutor {

    private final PrisonVaults plugin;
    private final ChatChannelManager.Channel targetChannel;

    public ChannelCommand(PrisonVaults plugin, ChatChannelManager.Channel targetChannel) {
        this.plugin = plugin;
        this.targetChannel = targetChannel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        // Permission Check
        if (targetChannel == ChatChannelManager.Channel.STAFF && !p.hasPermission("prisonvaults.staff")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        // 1. Toggle Mode (No args)
        if (args.length == 0) {
            plugin.chatChannelManager.toggleChannel(p, targetChannel);
            return true;
        }

        // 2. Quick Message (Args present)
        String msg = String.join(" ", args);
        if (targetChannel == ChatChannelManager.Channel.STAFF) {
            plugin.chatChannelManager.sendStaffMessage(p, msg);
        } else {
            plugin.chatChannelManager.sendGangMessage(p, msg);
        }

        return true;
    }
}