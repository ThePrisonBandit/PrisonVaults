package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class JobScheduleManager {

    private final PrisonVaults plugin;

    // STATE TRACKING
    private boolean wasOpen;
    private int lastDayIndex = -1;
    private String mainWorldName; // Stores the config world name

    // CONFIGURATION: Work Hours (in ticks)
    private static final long WORK_START_TICK = 1000;  // 7:00 AM
    private static final long WORK_END_TICK = 13000;   // 7:00 PM

    public JobScheduleManager(PrisonVaults plugin) {
        this.plugin = plugin;

        // Load world name from config (Default: "world")
        this.mainWorldName = plugin.getConfig().getString("job-world-name", "world");

        // FIX CRASH: Do NOT calculate time here. Wait for the server to fully load.
        this.wasOpen = false;
    }

    /**
     * Updates the world name dynamically (called by /pvconfig).
     */
    public void setWorldName(String newName) {
        this.mainWorldName = newName;
    }

    /**
     * Called by ScoreboardManager every second.
     * FIXES RED LINES: This is the method your ScoreboardManager is looking for.
     */
    public void checkTime() {
        // FIX CRASH: If world isn't loaded yet, stop here.
        if (getMainWorld() == null) return;

        // 1. Check for Day Change (Monday -> Tuesday)
        int currentDayIndex = getDayIndex();
        if (currentDayIndex != lastDayIndex) {
            lastDayIndex = currentDayIndex;

            // Optional: Announce the new day
            // Bukkit.broadcastMessage(ChatColor.YELLOW + "It is now " + getCurrentDayName() + "!");
        }

        // 2. Check for Shift Change (Open/Closed)
        boolean currentlyOpen = isJobOpen();

        // Detect if status changed
        if (currentlyOpen != wasOpen) {
            wasOpen = currentlyOpen;

            if (currentlyOpen) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "⚒ " + ChatColor.BOLD + "JOBS OPEN " + ChatColor.GREEN + "Work shifts have begun!");
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "☾ " + ChatColor.BOLD + "JOBS CLOSED " + ChatColor.RED + "The work day is over.");
            }
        }
    }

    public String getScoreboardString() {
        if (getMainWorld() == null) return "Loading...";

        String dayName = getCurrentDayName();
        boolean isOpen = isJobOpen();

        String status;
        if (isOpen) {
            status = ChatColor.GREEN + "Open";
        } else {
            status = ChatColor.RED + "Closed";
        }

        return dayName + " (" + status + ChatColor.RESET + ")";
    }

    public boolean isJobOpen() {
        if (!isWorkDay()) return false;
        return isWorkingHours();
    }

    private boolean isWorkingHours() {
        World world = getMainWorld();
        if (world == null) return false;

        long time = world.getTime();
        return time >= WORK_START_TICK && time < WORK_END_TICK;
    }

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

    public boolean isWorkDay() {
        return getDayIndex() < 5;
    }

    private int getDayIndex() {
        World world = getMainWorld();
        if (world == null) return 0;

        long fullTime = world.getFullTime();
        long totalDays = fullTime / 24000L;
        return (int) (totalDays % 7);
    }

    /**
     * Safe world getter. Prevents "Index 0 out of bounds" crash.
     */
    private World getMainWorld() {
        // 1. Try config name
        World w = Bukkit.getWorld(mainWorldName);
        if (w != null) return w;

        // 2. Fallback safely
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0);
        }

        // 3. Server is still starting up
        return null;
    }

    public void sendClosedMessage(Player player) {
        if (!isWorkDay()) {
            player.sendMessage(ChatColor.RED + "It's the weekend (" + getCurrentDayName() + ")!");
            player.sendMessage(ChatColor.YELLOW + "Jobs are closed until Monday.");
        } else {
            player.sendMessage(ChatColor.RED + "The job center is closed for the night.");
            player.sendMessage(ChatColor.YELLOW + "Work shifts are from sunrise to sunset.");
        }
    }
}