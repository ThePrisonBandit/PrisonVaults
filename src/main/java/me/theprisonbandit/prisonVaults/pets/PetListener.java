package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class PetListener implements Listener {

    private final PrisonVaults plugin;

    public PetListener(PrisonVaults plugin) {
        this.plugin = plugin;
    }

    // --- WARDEN DARKNESS HANDLING (NEW) ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWardenDarkness(EntityPotionEffectEvent event) {
        // 1. Filter for Darkness applied to Players
        if (event.getModifiedType() != PotionEffectType.DARKNESS) return;
        if (!(event.getEntity() instanceof Player)) return;

        // 2. Filter for Cause: WARDEN
        if (event.getCause() != EntityPotionEffectEvent.Cause.WARDEN) return;

        Player victim = (Player) event.getEntity();

        // 3. Scan nearby entities to find the Warden Pet responsible
        // The Warden's sonic boom/darkness range is roughly 20 blocks.
        boolean nearbyWardenPetFound = false;

        for (Entity entity : victim.getNearbyEntities(25, 25, 25)) {
            if (entity instanceof Warden && plugin.petManager.isPet(entity)) {
                Player owner = plugin.petManager.getPetOwner(entity);
                if (owner == null) continue;

                // LOGIC A: OWNER SAFETY (Always active)
                // If the victim is the Owner, they are immune to their own pet's darkness.
                if (victim.equals(owner)) {
                    event.setCancelled(true); //
                    return;
                }

                // LOGIC B: PvE SAFETY
                // If the Owner is in PvE mode, their pet should NOT blind bystanders.
                PVEManager.Mode mode = plugin.pveManager.getMode(owner); //
                if (mode == PVEManager.Mode.PVE) {
                    event.setCancelled(true); //
                    return;
                }

                // LOGIC C: PvP Mode (Implicit)
                // If we are here, it means:
                // - Victim is NOT the owner.
                // - Owner is in PvP mode.
                // Therefore, we DO NOT cancel. The darkness applies to the enemy.
            }
        }
    }

    // --- IMMUNITIES ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (plugin.petManager.isPet(event.getEntity())) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.DROWNING ||
                    cause == EntityDamageEvent.DamageCause.FALL ||
                    cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    cause == EntityDamageEvent.DamageCause.CONTACT ||
                    cause == EntityDamageEvent.DamageCause.FALLING_BLOCK ||
                    cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

    // --- CREEPER & EXPLOSION HANDLING ---
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        // 1. Projectiles (WitherSkull / Fireball)
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
        // 2. Pet Entities (Creeper Logic)
        else if (plugin.petManager.isPet(entity)) {

            // STRICT PASSIVE CHECK: If Passive, cancel EVERYTHING (no explosion, no death, just reset).
            Player owner = plugin.petManager.getPetOwner(entity);
            if (owner != null && plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.PASSIVE) {
                event.setCancelled(true);
                if (entity instanceof Creeper) {
                    ((Creeper) entity).setFuseTicks(0); // Reset fuse so it doesn't blow up
                }
                return;
            }

            // AGGRESSIVE MODE: Handle the "Attack"
            if (entity instanceof Creeper) {
                event.setCancelled(true); // Stop block breaking
                Creeper creeper = (Creeper) entity;

                // A. Visual Effects
                creeper.getWorld().spawnParticle(Particle.EXPLOSION, creeper.getLocation(), 1);
                creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                // B. Damage Logic
                if (owner != null) {
                    PetType type = plugin.petManager.getActivePetType(owner);
                    if (type != null) {
                        int level = plugin.petManager.getPetLevel(owner, type);
                        double damage = plugin.petManager.getScaledDamage(level, 12.0); // Base 12 damage

                        for (Entity nearby : creeper.getNearbyEntities(5, 5, 5)) {
                            if (nearby instanceof LivingEntity) {
                                LivingEntity victim = (LivingEntity) nearby;
                                // PvE/PvP Check
                                if (plugin.pveManager.canPetAttack(owner, victim)) {
                                    victim.damage(damage, creeper);
                                }
                            }
                        }
                    }

                    // C. REMOVAL & RESPAWN LOGIC
                    // We manually remove the creeper to simulate it "blowing up".
                    creeper.remove();

                    // We trigger the death handler to start the respawn timer.
                    plugin.petManager.handlePetDeath(owner);
                }
                return;
            }

            // Fallback for non-creeper pets that somehow explode
            event.blockList().clear();
        }
    }

    // --- NAMETAGS ---
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

    // --- GRIEFING ---
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() != null) {
            Entity igniter = event.getIgnitingEntity();
            if (plugin.petManager.isPet(igniter)) { event.setCancelled(true); return; }
            if (igniter instanceof Projectile) {
                ProjectileSource source = ((Projectile) igniter).getShooter();
                if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) event.setCancelled(true);
            }
        }
    }
    @EventHandler public void onEntityChangeBlock(EntityChangeBlockEvent event) { if (plugin.petManager.isPet(event.getEntity())) event.setCancelled(true); }

    // --- AI CONTROL ---
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.petManager.isPet(event.getEntity())) return;
        LivingEntity target = event.getTarget();
        Entity pet = event.getEntity();
        Player owner = plugin.petManager.getPetOwner(pet);

        if (owner == null) return;

        // STRICT PASSIVE CHECK
        if (plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.PASSIVE) {
            event.setCancelled(true);
            return;
        }

        // PvE/PvP Check
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

            if (attacker.equals(owner)) {
                event.setCancelled(true);
                owner.sendMessage(ChatColor.RED + "You cannot hurt your own pet!");
                return;
            }

            // RETALIATION (Pet Hit -> Attack Back)
            if (victim instanceof Mob && attacker instanceof LivingEntity) {
                // STRICT: Aggressive Only
                if (plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.AGGRESSIVE) {
                    if (plugin.pveManager.canPetAttack(owner, (LivingEntity) attacker)) {
                        plugin.petManager.attackTarget(owner, (LivingEntity) attacker);
                    }
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

            // STRICT PASSIVE CHECK
            if (plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.PASSIVE) {
                event.setCancelled(true);
                return;
            }

            // PvE/PvP Check
            if (!plugin.pveManager.canPetAttack(owner, victim)) {
                event.setCancelled(true);
                return;
            }

            // Effects
            if (victim instanceof LivingEntity && !event.isCancelled()) {
                LivingEntity livingVictim = (LivingEntity) victim;
                if (trueAttacker instanceof Blaze || trueAttacker instanceof Ghast) livingVictim.setFireTicks(100);
                if (trueAttacker instanceof CaveSpider) livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                if (trueAttacker instanceof Wither) livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
            }
            if (trueAttacker instanceof Bee) {
                Bee bee = (Bee) trueAttacker;
                plugin.getServer().getScheduler().runTask(plugin, () -> { if (bee.isValid()) bee.setHasStung(false); });
            }
        }

        // 3. OWNER IS VICTIM (DEFENSE MODE TRIGGER)
        if (victim instanceof Player) {
            Player p = (Player) victim;
            Entity damagerEnt = attacker;
            if (attacker instanceof Projectile && ((Projectile)attacker).getShooter() instanceof Entity) {
                damagerEnt = (Entity) ((Projectile)attacker).getShooter();
            }

            if (damagerEnt instanceof LivingEntity && plugin.petManager.getPet(p) != null) {
                CombatHandlingManager.AggressionMode agg = plugin.combatHandlingManager.getAggression(p);
                // STRICT: Aggressive Only
                if (agg == CombatHandlingManager.AggressionMode.AGGRESSIVE) {
                    // Triggers if Defense OR Attack (Attack mode usually autos-scans, but this helps reaction time)
                    if (plugin.pveManager.canPetAttack(p, (LivingEntity) damagerEnt)) {
                        plugin.petManager.attackTarget(p, (LivingEntity) damagerEnt);
                    }
                }
            }
        }
    }

    // --- XP & DEATH ---
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // XP Logic
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
            Entity damager = dmg.getDamager();
            Player owner = null;
            if (plugin.petManager.isPet(damager)) owner = plugin.petManager.getPetOwner(damager);
            else if (damager instanceof Projectile) {
                ProjectileSource source = ((Projectile) damager).getShooter();
                if (source instanceof Entity && plugin.petManager.isPet((Entity) source)) owner = plugin.petManager.getPetOwner((Entity) source);
            }

            if (owner != null && owner.isOnline()) {
                plugin.petManager.addPetXp(owner, 100.0);
                PetType type = plugin.petManager.getActivePetType(owner);
                owner.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "+100 XP for " + plugin.petManager.getPetDisplayName(owner, type)));
            }
        }

        // Pet Death Logic (Standard Death)
        if (plugin.petManager.isPet(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            Player owner = plugin.petManager.getPetOwner(event.getEntity());
            if (owner != null) plugin.petManager.handlePetDeath(owner);
        }
    }

    // --- GUI ---
    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("Pet Shop") && !title.equals("My Pets") && !title.startsWith("Shop: ") && !title.startsWith("Pets: ")) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (!item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (name.contains("Close")) { p.closeInventory(); return; }
        if (name.contains("Despawn")) { plugin.petManager.removePetFully(p); return; }
        if (name.contains("Back to Categories")) {
            if (title.startsWith("Shop: ")) plugin.petManager.openShop(p);
            else plugin.petManager.openSelector(p);
            return;
        }

        if (name.contains("Aggression:")) { plugin.combatHandlingManager.toggleAggression(p); refreshMenu(p, title); return; }
        if (name.contains("Style:")) { plugin.combatHandlingManager.toggleStyle(p); refreshMenu(p, title); return; }
        if (name.contains("Target Mode")) { plugin.pveManager.toggleMode(p); refreshMenu(p, title); return; }

        if (title.equals("Pet Shop") || title.equals("My Pets")) {
            boolean isShop = title.equals("Pet Shop");
            if (name.equals("Cats")) plugin.petManager.openCategory(p, "Cats", isShop);
            else if (name.equals("Dogs & Farm")) plugin.petManager.openCategory(p, "Dogs & Farm", isShop);
            else if (name.equals("Rabbits")) plugin.petManager.openCategory(p, "Rabbits", isShop);
            else if (name.equals("Parrots")) plugin.petManager.openCategory(p, "Parrots", isShop);
            else if (name.equals("Axolotls")) plugin.petManager.openCategory(p, "Axolotls", isShop);
            else if (name.equals("Horses")) plugin.petManager.openCategory(p, "Horses", isShop);
            else if (name.equals("Flying Pets")) plugin.petManager.openCategory(p, "Flying Pets", isShop);
            else if (name.equals("Monsters")) plugin.petManager.openCategory(p, "Monsters", isShop);
            else if (name.equals("Mythical")) plugin.petManager.openCategory(p, "Mythical", isShop);
            return;
        }
        for (PetType type : PetType.values()) {
            if (type.display.equals(name)) {
                if (title.startsWith("Shop: ")) {
                    if (!plugin.petManager.hasPet(p, type)) plugin.petManager.buyPet(p, type);
                    else p.sendMessage(ChatColor.RED + "You already own this pet!");
                } else if (title.startsWith("Pets: ")) {
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

    private void refreshMenu(Player p, String title) {
        if (title.startsWith("Pets: ")) {
            String category = title.replace("Pets: ", "");
            plugin.petManager.openCategorySelector(p, category);
        } else {
            plugin.petManager.openSelector(p);
        }
    }
    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.petManager.renamingPlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            PetType type = plugin.petManager.renamingPlayers.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) player.sendMessage(ChatColor.RED + "Renaming cancelled.");
            else {
                plugin.petManager.setPetNickname(player, type, message);
                player.sendMessage(ChatColor.GREEN + "Pet renamed!");
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.petManager.openSelector(player));
        }
    }
    @EventHandler public void onCombust(EntityCombustEvent event) { if (plugin.petManager.isPet(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { plugin.petManager.removePetFully(event.getPlayer()); }
}