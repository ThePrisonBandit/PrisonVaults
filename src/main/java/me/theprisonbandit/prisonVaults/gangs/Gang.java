package me.theprisonbandit.prisonVaults.gangs;

import org.bukkit.ChatColor;
import java.util.*;

public class Gang {
    private final UUID id;
    private String name;
    private String tag; // The Acronym (e.g. TDC)
    private String description;
    private String color; // e.g. "&c" or "RED"
    private UUID owner;
    private final Map<UUID, Rank> members = new HashMap<>();
    private final List<UUID> invitedPlayers = new ArrayList<>();

    public Gang(UUID id, String name, String tag, UUID owner) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.description = "None.";
        this.color = "&f"; // Default white
        this.members.put(owner, Rank.LEADER);
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Map<UUID, Rank> getMembers() { return members; }
    public List<UUID> getInvitedPlayers() { return invitedPlayers; }

    public String getFormattedName() {
        return ChatColor.translateAlternateColorCodes('&', color + "[" + tag + "] " + name);
    }
}