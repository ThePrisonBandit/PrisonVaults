package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PetAttackListener implements Listener {

    private final PrisonVaults plugin;

    public PetAttackListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

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

        // Command the pet to attack
        plugin.petManager.attackTarget(player, target);
    }
}