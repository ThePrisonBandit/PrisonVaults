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
import org.bukkit.command.TabCompleter; // Added
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil; // Added

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MailCommands implements CommandExecutor, TabCompleter {

    private final PrisonVaults plugin;

    public MailCommands(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("inbox")) {
            openInbox(player);
            return true;
        }

        if (label.equalsIgnoreCase("mail")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /mail <player|gang|staff> <message>");
                return true;
            }

            String targetName = args[0];
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (targetName.equalsIgnoreCase("staff")) {
                plugin.staffMailManager.sendToAllStaff(player.getName(), msg);
                player.sendMessage(ChatColor.GREEN + "Message sent to the Staff Team.");
                SoundUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                return true;
            }

            if (targetName.equalsIgnoreCase("gang")) {
                plugin.mailManager.sendGangMail(player, msg);
                return true;
            }

            if (plugin.gangManager.gangExists(targetName)) {
                Gang g = plugin.gangManager.getGang(targetName);
                plugin.mailManager.sendMailToGang(g, player.getName(), msg);
                player.sendMessage(ChatColor.GREEN + "Mail sent to Gang " + g.getName());
                return true;
            }

            Player t = Bukkit.getPlayer(targetName);
            if (t != null) {
                plugin.mailManager.sendMail(player.getName(), t.getUniqueId(), msg);
                player.sendMessage(ChatColor.GREEN + "Mail sent to " + t.getName());
            } else {
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

    private void openInbox(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "Inbox");
        List<Mail> mails = plugin.mailManager.getInbox(player.getUniqueId());

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("mail")) {
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                options.add("staff");
                options.add("gang");

                // Add online players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    options.add(p.getName());
                }

                // Add gang names
                for (Gang g : plugin.gangManager.getAllGangs()) {
                    options.add(g.getName());
                }

                return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }
}