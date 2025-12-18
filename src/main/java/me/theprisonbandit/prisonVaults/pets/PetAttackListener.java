package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PetAttackListener implements Listener {

    private final PrisonVaults plugin;

    public PetAttackListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- FIX: STRICT COMBAT HIERARCHY ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetDamageEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Handle Projectiles (e.g. Blaze Fireball, Skeleton Arrow)
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Entity) {
                damager = (Entity) source;
            }
        }

        // Check if the Attacker is a Pet
        if (plugin.petManager.isPet(damager)) {
            Player owner = plugin.petManager.getPetOwner((LivingEntity) damager);
            if (owner == null) return;

            // 1. MASTER SAFETY SWITCH: Passive Mode
            // If Passive, NO damage is allowed unless explicitly commanded by the player.
            CombatHandlingManager.AggressionMode agg = plugin.combatHandlingManager.getAggression(owner);
            if (agg == CombatHandlingManager.AggressionMode.PASSIVE) {
                // If the pet is NOT explicitly commanded to attack this target, Cancel.
                if (!plugin.petManager.isExplicitlyTargeting(damager.getUniqueId(), victim.getUniqueId())) {
                    event.setCancelled(true);
                    if (damager instanceof Mob) ((Mob) damager).setTarget(null); // Force stop tracking
                    return;
                }
            }

            // 2. Prevent attacking Owner
            if (victim.equals(owner)) {
                event.setCancelled(true);
                return;
            }

            // 3. PvE / PvP Compatibility Check
            // We use PVEManager to decide if this specific target is allowed for the current mode.
            if (!plugin.pveManager.canPetAttack(owner, victim)) {
                event.setCancelled(true);
                if (damager instanceof Mob) {
                    ((Mob) damager).setTarget(null); // Stop tracking invalid target
                }
            }
        }
    }

    // --- YOUR EXISTING CODE: PLAYER COMMANDING PET ---
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        // Check if the attacker is a player
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // Check if the victim is alive (we don't attack item frames/boats)
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) event.getEntity();

        // Check if player has an active pet
        Entity pet = plugin.petManager.getPet(player);
        if (pet == null) return;

        // Prevent pet from attacking its own owner (sanity check)
        if (target.equals(player)) return;

        // Prevent pet from attacking itself
        if (target.equals(pet)) return;

        // PVP Check for Commanding Pet
        if (target instanceof Player) {
            Player victimPlayer = (Player) target;
            if (!plugin.pvpManager.canAttack(player, victimPlayer)) {
                player.sendMessage(ChatColor.RED + "You or the target are in PVE mode!");
                return;
            }
        }

        // Command the pet to attack
        plugin.petManager.attackTarget(player, target);
    }
}