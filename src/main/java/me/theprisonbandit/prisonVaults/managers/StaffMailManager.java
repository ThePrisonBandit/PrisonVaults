package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StaffMailManager implements Listener {

    private final PrisonVaults plugin;
    private File staffMailFile;
    private FileConfiguration staffMailConfig;

    public StaffMailManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadGlobalMail();
    }

    private void loadGlobalMail() {
        staffMailFile = new File(plugin.getDataFolder(), "staff-mail.yml");
        if (!staffMailFile.exists()) {
            try { staffMailFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        staffMailConfig = YamlConfiguration.loadConfiguration(staffMailFile);
    }

    public void saveGlobalMail() {
        try { staffMailConfig.save(staffMailFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // --- 1. GLOBAL MAIL (Send to All Staff) ---
    public void sendToAllStaff(String senderName, String message) {
        // A. Save to Global File (For offline staff to see later)
        List<String> mail = staffMailConfig.getStringList("inbox");
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        mail.add(ChatColor.GOLD + "[" + timestamp + "] " + ChatColor.AQUA + senderName + ": " + ChatColor.WHITE + message);

        staffMailConfig.set("inbox", mail);
        saveGlobalMail();

        // B. Notify Online Staff Immediately
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.rankManager.isStaff(p)) {
                p.sendMessage(ChatColor.RED + "[Staff Mail] " + ChatColor.YELLOW + senderName + ": " + ChatColor.WHITE + message);
                SoundUtils.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
            }
        }
    }

    public void readGlobalMail(Player player) {
        List<String> mail = staffMailConfig.getStringList("inbox");
        if (mail.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Global Staff Inbox is empty.");
        } else {
            player.sendMessage(ChatColor.DARK_RED + "--- GLOBAL STAFF MAIL (" + mail.size() + ") ---");
            for (String msg : mail) {
                player.sendMessage(msg);
            }
            player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "/staffmail clear" + ChatColor.GRAY + " to clear all messages.");
        }
    }

    public void clearGlobalMail(Player player) {
        staffMailConfig.set("inbox", new ArrayList<>());
        saveGlobalMail();
        player.sendMessage(ChatColor.RED + "Global Staff Inbox cleared.");

        // Notify other online staff
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.rankManager.isStaff(p) && !p.equals(player)) {
                p.sendMessage(ChatColor.GRAY + "Global Staff Mail was cleared by " + player.getName());
            }
        }
    }

    // --- 2. PERSONAL ALERTS (Warnings/Direct Messages - Your Old Code) ---
    public void sendStaffMail(OfflinePlayer target, String type, String message) {
        File f = plugin.getPlayerDataFile(target.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);

        List<String> mail = data.getStringList("staff_mail");
        mail.add(ChatColor.RED + "[" + type + "] " + ChatColor.WHITE + message);

        data.set("staff_mail", mail);
        try { data.save(f); } catch (IOException e) {}

        if (target.isOnline()) {
            ((Player)target).sendMessage(ChatColor.GOLD + "You have new Personal Staff Mail! Type /staffmail read");
            SoundUtils.playSound(((Player)target), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
    }

    public void readPersonalMail(Player player) {
        File f = plugin.getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);

        List<String> mail = data.getStringList("staff_mail");
        if (mail.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No new personal staff mail.");
            return;
        }

        player.sendMessage(ChatColor.DARK_RED + "--- PERSONAL STAFF ALERTS ---");
        for (String s : mail) {
            player.sendMessage(s);
        }

        // Clear after reading
        data.set("staff_mail", null);
        try { data.save(f); } catch (IOException e) {}
    }

    // --- 3. NOTIFY ON JOIN (Checks Both) ---
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.rankManager.isStaff(player)) {
            boolean hasNotification = false;

            // Check Personal Mail (Old Code)
            File f = plugin.getPlayerDataFile(player.getUniqueId());
            FileConfiguration data = YamlConfiguration.loadConfiguration(f);
            if (data.contains("staff_mail") && !data.getStringList("staff_mail").isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "ATTENTION: " + ChatColor.YELLOW + "You have unread Personal Alerts.");
                hasNotification = true;
            }

            // Check Global Mail (New Code)
            List<String> globalMail = staffMailConfig.getStringList("inbox");
            if (!globalMail.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "STAFF MAIL: " + ChatColor.YELLOW + "There are " + globalMail.size() + " unread global messages.");
                hasNotification = true;
            }

            if (hasNotification) {
                player.sendMessage(ChatColor.GRAY + "Type /staffmail read to view them.");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
            }
        }
    }
}