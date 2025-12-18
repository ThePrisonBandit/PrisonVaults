package me.theprisonbandit.prisonVaults.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpCommand implements TabExecutor {

    private final List<String> helpLines = new ArrayList<>();
    private static final int LINES_PER_PAGE = 8;

    public HelpCommand() {
        loadHelp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        int totalPages = (int) Math.ceil((double) helpLines.size() / LINES_PER_PAGE);

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        sender.sendMessage(ChatColor.DARK_GRAY + "----------------[" + ChatColor.GOLD + "PrisonVaults Help (" + page + "/" + totalPages + ")" + ChatColor.DARK_GRAY + "]----------------");

        int startIndex = (page - 1) * LINES_PER_PAGE;
        int endIndex = Math.min(startIndex + LINES_PER_PAGE, helpLines.size());

        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(helpLines.get(i));
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "Type " + ChatColor.YELLOW + "/pvhelp " + (page + 1) + ChatColor.GRAY + " for the next page.");
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------------");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            int totalPages = (int) Math.ceil((double) helpLines.size() / LINES_PER_PAGE);
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= totalPages; i++) {
                pages.add(String.valueOf(i));
            }
            return StringUtil.copyPartialMatches(args[0], pages, new ArrayList<>());
        }
        return Collections.emptyList();
    }

    private void loadHelp() {
        // --- CORE & ECONOMY ---
        addCmd("/pv <number>", "Open a personal vault");
        addCmd("/sell (or /sell all)", "Sell items to the server");
        addCmd("/balance (or /bal, /money)", "Check your PrisonBucks");
        addCmd("/pay <player> <amount>", "Send money to others");
        addCmd("/rankup", "Rank up to the next level");
        addCmd("/addmoney <player> <amount>", "Give money to a player");
        addCmd("/rob <player>", "Attempt to rob a player (Very low chance!)");
        addCmd("/colorify <preset>", "Set a gradient name color");

        // --- JOBS & SHOPS ---
        addCmd("/job <join|quit|info|promote>", "Join or quit jobs");
        addCmd("/pvshop <buy|sell> <messhall|smithy>", "Open the job ingredient shops");

        // --- GANGS ---
        addCmd("/gang", "Main gang command");
        addCmd("/gangs", "List all gangs on the server");
        addCmd("/gangchat (or /gc)", "Toggle or send gang chat");

        // --- PROFILES ---
        addCmd("/myprofile", "View your stats");
        addCmd("/whois <player>", "View another player's stats");
        addCmd("/setbio <text>", "Set your profile biography");

        // --- MAIL ---
        addCmd("/mail <gang> or <player>", "Send mail");
        addCmd("/inbox", "Check mail");

        // --- PETS ---
        addCmd("/pets", "Open your pet collection");
        addCmd("/petshop", "Buy new pets");

        // --- KITS ---
        addCmd("/kit <name>", "Get a kit");
        addCmd("/kits", "List kits");
        addCmd("/buykit", "Open kit shop");
        addCmd("/createkit <name> <color> <tool tier> <armor tier> <enchant level> <price>", "Admin create kit");

        // --- ADMIN / MISC ---
        addCmd("/prisonvaults reload", "Reload the plugin configuration");
        addCmd("/pvconfig set <rob|pickpocket> <decimal> | /pvconfig setworld <name>", "Open the prisonvaults config file and edit it");
        addCmd("/pvscoreboard <show|hide>", "Toggle the prisonvaults scoreboard");
        addCmd("/pvhelp [page]", "List all plugin commands");
        addCmd("/pvcompass", "Get a compass to find your way.");
        addCmd("/pvinfo", "View plugin information");
        addCmd("/pvperm <group|user> ...", "Manage permissions (OP Only)");
        addCmd("/pvannounce <message>", "Make a server-wide announcement (OP Only)");
        addCmd("/resetcooldown", "Reset the server cooldowns (OP Only)");

        // --- STAFF MANAGEMENT ---
        addCmd("/staff [player]", "Open the Staff Management GUI");
        addCmd("/setstaff <player> <rank>", "Set a player's staff rank (OP Only)");
        addCmd("/staffmail <read|clear>", "Check staff notifications/mail");
        addCmd("/staffchat (or /sc)", "Toggle or send staff chat");

        // --- PUNISHMENTS ---
        addCmd("/pvkick <player> [reason]", "Kick a player (Owners protected)");
        addCmd("/pvban <player> [reason]", "Ban a player (Owners protected)");
        addCmd("/pvwarn <player> <reason>", "Warn a player");
        addCmd("/pvpardon <player>", "Unban a player");
    }

    private void addCmd(String syntax, String desc) {
        helpLines.add(ChatColor.YELLOW + syntax + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + desc);
    }
}