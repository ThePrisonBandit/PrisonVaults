package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Gang;
import me.theprisonbandit.prisonVaults.gangs.Mail;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MailCommands implements CommandExecutor {

    private final PrisonVaults plugin;

    public MailCommands(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // --- /inbox (Old Code) ---
        if (label.equalsIgnoreCase("inbox")) {
            openInbox(player);
            return true;
        }

        // --- /mail (Merged) ---
        if (label.equalsIgnoreCase("mail")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /mail <player|gang|staff> <message>");
                return true;
            }

            String targetName = args[0];
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // 1. MAIL STAFF (New)
            if (targetName.equalsIgnoreCase("staff")) {
                plugin.staffMailManager.sendToAllStaff(player.getName(), msg);
                player.sendMessage(ChatColor.GREEN + "Message sent to the Staff Team.");
                SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                return true;
            }

            // 2. MAIL OWN GANG (Old Code - literal "gang")
            if (targetName.equalsIgnoreCase("gang")) {
                plugin.mailManager.sendGangMail(player, msg);
                // Old method sends the confirmation message inside itself usually
                return true;
            }

            // 3. MAIL SPECIFIC GANG (New)
            if (plugin.gangManager.gangExists(targetName)) {
                Gang g = plugin.gangManager.getGang(targetName);
                plugin.mailManager.sendMailToGang(g, player.getName(), msg);
                player.sendMessage(ChatColor.GREEN + "Mail sent to Gang " + g.getName());
                return true;
            }

            // 4. MAIL PLAYER (Merged Online + Offline)
            Player t = Bukkit.getPlayer(targetName);
            if (t != null) {
                // Online Player (Old Code)
                plugin.mailManager.sendMail(player.getName(), t.getUniqueId(), msg);
                player.sendMessage(ChatColor.GREEN + "Mail sent to " + t.getName());
            } else {
                // Offline Player (New)
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                if (off.hasPlayedBefore()) {
                    plugin.mailManager.sendMail(player.getName(), off.getUniqueId(), msg);
                    player.sendMessage(ChatColor.GREEN + "Mail sent to " + off.getName() + " (Offline)");
                } else {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                }
            }
            return true;
        }
        return true;
    }

    // --- OLD CODE HELPERS ---
    private void openInbox(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "Inbox");
        List<Mail> mails = plugin.mailManager.getInbox(player.getUniqueId());

        // Add "Clear Inbox" button
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta cm = clear.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Clear All Mail");
        clear.setItemMeta(cm);
        inv.setItem(53, clear);

        int slot = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
        for (Mail m : mails) {
            if (slot >= 53) break;
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "From: " + m.getSender());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + sdf.format(new Date(m.getTimestamp())),
                    ChatColor.WHITE + m.getMessage()
            ));
            paper.setItemMeta(meta);
            inv.setItem(slot++, paper);
        }
        player.openInventory(inv);
    }
}