package me.theprisonbandit.prisonVaults.gangs;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MailManager {
    private final PrisonVaults plugin;

    public MailManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    public void sendMail(String senderName, UUID targetUUID, String message) {
        File file = plugin.getPlayerDataFile(targetUUID);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        List<String> mails = data.getStringList("inbox");
        // Format: Timestamp|Sender|Message
        String mailEntry = System.currentTimeMillis() + "|" + senderName + "|" + message;
        mails.add(0, mailEntry); // Add to top

        data.set("inbox", mails);
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(ChatColor.GOLD + "You have new mail! Type /inbox to read.");
        }
    }

    public void sendGangMail(Player sender, String message) {
        Gang gang = plugin.gangManager.getPlayerGang(sender.getUniqueId());
        if (gang == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a gang.");
            return;
        }

        for (UUID memberId : gang.getMembers().keySet()) {
            sendMail(sender.getName() + " (Gang)", memberId, message);
        }
        sender.sendMessage(ChatColor.GREEN + "Gang mail sent!");
    }

    public List<Mail> getInbox(UUID uuid) {
        File file = plugin.getPlayerDataFile(uuid);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> raw = data.getStringList("inbox");
        List<Mail> inbox = new ArrayList<>();

        for (String entry : raw) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length == 3) {
                inbox.add(new Mail(parts[1], parts[2], Long.parseLong(parts[0])));
            }
        }
        return inbox;
    }

    public void clearInbox(UUID uuid) {
        File file = plugin.getPlayerDataFile(uuid);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set("inbox", null);
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}