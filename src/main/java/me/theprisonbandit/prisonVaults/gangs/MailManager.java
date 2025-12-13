package me.theprisonbandit.prisonVaults.gangs;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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

    // --- NEW METHOD FOR MAIL COMMANDS ---
    public void sendMailToGang(Gang gang, String senderName, String message) {
        for (UUID memberId : gang.getMembers().keySet()) {
            sendMail(senderName, memberId, "[Gang Mail] " + message);
        }
    }
    // -------------------------------------

    public void sendMail(String sender, UUID receiverId, String message) {
        File f = plugin.getPlayerDataFile(receiverId);
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);

        List<String> inbox = data.getStringList("mail");
        // Format: Sender;Timestamp;Message
        String entry = sender + ";" + System.currentTimeMillis() + ";" + message;
        inbox.add(entry);

        data.set("mail", inbox);
        try { data.save(f); } catch (IOException e) { e.printStackTrace(); }

        Player target = Bukkit.getPlayer(receiverId);
        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.GOLD + "You have new mail! Type /inbox to read.");
            SoundUtils.playSound(target, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        }
    }

    public void sendGangMail(Player sender, String message) {
        Gang gang = plugin.gangManager.getPlayerGang(sender.getUniqueId());
        if (gang == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a gang.");
            return;
        }
        sendMailToGang(gang, sender.getName(), message);
        sender.sendMessage(ChatColor.GREEN + "Mail sent to your gang.");
    }

    public List<Mail> getInbox(UUID playerId) {
        File f = plugin.getPlayerDataFile(playerId);
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        List<String> raw = data.getStringList("mail");
        List<Mail> mails = new ArrayList<>();

        for (String s : raw) {
            String[] parts = s.split(";", 3);
            if (parts.length == 3) {
                // FIXED: Convert timestamp (parts[1]) to long
                try {
                    long timestamp = Long.parseLong(parts[1]);
                    mails.add(new Mail(parts[0], timestamp, parts[2]));
                } catch (NumberFormatException e) {
                    // Fallback if data is corrupted
                    mails.add(new Mail(parts[0], System.currentTimeMillis(), parts[2]));
                }
            }
        }
        Collections.reverse(mails);
        return mails;
    }

    public void clearInbox(UUID playerId) {
        File f = plugin.getPlayerDataFile(playerId);
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        data.set("mail", null);
        try { data.save(f); } catch (IOException e) {}
    }
}