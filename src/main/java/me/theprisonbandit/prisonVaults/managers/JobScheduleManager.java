package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class JobScheduleManager {

    private final PrisonVaults plugin;

    // 24000 ticks = 1 Minecraft Day
    // We treat day 0 as Monday, 1 as Tuesday, etc.
    // Modulo 7 gives us a repeatable week cycle.

    public JobScheduleManager(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the current day name (e.g., "Monday", "Saturday")
     */
    public String getCurrentDayName() {
        int dayIndex = getDayIndex();
        switch (dayIndex) {
            case 0: return "Monday";
            case 1: return "Tuesday";
            case 2: return "Wednesday";
            case 3: return "Thursday";
            case 4: return "Friday";
            case 5: return "Saturday";
            case 6: return "Sunday";
            default: return "Unknown";
        }
    }

    /**
     * Checks if the current day is a workday (Mon-Fri)
     */
    public boolean isWorkDay() {
        int day = getDayIndex();
        // 0-4 are Mon-Fri (Work)
        // 5-6 are Sat-Sun (Off)
        return day < 5;
    }

    /**
     * Calculates the day index (0-6) based on the main world time.
     */
    private int getDayIndex() {
        // Get the main world (usually the first one loaded)
        World world = Bukkit.getWorlds().get(0);
        if (world == null) return 0;

        long fullTime = world.getFullTime();
        long totalDays = fullTime / 24000L;

        // Modulo 7 gives us 0 through 6
        return (int) (totalDays % 7);
    }

    /**
     * Helper to send a rejection message if it's the weekend.
     */
    public void sendWeekendMessage(Player player) {
        player.sendMessage(ChatColor.RED + "It's the weekend (" + getCurrentDayName() + ")!");
        player.sendMessage(ChatColor.YELLOW + "Take a break! Jobs are closed until Monday.");
    }
}