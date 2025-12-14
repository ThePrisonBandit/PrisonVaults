package me.theprisonbandit.prisonVaults.managers;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PermissionManager {

    private final PrisonVaults plugin;
    private File file;
    private FileConfiguration config;

    // Cache: UUID -> Attachment
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PermissionManager(PrisonVaults plugin) {
        this.plugin = plugin;
        loadFile();
    }

    // --- CORE: SETUP PLAYER ---
    public void setupPlayer(Player player) {
        // 1. Clear old attachment if exists
        removeAttachment(player);

        // 2. Create new attachment
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        // 3. Calculate Permissions
        Set<String> perms = calculatePlayerPermissions(player);

        // 4. Apply
        for (String p : perms) {
            // Handle negation (e.g. "-prisonvaults.ban")
            if (p.startsWith("-")) {
                attachment.setPermission(p.substring(1), false);
            } else {
                attachment.setPermission(p, true);
            }
        }

        // 5. Re-calculate OP status if necessary (Optional, usually handled by server)
    }

    public void removeAttachment(Player player) {
        if (attachments.containsKey(player.getUniqueId())) {
            player.removeAttachment(attachments.get(player.getUniqueId()));
            attachments.remove(player.getUniqueId());
        }
    }

    // --- CALCULATION LOGIC ---
    private Set<String> calculatePlayerPermissions(Player player) {
        Set<String> permissions = new HashSet<>();
        String uuid = player.getUniqueId().toString();

        // 1. Get User's Group (Default to 'default')
        String group = config.getString("users." + uuid + ".group", "default");

        // 2. Add Group Permissions (Recursive for inheritance)
        permissions.addAll(getGroupPermissions(group));

        // 3. Add User-Specific Permissions
        List<String> userPerms = config.getStringList("users." + uuid + ".permissions");
        permissions.addAll(userPerms);

        return permissions;
    }

    private Set<String> getGroupPermissions(String groupName) {
        Set<String> perms = new HashSet<>();
        if (!config.contains("groups." + groupName)) return perms;

        // 1. Own Permissions
        List<String> nodes = config.getStringList("groups." + groupName + ".permissions");
        perms.addAll(nodes);

        // 2. Inheritance
        List<String> parents = config.getStringList("groups." + groupName + ".inheritance");
        for (String parent : parents) {
            perms.addAll(getGroupPermissions(parent));
        }

        return perms;
    }

    // --- MANAGEMENT METHODS ---
    public void setGroup(UUID uuid, String group) {
        config.set("users." + uuid + ".group", group);
        save();
        refreshPlayer(uuid);
    }

    public void addPermissionToGroup(String group, String permission) {
        List<String> perms = config.getStringList("groups." + group + ".permissions");
        if (!perms.contains(permission)) {
            perms.add(permission);
            config.set("groups." + group + ".permissions", perms);
            save();
            refreshAll();
        }
    }

    public void removePermissionFromGroup(String group, String permission) {
        List<String> perms = config.getStringList("groups." + group + ".permissions");
        perms.remove(permission);
        config.set("groups." + group + ".permissions", perms);
        save();
        refreshAll();
    }

    public void createGroup(String group) {
        if (!config.contains("groups." + group)) {
            config.createSection("groups." + group);
            config.set("groups." + group + ".permissions", new ArrayList<>());
            config.set("groups." + group + ".inheritance", new ArrayList<>());
            save();
        }
    }

    public Set<String> getGroups() {
        if (config.getConfigurationSection("groups") == null) return new HashSet<>();
        return config.getConfigurationSection("groups").getKeys(false);
    }

    private void refreshPlayer(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) setupPlayer(p);
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            setupPlayer(p);
        }
    }

    // --- FILE IO ---
    private void loadFile() {
        file = new File(plugin.getDataFolder(), "permissions.yml");
        if (!file.exists()) {
            plugin.saveResource("permissions.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}