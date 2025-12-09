package me.theprisonbandit.prisonVaults.commands;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.gangs.Mail;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

        // /inbox
        if (label.equalsIgnoreCase("inbox")) {
            openInbox(player);
            return true;
        }

        // /mail
        if (label.equalsIgnoreCase("mail")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /mail <player|gang> <message>");
                return true;
            }

            String target = args[0];
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (target.equalsIgnoreCase("gang")) {
                plugin.mailManager.sendGangMail(player, msg);
            } else {
                Player t = Bukkit.getPlayer(target);
                if (t == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                plugin.mailManager.sendMail(player.getName(), t.getUniqueId(), msg);
                player.sendMessage(ChatColor.GREEN + "Mail sent!");
            }
            return true;
        }
        return true;
    }

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