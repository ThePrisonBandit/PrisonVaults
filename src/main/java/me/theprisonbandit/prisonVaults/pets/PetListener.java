package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class PetListener implements Listener {

    private final PrisonVaults plugin;

    public PetListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- PREVENT EXPLOSION BLOCK DAMAGE ---
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof WitherSkull) {
            WitherSkull skull = (WitherSkull) entity;
            if (skull.getShooter() instanceof Entity && plugin.petManager.isPet((Entity) skull.getShooter())) {
                event.blockList().clear();
            }
        }
        else if (entity instanceof LargeFireball) {
            LargeFireball fireball = (LargeFireball) entity;
            if (fireball.getShooter() instanceof Entity && plugin.petManager.isPet((Entity) fireball.getShooter())) {
                event.blockList().clear();
            }
        }
        else if (plugin.petManager.isPet(entity)) {
            event.blockList().clear();
        }
    }

    // --- NAMETAG UPDATES ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPetHealthChange(EntityDamageEvent event) {
        if (plugin.petManager.isPet(event.getEntity())) {
            LivingEntity pet = (LivingEntity) event.getEntity();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (pet.isValid() && !pet.isDead()) plugin.petManager.updatePetNametag(pet);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPetHeal(EntityRegainHealthEvent event) {
        if (plugin.petManager.isPet(event.getEntity())) {
            LivingEntity pet = (LivingEntity) event.getEntity();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (pet.isValid() && !pet.isDead()) plugin.petManager.updatePetNametag(pet);
            });
        }
    }

    // --- PREVENT GRIEFING (FIRE) ---
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() != null) {
            Entity igniter = event.getIgnitingEntity();
            if (plugin.petManager.isPet(igniter)) {
                event.setCancelled(true);
                return;
            }
            if (igniter instanceof Projectile) {
                ProjectileSource source = ((Projectile) igniter).getShooter();
                if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (plugin.petManager.isPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // --- AI CONTROL ---
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.petManager.isPet(event.getEntity())) return;
        LivingEntity target = event.getTarget();
        Entity pet = event.getEntity();
        Player owner = plugin.petManager.getPetOwner(pet);

        if (owner == null) return;

        // Check using PVEManager
        if (target != null && !plugin.pveManager.canPetAttack(owner, target)) {
            event.setCancelled(true);
        }
    }

    // --- DAMAGE LOGIC ---
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();

        // 1. VICTIM IS PET
        if (plugin.petManager.isPet(victim)) {
            Player owner = plugin.petManager.getPetOwner(victim);
            if (owner == null) return;
            PVEManager.Mode mode = plugin.pveManager.getMode(owner);

            if (attacker.equals(owner)) {
                event.setCancelled(true);
                owner.sendMessage(ChatColor.RED + "You cannot hurt your own pet!");
                return;
            }
            if (attacker instanceof Player) {
                if (mode == PVEManager.Mode.PVE) {
                    event.setCancelled(true);
                    ((Player) attacker).sendMessage(ChatColor.RED + "This pet is in Passive Mode (PvE) and cannot be hurt.");
                    return;
                }
            }
        }

        // 2. ATTACKER IS PET
        Entity trueAttacker = attacker;
        if (attacker instanceof Projectile) {
            ProjectileSource src = ((Projectile) attacker).getShooter();
            if (src instanceof Entity) trueAttacker = (Entity) src;
        }

        if (plugin.petManager.isPet(trueAttacker)) {
            Player owner = plugin.petManager.getPetOwner(trueAttacker);
            if (owner == null) return;

            // Check using PVEManager
            if (!plugin.pveManager.canPetAttack(owner, victim)) {
                event.setCancelled(true);
                return;
            }

            // Manually burn enemies for Blaze/Ghast
            if (victim instanceof LivingEntity && !event.isCancelled()) {
                if (trueAttacker instanceof Blaze || trueAttacker instanceof Ghast) {
                    victim.setFireTicks(100);
                }
            }

            // --- BEE FIX: INFINITE STINGER ---
            if (trueAttacker instanceof Bee) {
                Bee bee = (Bee) trueAttacker;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (bee.isValid()) {
                        bee.setHasStung(false);
                    }
                });
            }
        }

        // 3. OWNER ATTACK TRIGGER
        if (attacker instanceof Player) {
            Player p = (Player) attacker;
            if (victim instanceof LivingEntity && !plugin.petManager.isPet(victim) && !victim.equals(p)) {
                triggerPetAttack(p, (LivingEntity) victim);
            }
        }
    }

    private void triggerPetAttack(Player owner, LivingEntity victim) {
        if (plugin.pveManager.canPetAttack(owner, victim)) {
            plugin.petManager.attackTarget(owner, victim);
        }
    }

    // --- XP & DEATH ---
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
            Entity damager = dmg.getDamager();

            Player owner = null;
            Mob petMob = null;

            if (plugin.petManager.isPet(damager)) {
                owner = plugin.petManager.getPetOwner(damager);
                if (damager instanceof Mob) petMob = (Mob) damager;
            }
            else if (damager instanceof Projectile) {
                ProjectileSource source = ((Projectile) damager).getShooter();
                if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) {
                    owner = plugin.petManager.getPetOwner((Entity) source);
                    if (source instanceof Mob) petMob = (Mob) source;
                }
            }

            if (owner != null && owner.isOnline()) {
                double xpAmount = 100.0;
                plugin.petManager.addPetXp(owner, xpAmount);
                PetType type = plugin.petManager.getActivePetType(owner);
                String displayName = plugin.petManager.getPetDisplayName(owner, type);
                owner.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(ChatColor.GREEN + "+" + (int)xpAmount + " XP for " + displayName));

                if (petMob != null) {
                    petMob.setTarget(null);
                }
            }
        }

        if (plugin.petManager.isPet(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            Player owner = plugin.petManager.getPetOwner(event.getEntity());
            if (owner != null) plugin.petManager.handlePetDeath(owner);
        }
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

        if (name.contains("Close")) { p.closeInventory(); return; }
        if (name.contains("Despawn")) { plugin.petManager.removePetFully(p); return; }

        if (name.contains("Target Mode")) {
            // Updated to use PVEManager
            plugin.pveManager.toggleMode(p);
            plugin.petManager.openSelector(p); // Re-open to update icon
            return;
        }

        for (PetType type : PetType.values()) {
            if (type.display.equals(name)) {
                if (title.equals("Pet Shop")) {
                    if (!plugin.petManager.hasPet(p, type)) plugin.petManager.buyPet(p, type);
                    else p.sendMessage(ChatColor.RED + "You already own this pet!");
                } else {
                    if (event.isRightClick()) {
                        p.closeInventory();
                        plugin.petManager.renamingPlayers.put(p.getUniqueId(), type);
                        p.sendMessage(ChatColor.GREEN + "Enter nickname in chat:");
                    } else {
                        plugin.petManager.spawnPet(p, type);
                        p.closeInventory();
                    }
                }
                break;
            }
        }
    }

    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.petManager.renamingPlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            PetType type = plugin.petManager.renamingPlayers.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Renaming cancelled.");
            } else {
                plugin.petManager.setPetNickname(player, type, message);
                player.sendMessage(ChatColor.GREEN + "Pet renamed!");
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.petManager.openSelector(player));
        }
    }
    @EventHandler public void onCombust(EntityCombustEvent event) { if (plugin.petManager.isPet(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { plugin.petManager.removePetFully(event.getPlayer()); }
}