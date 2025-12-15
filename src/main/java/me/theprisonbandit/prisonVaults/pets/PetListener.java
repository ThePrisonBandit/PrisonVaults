package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent; // IMPORT ADDED
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class PetListener implements Listener {

    private final PrisonVaults plugin;

    public PetListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- CHAT LISTENER FOR RENAMING ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.petManager.renamingPlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Don't show in chat

            String message = event.getMessage();
            PetType type = plugin.petManager.renamingPlayers.remove(player.getUniqueId()); // Remove and get type

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Renaming cancelled.");
            } else {
                plugin.petManager.setPetNickname(player, type, message);
            }

            // Re-open the GUI so they can see their pet
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.petManager.openSelector(player));
        }
    }

    // --- EXISTING EVENT HANDLERS BELOW ---

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (plugin.petManager.isPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(EntityBlockFormEvent event) {
        if (event.getEntity() instanceof Snowman && plugin.petManager.isPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();
        Player damagerPlayer = null;

        if (attacker instanceof Player) {
            damagerPlayer = (Player) attacker;
        } else if (attacker instanceof Projectile) {
            Projectile proj = (Projectile) attacker;
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player) {
                damagerPlayer = (Player) source;
            } else if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) {
                Entity shooterPet = (Entity) source;
                if (victim.equals(shooterPet)) { event.setCancelled(true); return; }
                Player owner = plugin.petManager.getPetOwner(shooterPet);
                if (owner != null && victim.equals(owner)) { event.setCancelled(true); return; }

                if (proj instanceof WitherSkull && victim instanceof LivingEntity) {
                    ((LivingEntity) victim).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                }
                if (proj instanceof Fireball) {
                    victim.setFireTicks(0);
                }
                return;
            }
        }

        if (damagerPlayer != null && plugin.petManager.isPet(victim)) {
            Player owner = plugin.petManager.getPetOwner(victim);
            if (owner != null && owner.getUniqueId().equals(damagerPlayer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (damagerPlayer != null && victim instanceof LivingEntity) {
            triggerPetAttack(damagerPlayer, victim);
        }
        if (victim instanceof Player && attacker instanceof LivingEntity) {
            triggerPetAttack((Player) victim, attacker);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Projectile) {
            ProjectileSource source = ((Projectile) e).getShooter();
            if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() != null && plugin.petManager.isPet(event.getIgnitingEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPetDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity && plugin.petManager.isPet(event.getEntity())) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.DROWNING ||
                    cause == EntityDamageEvent.DamageCause.FALL ||
                    cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL ||
                    cause == EntityDamageEvent.DamageCause.CRAMMING ||
                    cause == EntityDamageEvent.DamageCause.CONTACT) {
                event.setCancelled(true);
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (event.getEntity().isValid()) plugin.petManager.updatePetNametag((LivingEntity) event.getEntity());
            });
        }
    }

    @EventHandler
    public void onPetDeath(EntityDeathEvent event) {
        Entity deadEntity = event.getEntity();
        Player owner = plugin.petManager.getPetOwner(deadEntity);
        if (owner != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.petManager.handlePetDeath(owner);
        }
    }

    private void triggerPetAttack(Player owner, Entity victim) {
        if (!(victim instanceof LivingEntity)) return;
        if (victim.equals(owner)) return;
        Entity pet = plugin.petManager.getPet(owner);
        if (pet == null || victim.equals(pet)) return;

        boolean shouldAttack = false;
        if (victim instanceof Player || plugin.petManager.isPet(victim)) shouldAttack = true;
        if (victim instanceof Monster) shouldAttack = true;

        if (shouldAttack) {
            plugin.petManager.attackTarget(owner, (LivingEntity) victim);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.petManager.removePetFully(event.getPlayer());
    }

    // --- GUI HANDLER ---
    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("Pet Shop") && !title.equals("My Pets")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (!item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (name.contains("Close")) {
            p.closeInventory();
            return;
        }

        if (name.contains("Despawn")) {
            plugin.petManager.removePetFully(p);
            p.sendMessage(ChatColor.YELLOW + "Pet despawned.");
            return;
        }

        for (PetType type : PetType.values()) {
            if (type.display.equals(name)) {
                if (title.equals("Pet Shop")) {
                    if (plugin.petManager.hasPet(p, type)) {
                        p.sendMessage(ChatColor.RED + "You already own this pet!");
                    } else {
                        plugin.petManager.buyPet(p, type);
                        plugin.petManager.openShop(p);
                    }
                } else {
                    // --- CHANGED LOGIC FOR "MY PETS" ---
                    if (event.isRightClick()) {
                        // RENAME LOGIC
                        p.closeInventory();
                        plugin.petManager.renamingPlayers.put(p.getUniqueId(), type);
                        p.sendMessage(ChatColor.GREEN + "Type a new nickname for " + type.display + " in chat.");
                        p.sendMessage(ChatColor.GRAY + "Type 'cancel' to stop.");
                    } else {
                        // SPAWN LOGIC (Left Click)
                        plugin.petManager.spawnPet(p, type);
                    }
                }
                break;
            }
        }
    }
}