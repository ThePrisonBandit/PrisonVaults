package me.theprisonbandit.prisonVaults.listeners;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot; // Import this!
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class KitAbilityListener implements Listener {

    private final PrisonVaults plugin;

    public KitAbilityListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- SHIV MECHANICS (Right Click Shank) ---
    @EventHandler
    public void onShank(PlayerInteractEntityEvent event) {
        // FIX: Verify this is the Main Hand event to prevent double firing
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand != null && hand.getType().name().contains("SWORD")) {
            if (hand.hasItemMeta() && hand.getItemMeta().getDisplayName().contains("Shiv")) {
                if (event.getRightClicked() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getRightClicked();

                    // Shank Logic
                    // 4 Hearts = 8 Damage
                    target.damage(8.0, player);
                    player.swingMainHand();
                    player.sendMessage(ChatColor.RED + "You shanked " + target.getName() + "!");

                    // Bleed Effect (1 damage every second for 5 seconds)
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (ticks >= 5 || target.isDead()) {
                                this.cancel();
                                return;
                            }
                            target.damage(1.0); // Bleed damage
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 2f);
                            ticks++;
                        }
                    }.runTaskTimer(plugin, 20L, 20L);
                }
            }
        }
    }

    // --- CLEAVER MECHANICS (Always Crit) ---
    @EventHandler
    public void onCleaverHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack hand = player.getInventory().getItemInMainHand();

            if (hand != null && hand.getType().name().contains("AXE")) {
                if (hand.hasItemMeta() && hand.getItemMeta().getDisplayName().contains("Cleaver")) {
                    // Multiply damage by 1.5 (Standard crit multiplier)
                    event.setDamage(event.getDamage() * 1.5);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                }
            }
        }
    }

    // --- DIGGING SHOVEL MECHANICS (3x3 Mine) ---
    @EventHandler
    public void onDig(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Block center = event.getBlock();

        if (hand != null && hand.getType().name().contains("SHOVEL")) {
            if (hand.hasItemMeta() && hand.getItemMeta().getDisplayName().contains("Digging Shovel")) {
                // Simple 3x3x1 cube around the block.
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue; // Skip center

                            Block rel = center.getRelative(x, y, z);
                            if (rel.getType() != Material.BEDROCK && rel.getType() != Material.AIR) {
                                String type = rel.getType().name();
                                if (type.contains("DIRT") || type.contains("SAND") || type.contains("GRAVEL") || type.contains("GRASS")) {
                                    rel.breakNaturally(hand);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ABILITY ITEMS (MedKit & SwiftFeet) ---
    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        // FIX: Also good to check hand here to prevent using two items at once
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        Player player = event.getPlayer();
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // MedKit
        if (name.equals("MedKit")) {
            double health = player.getHealth();
            if (health >= player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                return; // Full health
            }
            double newHealth = Math.min(health + 8.0, 20.0); // Heal 4 hearts
            player.setHealth(newHealth);
            player.sendMessage(ChatColor.GREEN + "Used MedKit!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

            item.setAmount(item.getAmount() - 1); // Consume
        }

        // SwiftFeet
        if (name.equals("SwiftFeet")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1)); // Speed II for 10s
            player.sendMessage(ChatColor.AQUA + "SwiftFeet activated!");
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1f);

            item.setAmount(item.getAmount() - 1); // Consume
        }
    }
}