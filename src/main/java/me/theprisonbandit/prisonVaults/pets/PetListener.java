package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    // --- 1. MASTER DAMAGE HANDLER (Fixes Friendly Fire & Projectile Errors) ---
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();
        Player damagerPlayer = null;

        // A. Resolve the real attacker (Player vs Projectile)
        if (attacker instanceof Player) {
            damagerPlayer = (Player) attacker;
        } else if (attacker instanceof Projectile) {
            Projectile proj = (Projectile) attacker;
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player) {
                damagerPlayer = (Player) source;
            } else if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) {
                // If a PET shot this projectile, handle pet logic
                Entity shooterPet = (Entity) source;

                // Pet Logic: Prevent Pet Friendly Fire
                if (victim.equals(shooterPet)) { event.setCancelled(true); return; } // Pet hit itself
                Player owner = plugin.petManager.getPetOwner(shooterPet);
                if (owner != null && victim.equals(owner)) { event.setCancelled(true); return; } // Pet hit owner

                // Wither Effect
                if (proj instanceof WitherSkull && victim instanceof LivingEntity) {
                    ((LivingEntity) victim).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                }
                // Fireball Safety
                if (proj instanceof Fireball) {
                    victim.setFireTicks(0);
                }
                return; // Done with pet projectile logic
            }
        }

        // B. PLAYER ATTACKING PET (The Fix You Asked For)
        if (damagerPlayer != null && plugin.petManager.isPet(victim)) {
            // Check if this player owns this pet
            Player owner = plugin.petManager.getPetOwner(victim);
            if (owner != null && owner.getUniqueId().equals(damagerPlayer.getUniqueId())) {
                event.setCancelled(true); // BLOCK DAMAGE
                return;
            }
        }

        // C. TRIGGER PET ATTACK (Combat AI)
        if (damagerPlayer != null && victim instanceof LivingEntity) {
            triggerPetAttack(damagerPlayer, victim);
        }
        if (victim instanceof Player && attacker instanceof LivingEntity) {
            triggerPetAttack((Player) victim, attacker);
        }
    }

    // --- 2. SAFETY: NO EXPLOSIONS / FIRE ---
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

    // --- 3. NAMETAG UPDATE ---
    @EventHandler
    public void onPetDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity && plugin.petManager.isPet(event.getEntity())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (event.getEntity().isValid()) plugin.petManager.updatePetNametag((LivingEntity) event.getEntity());
            });
        }
    }

    // --- 4. DEATH HANDLING ---
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

    // --- HELPER: Trigger Pet Attack ---
    private void triggerPetAttack(Player owner, Entity victim) {
        if (!(victim instanceof LivingEntity)) return;
        if (victim.equals(owner)) return;

        Entity pet = plugin.petManager.getPet(owner);
        if (pet == null || victim.equals(pet)) return;

        boolean shouldAttack = false;
        if (victim instanceof Player || plugin.petManager.isPet(victim)) shouldAttack = true; // PvP
        if (victim instanceof Monster) shouldAttack = true; // PvE

        if (shouldAttack) {
            plugin.petManager.attackTarget(owner, (LivingEntity) victim);
        }
    }

    // --- GUI & QUIT ---
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.petManager.removePetFully(event.getPlayer());
    }

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

        if (item.getType() == Material.BARRIER && name.contains("Despawn")) {
            plugin.petManager.removePetFully(p);
            p.sendMessage(ChatColor.YELLOW + "Pet despawned.");
            p.closeInventory();
            return;
        }

        for (PetType type : PetType.values()) {
            if (type.display.equals(name)) {
                if (title.equals("Pet Shop")) {
                    if (plugin.petManager.hasPet(p, type)) p.sendMessage(ChatColor.RED + "Owned!");
                    else { plugin.petManager.buyPet(p, type); p.closeInventory(); }
                } else {
                    plugin.petManager.spawnPet(p, type);
                    p.closeInventory();
                }
                break;
            }
        }
    }
}