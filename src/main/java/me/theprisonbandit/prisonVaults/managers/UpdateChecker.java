package me.theprisonbandit.prisonVaults.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final PrisonVaults plugin;
    private final int projectID;
    private final String apiKey;
    private String latestVersion;

    public UpdateChecker(PrisonVaults plugin) {
        this.plugin = plugin;
        // Load settings from config, defaulting to the values you provided
        this.projectID = plugin.getConfig().getInt("update-checker.curseforge-project-id", 1401362);
        this.apiKey = plugin.getConfig().getString("update-checker.curseforge-api-key", "29213f04-3046-4b44-ba05-d5d1798d57f8");

        if (projectID == 0 || apiKey.isEmpty() || apiKey.contains("YOUR_API_KEY")) {
            plugin.getLogger().warning("Update Checker disabled: Invalid Config.");
            return;
        }

        checkForUpdates();
    }

    public void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // CurseForge API: Get Files for specific Mod ID
                    // We check specifically for Minecraft 1.21 versions
                    URL url = new URL("https://api.curseforge.com/v1/mods/" + projectID + "/files?gameVersion=1.21");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    // IMPORTANT: We use the key you generated in the screenshot here
                    connection.setRequestProperty("x-api-key", apiKey);
                    connection.setRequestProperty("Accept", "application/json");

                    if (connection.getResponseCode() == 200) {
                        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                        JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
                        JsonArray files = response.getAsJsonArray("data");

                        if (files.size() > 0) {
                            // The API usually returns the newest files first.
                            // We get the first file in the list.
                            JsonObject latestFile = files.get(0).getAsJsonObject();
                            String fileName = latestFile.get("fileName").getAsString();

                            // Remove ".jar" to get the version string (e.g., "PrisonVaults-1.0.10")
                            String remoteVersion = fileName.replace(".jar", "");
                            String currentVersion = plugin.getDescription().getVersion();

                            // Simple check: If the file name doesn't contain our current version, it's likely new.
                            if (!fileName.contains(currentVersion)) {
                                latestVersion = remoteVersion;
                                plugin.getLogger().info("Found a new version on CurseForge: " + latestVersion);
                            }
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    plugin.getLogger().warning("Update Check failed: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // If we found an update, notify OPs when they join
        if (latestVersion != null && event.getPlayer().isOp()) {
            Player p = event.getPlayer();
            p.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");
            p.sendMessage(ChatColor.GOLD + " PrisonVaults Update Available!");
            p.sendMessage(ChatColor.GRAY + " Current: " + ChatColor.RED + plugin.getDescription().getVersion());
            p.sendMessage(ChatColor.GRAY + " New: " + ChatColor.GREEN + latestVersion);
            p.sendMessage(ChatColor.YELLOW + " Check CurseForge to download.");
            p.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");
        }
    }
}