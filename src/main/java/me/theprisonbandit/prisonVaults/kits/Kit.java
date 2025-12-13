package me.theprisonbandit.prisonVaults.kits;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class Kit {
    private final String name;
    private final List<ItemStack> items;
    private final ItemStack icon;
    private double price;
    private final String permission;

    public Kit(String name, List<ItemStack> items, ItemStack icon, double price, String permission) {
        this.name = name;
        this.items = items;
        this.icon = icon;
        this.price = price;
        this.permission = permission;
    }

    public String getName() { return name; }
    public List<ItemStack> getItems() { return items; }
    public ItemStack getIcon() { return icon; }
    public double getPrice() { return price; }
    public String getPermission() { return permission; }
}