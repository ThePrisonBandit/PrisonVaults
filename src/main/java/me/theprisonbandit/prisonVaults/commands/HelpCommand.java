package me.theprisonbandit.prisonVaults.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements CommandExecutor {

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
        addCmd("/job <join|quit|shop|info|promote>", "Join or quit jobs");
        addCmd("/pvshop <messhall|smithy>", "Open the job ingredient shops");

        // --- GANGS ---
        addCmd("/gang", "Main gang command");
        addCmd("/gangs", "List all gangs on the server");

        // --- PROFILES ---
        addCmd("/myprofile", "View your stats");
        addCmd("/whois <player>", "View another player's stats");
        addCmd("/setbio <text>", "Set your profile biography");

        // --- MAIL ---
        addCmd("/mail <gang> or <player>", "Send mail");
        addCmd("/inbox", "Check mail");

        // --- KITS ---
        addCmd("/kit <name>", "Get a kit");
        addCmd("/kits", "List kits");
        addCmd("/buykit", "Open kit shop");
        addCmd("/createkit <name> <color> <tool tier> <armor tier> <enchant level> <price>", "Admin create kit");

        // --- ADMIN / MISC ---
        addCmd("/prisonvaults reload", "Reload the plugin configuration");
        addCmd("/pvconfig set <rob|pickpocket> <decimal>, example: 0.5 sets it to 50% success chance.", "Open the prisonvaults config file and edit it");
        addCmd("/pvscoreboard <show|hide>", "Toggle the prisonvaults scoreboard");
        addCmd("/pvhelp [page]", "List all plugin commands");
    }

    private void addCmd(String syntax, String desc) {
        helpLines.add(ChatColor.YELLOW + syntax + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + desc);
    }
}