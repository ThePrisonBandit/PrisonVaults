package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import me.theprisonbandit.prisonVaults.utils.NumberUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PetManager {

    public final PrisonVaults plugin;
    private final File file;
    private final FileConfiguration config;

    private final Map<UUID, Entity> activePets = new HashMap<>();
    private final Map<UUID, PetType> activePetTypes = new HashMap<>();
    public final Map<UUID, PetType> renamingPlayers = new HashMap<>();

    private final Map<UUID, Long> petAttackCooldowns = new HashMap<>();
    private final Map<UUID, Long> autoAttackCooldowns = new HashMap<>();
    private final Map<UUID, UUID> activeTargets = new HashMap<>();
    private final Set<UUID> respawningPlayers = new HashSet<>();

    private static final int MAX_LEVEL = 100;
    private static final double MIN_HP = 100.0;
    private static final double MAX_HP = 1024.0;
    private static final double HP_PER_LEVEL = (MAX_HP - MIN_HP) / (MAX_LEVEL - 1);

    public PetManager(PrisonVaults plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        if (!file.exists()) plugin.saveResource("pets.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);
        startFollowTask();
    }

    public PetType getActivePetType(Player p) {
        return activePetTypes.get(p.getUniqueId());
    }

    public void spawnPet(Player player, PetType type) {
        if (respawningPlayers.contains(player.getUniqueId())) return;
        despawnPet(player);
        Entity entity = player.getWorld().spawnEntity(player.getLocation(), type.type);

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            activePets.put(player.getUniqueId(), entity);
            activePetTypes.put(player.getUniqueId(), type);
            living.setMetadata("prisonvaults_pet_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            try {
                if (living.getAttribute(Attribute.SCALE) != null) {
                    living.getAttribute(Attribute.SCALE).setBaseValue(type.scale);
                }
                int level = getPetLevel(player, type);
                double maxHp = getScaledHealth(level);
                if (maxHp > 1024.0) maxHp = 1024.0;
                if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
                    living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
                }
                living.setHealth(maxHp);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply pet stats: " + e.getMessage());
            }

            living.setInvulnerable(false);
            living.setAI(true);
            living.setSilent(false);
            living.setCollidable(false);

            if (living instanceof Mob) {
                Mob mob = (Mob) living;
                mob.setTarget(null);
                mob.setAware(true);
            }

            if (living instanceof Warden) ((Warden) living).clearAnger(player);
            if (living instanceof Bat) ((Bat) living).setAwake(true);
            if (living instanceof Phantom) ((Phantom) living).setSize(1);

            if (living instanceof Boss) {
                BossBar bar = ((Boss) living).getBossBar();
                if (bar != null) {
                    bar.setVisible(false);
                    bar.removeAll();
                }
            }

            applyVariants(living, type);
            updatePetNametag(living);
        }
        player.sendMessage(ChatColor.GREEN + "Summoned " + type.display + " (Lvl " + getPetLevel(player, type) + ")");
    }

    // --- FIX: STOP ATTACK CRASH ---
    public void stopPetAttack(Player owner) {
        Entity rawPet = activePets.get(owner.getUniqueId());
        if (rawPet == null) return;
        activeTargets.remove(rawPet.getUniqueId());

        if (rawPet instanceof Mob) {
            Mob mob = (Mob) rawPet;
            LivingEntity target = mob.getTarget();
            mob.setTarget(null);

            // FIX: Only clear anger if we have a valid target.
            // Passing 'null' to clearAnger throws IllegalArgumentException.
            if (rawPet instanceof Warden && target != null) {
                ((Warden) rawPet).clearAnger(target);
            }
        }
    }

    private void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
                    Player owner = Bukkit.getPlayer(entry.getKey());
                    Entity rawPet = entry.getValue();

                    if (owner == null || !owner.isOnline()) {
                        if (rawPet != null) rawPet.remove();
                        activeTargets.remove(entry.getKey());
                        continue;
                    }
                    if (rawPet == null || rawPet.isDead()) {
                        activeTargets.remove(entry.getKey());
                        continue;
                    }
                    if (rawPet instanceof LivingEntity) {
                        ((LivingEntity) rawPet).setRemainingAir(((LivingEntity) rawPet).getMaximumAir());
                    }

                    if (rawPet instanceof Mob) {
                        Mob pet = (Mob) rawPet;
                        UUID petId = pet.getUniqueId();

                        // 1. Anti-Self/Owner Target
                        if (pet.getTarget() != null && pet.getTarget().equals(owner)) {
                            pet.setTarget(null);
                        }
                        if (pet instanceof Warden) ((Warden) pet).clearAnger(owner);

                        // 2. Resolve Target
                        LivingEntity target = null;

                        if (activeTargets.containsKey(petId)) {
                            Entity storedTarget = Bukkit.getEntity(activeTargets.get(petId));
                            if (storedTarget instanceof LivingEntity && storedTarget.isValid() && !storedTarget.isDead()) {
                                target = (LivingEntity) storedTarget;
                            } else {
                                activeTargets.remove(petId);
                            }
                        }

                        // 3. Auto-Target Logic (Hunt)
                        if (target == null) {
                            CombatHandlingManager.AggressionMode agg = plugin.combatHandlingManager.getAggression(owner);
                            CombatHandlingManager.CombatStyle style = plugin.combatHandlingManager.getStyle(owner);

                            if (agg == CombatHandlingManager.AggressionMode.AGGRESSIVE && style == CombatHandlingManager.CombatStyle.ATTACK) {
                                target = findNearbyTarget(pet, owner);
                            }
                        }

                        // Apply
                        if (target != null && pet.getTarget() != target) {
                            pet.setTarget(target);
                        } else if (target == null && pet.getTarget() != null && !activeTargets.containsKey(petId)) {
                            pet.setTarget(null);
                        }

                        // 4. Teleport if lost
                        double distToOwner = pet.getLocation().distance(owner.getLocation());
                        if (distToOwner > 20) {
                            pet.teleport(owner.getLocation());
                            pet.setTarget(null);
                            activeTargets.remove(petId);
                            continue;
                        }

                        // 5. Combat Execution
                        boolean isFightingEnemy = (target != null &&
                                !target.equals(owner) &&
                                !target.isDead() &&
                                plugin.pveManager.canPetAttack(owner, target));

                        if (isFightingEnemy) {
                            if (isPassiveMelee(pet)) {
                                handlePassiveMobCombat(pet, owner, target);
                            }
                        } else {
                            if (activeTargets.containsKey(petId)) activeTargets.remove(petId);
                            if (distToOwner > 3.5) movePetToOwner(pet, owner);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private LivingEntity findNearbyTarget(Mob pet, Player owner) {
        double range = 10.0;
        List<Entity> nearby = pet.getNearbyEntities(range, range, range);
        LivingEntity best = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) e;

            if (plugin.pveManager.canPetAttack(owner, living)) {
                double d = pet.getLocation().distance(living.getLocation());
                if (d < closestDist) {
                    closestDist = d;
                    best = living;
                }
            }
        }
        return best;
    }

    private boolean isPassiveMelee(Entity pet) {
        return pet instanceof Rabbit || pet instanceof Horse || pet instanceof Pig ||
                pet instanceof Cow || pet instanceof Sheep || pet instanceof Chicken ||
                pet instanceof Panda || pet instanceof Axolotl || pet instanceof Cat ||
                pet instanceof Parrot || pet instanceof Fox || pet instanceof Turtle ||
                pet instanceof Ocelot || pet instanceof MushroomCow ||
                pet instanceof Bat || pet instanceof Bee;
    }

    private void handlePassiveMobCombat(Mob pet, Player owner, LivingEntity target) {
        if (plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.PASSIVE) return;

        double distToTarget = pet.getLocation().distance(target.getLocation());
        if (distToTarget > 1.5) {
            Location petLoc = pet.getLocation();
            Location targetLoc = target.getLocation();
            Vector dir = targetLoc.toVector().subtract(petLoc.toVector()).normalize();
            double speed = 0.8;

            if (pet instanceof Bat || pet instanceof Parrot || pet instanceof Bee) {
                if (targetLoc.getY() > petLoc.getY()) dir.setY(dir.getY() + 0.2);
            } else if (pet.isOnGround()) {
                if (pet instanceof Rabbit) dir.setY(0.4);
                else dir.setY(0.3);
            }

            if (pet.isOnGround() && targetLoc.getY() > petLoc.getY() + 0.5) dir.setY(0.5);
            pet.setVelocity(dir.multiply(speed));
            targetLoc.setY(petLoc.getY());
            pet.teleport(petLoc.setDirection(dir));
        }

        if (distToTarget <= 2.0) {
            UUID petId = pet.getUniqueId();
            if (autoAttackCooldowns.containsKey(petId) && System.currentTimeMillis() < autoAttackCooldowns.get(petId)) return;

            int level = getPetLevel(owner, activePetTypes.get(owner.getUniqueId()));
            double damage = getScaledDamage(level, 4.0);
            target.damage(damage, pet);
            pet.swingMainHand();
            autoAttackCooldowns.put(petId, System.currentTimeMillis() + 1000);
        }
    }

    private void movePetToOwner(Mob pet, Player owner) {
        boolean isCustomMovement = (pet instanceof Wither || pet instanceof Blaze || pet instanceof Ghast ||
                pet instanceof Skeleton || pet instanceof Warden || pet instanceof Enderman ||
                pet instanceof Phantom || pet instanceof Bee || isPassiveMelee(pet) ||
                pet instanceof Spider || pet instanceof CaveSpider);

        if (isCustomMovement) {
            Location petLoc = pet.getLocation();
            Location ownerLoc = owner.getLocation();
            Vector dir = ownerLoc.toVector().subtract(petLoc.toVector()).normalize();
            double speed = 0.35;

            if (pet instanceof Flying || pet instanceof Blaze || pet instanceof Ghast || pet instanceof Wither ||
                    pet instanceof Bat || pet instanceof Phantom || pet instanceof Bee) {
                speed = 0.5;
                if (petLoc.getY() < ownerLoc.getY() + 2) dir.setY(0.4);
            } else if (isPassiveMelee(pet)) {
                speed = 0.6;
                if (pet instanceof Axolotl && pet.isInWater()) speed = 0.5;
                else {
                    if (pet.isOnGround()) dir.setY(0.3);
                    if (ownerLoc.getY() > petLoc.getY() + 0.5) dir.setY(0.5);
                }
            } else {
                if (pet.isOnGround() && ownerLoc.getY() > petLoc.getY() + 0.5) dir.setY(0.4);
            }
            pet.setVelocity(dir.multiply(speed));
        } else {
            pet.setTarget(owner);
        }
    }

    public void attackTarget(Player owner, LivingEntity target) {
        Entity rawPet = activePets.get(owner.getUniqueId());
        if (rawPet == null || !(rawPet instanceof Mob)) return;
        Mob pet = (Mob) rawPet;

        if (plugin.combatHandlingManager.getAggression(owner) == CombatHandlingManager.AggressionMode.PASSIVE) return;
        if (!plugin.pveManager.canPetAttack(owner, target)) return;

        if (petAttackCooldowns.containsKey(owner.getUniqueId())) {
            if (System.currentTimeMillis() < petAttackCooldowns.get(owner.getUniqueId())) return;
        }
        petAttackCooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + 1000);

        activeTargets.put(pet.getUniqueId(), target.getUniqueId());
        pet.setTarget(target);

        if (pet instanceof Warden) ((Warden) pet).setAnger(target, 150);

        int level = getPetLevel(owner, activePetTypes.get(owner.getUniqueId()));
        double baseDmg = 5.0;

        Location targetLoc = target.getLocation().add(0, target.getHeight()/2, 0);
        Vector dir = targetLoc.toVector().subtract(pet.getLocation().toVector()).normalize();

        if (pet instanceof Skeleton || pet instanceof Blaze || pet instanceof Snowman || pet instanceof Wither || pet instanceof Ghast) {
            if (pet instanceof Skeleton) {
                pet.launchProjectile(Arrow.class, dir.multiply(1.6));
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1f, 1f);
            } else if (pet instanceof Blaze) {
                SmallFireball sf = pet.launchProjectile(SmallFireball.class, dir.multiply(1.2));
                sf.setIsIncendiary(false);
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            } else if (pet instanceof Snowman) {
                pet.launchProjectile(Snowball.class, dir.multiply(1.5));
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1f, 1f);
            } else if (pet instanceof Wither) {
                WitherSkull ws = pet.launchProjectile(WitherSkull.class, dir.multiply(1.5));
                ws.setCharged(true);
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
            } else if (pet instanceof Ghast) {
                LargeFireball lf = pet.launchProjectile(LargeFireball.class, dir.multiply(1.0));
                lf.setIsIncendiary(false);
                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
            }

            double finalDmg = getScaledDamage(level, baseDmg);
            target.damage(finalDmg, pet);
        }
    }

    public int getPetLevel(Player p, PetType type) { return config.getInt("stats." + p.getUniqueId() + "." + type.name() + ".level", 1); }
    public void addPetXp(Player p, double amount) {
        PetType type = activePetTypes.get(p.getUniqueId());
        if (type == null) return;
        int level = getPetLevel(p, type);
        if (level >= MAX_LEVEL) return;
        double currentXp = config.getDouble("stats." + p.getUniqueId() + "." + type.name() + ".xp", 0.0);
        double needed = level * 500;
        if (currentXp + amount >= needed) {
            level++;
            config.set("stats." + p.getUniqueId() + "." + type.name() + ".level", level);
            config.set("stats." + p.getUniqueId() + "." + type.name() + ".xp", 0.0);
            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PET LEVEL UP! " + ChatColor.YELLOW + type.display + " is now Lvl " + level + "!");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            spawnPet(p, type);
        } else {
            config.set("stats." + p.getUniqueId() + "." + type.name() + ".xp", currentXp + amount);
        }
        save();
    }
    public double getScaledHealth(int level) {
        if (level <= 1) return MIN_HP;
        if (level >= MAX_LEVEL) return MAX_HP;
        return MIN_HP + ((level - 1) * HP_PER_LEVEL);
    }
    public double getScaledDamage(int level, double baseDmg) {
        double multiplier = 1.0 + ((level - 1) * 0.05);
        return baseDmg * multiplier;
    }
    public void openShop(Player p) { openCategoryMenu(p, "Pet Shop", true); }
    public void openSelector(Player p) { openCategoryMenu(p, "My Pets", false); }

    private void openCategoryMenu(Player p, String title, boolean isShop) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        inv.setItem(10, createGuiItem(Material.COD, "Cats"));
        inv.setItem(11, createGuiItem(Material.BONE, "Dogs & Farm"));
        inv.setItem(12, createGuiItem(Material.RABBIT_FOOT, "Rabbits"));
        inv.setItem(13, createGuiItem(Material.FEATHER, "Parrots"));
        inv.setItem(14, createGuiItem(Material.AXOLOTL_BUCKET, "Axolotls"));
        inv.setItem(15, createGuiItem(Material.SADDLE, "Horses"));
        inv.setItem(16, createGuiItem(Material.ELYTRA, "Flying Pets"));
        inv.setItem(19, createGuiItem(Material.ZOMBIE_HEAD, "Monsters"));
        inv.setItem(20, createGuiItem(Material.NETHER_STAR, "Mythical"));

        if (!isShop) {
            PVEManager.Mode mode = plugin.pveManager.getMode(p);
            CombatHandlingManager.AggressionMode agg = plugin.combatHandlingManager.getAggression(p);
            CombatHandlingManager.CombatStyle style = plugin.combatHandlingManager.getStyle(p);

            ItemStack aggItem = new ItemStack(agg == CombatHandlingManager.AggressionMode.AGGRESSIVE ? Material.RED_DYE : Material.LIME_DYE);
            ItemMeta am = aggItem.getItemMeta();
            am.setDisplayName(ChatColor.GOLD + "Aggression: " + (agg == CombatHandlingManager.AggressionMode.AGGRESSIVE ? ChatColor.RED + "AGGRESSIVE" : ChatColor.GREEN + "PASSIVE"));
            aggItem.setItemMeta(am);
            inv.setItem(46, aggItem);

            ItemStack tmItem = new ItemStack(mode == PVEManager.Mode.PVP ? Material.DIAMOND_SWORD : Material.GRASS_BLOCK);
            ItemMeta tm = tmItem.getItemMeta();
            tm.setDisplayName(ChatColor.GOLD + "Target Mode: " + (mode == PVEManager.Mode.PVP ? ChatColor.RED + "PvP" : ChatColor.GREEN + "PvE"));
            tmItem.setItemMeta(tm);
            inv.setItem(47, tmItem);

            ItemStack styleItem = new ItemStack(style == CombatHandlingManager.CombatStyle.ATTACK ? Material.IRON_SWORD : Material.SHIELD);
            ItemMeta sm = styleItem.getItemMeta();
            sm.setDisplayName(ChatColor.GOLD + "Style: " + (style == CombatHandlingManager.CombatStyle.ATTACK ? ChatColor.RED + "ATTACK" : ChatColor.BLUE + "DEFENSE"));
            styleItem.setItemMeta(sm);
            inv.setItem(48, styleItem);
        }
        addBackAndCloseButtons(inv, isShop);
        p.openInventory(inv);
    }
    public void openCategory(Player p, String category, boolean isShop) {
        if (isShop) openCategoryShop(p, category);
        else openCategorySelector(p, category);
    }
    private void openCategoryShop(Player p, String category) {
        Inventory inv = Bukkit.createInventory(null, 54, "Shop: " + category);
        for (PetType type : PetType.values()) {
            if (!getCategory(type).equals(category)) continue;
            ItemStack item = new ItemStack(type.icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + type.display);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + "$" + NumberUtils.format(type.price));
            if (hasPet(p, type)) lore.add(ChatColor.RED + "ALREADY OWNED");
            else lore.add(ChatColor.YELLOW + "Click to Buy");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        addBackAndCloseButtons(inv, true);
        p.openInventory(inv);
    }
    public void openCategorySelector(Player p, String category) {
        Inventory inv = Bukkit.createInventory(null, 54, "Pets: " + category);
        List<String> owned = config.getStringList(p.getUniqueId().toString());
        for (String key : owned) {
            try {
                PetType pet = PetType.valueOf(key);
                if (!getCategory(pet).equals(category)) continue;
                ItemStack item = new ItemStack(pet.icon);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + pet.display);
                int lvl = getPetLevel(p, pet);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.AQUA + "Level: " + lvl);
                lore.add(ChatColor.RED + "Health: " + NumberUtils.format((int) getScaledHealth(lvl)));
                lore.add(ChatColor.YELLOW + "Left-Click to Summon");
                lore.add(ChatColor.GOLD + "Right-Click to Rename");
                meta.setLore(lore);
                item.setItemMeta(meta);
                inv.addItem(item);
            } catch (Exception ignored) {}
        }
        addBackAndCloseButtons(inv, false);
        p.openInventory(inv);
    }
    private void addBackAndCloseButtons(Inventory inv, boolean isShop) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(ChatColor.YELLOW + "Back to Categories");
        back.setItemMeta(bm);
        inv.setItem(45, back);
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(49, close);
    }
    private ItemStack createGuiItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        item.setItemMeta(meta);
        return item;
    }
    private String getCategory(PetType type) {
        String name = type.name();
        if (name.contains("CAT") && !name.contains("OCELOT")) return "Cats";
        if (name.contains("RABBIT")) return "Rabbits";
        if (name.contains("PARROT")) return "Parrots";
        if (name.contains("AXOLOTL")) return "Axolotls";
        if (name.contains("HORSE")) return "Horses";
        if (name.contains("BAT") || name.contains("BEE") || name.contains("PHANTOM") || name.contains("BLAZE") || name.contains("GHAST")) return "Flying Pets";
        if (name.contains("SPIDER") || name.contains("SKELETON") || name.contains("CREEPER") || name.contains("ENDERMAN") || name.contains("ZOMBIE")) return "Monsters";
        if (name.contains("WARDEN") || name.contains("WITHER") || name.contains("GOLEM")) return "Mythical";
        return "Dogs & Farm";
    }
    public Entity getPet(Player p) { return activePets.get(p.getUniqueId()); }
    public boolean isPet(Entity entity) {
        if (activePets.containsValue(entity)) return true;
        return entity.hasMetadata("prisonvaults_pet_owner");
    }
    public Player getPetOwner(Entity pet) {
        for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) if (entry.getValue().equals(pet)) return Bukkit.getPlayer(entry.getKey());
        if (pet.hasMetadata("prisonvaults_pet_owner")) {
            try {
                String uuidStr = pet.getMetadata("prisonvaults_pet_owner").get(0).asString();
                return Bukkit.getPlayer(UUID.fromString(uuidStr));
            } catch (Exception e) { return null; }
        }
        return null;
    }
    public void despawnPet(Player player) {
        if (activePets.containsKey(player.getUniqueId())) {
            Entity e = activePets.get(player.getUniqueId());
            if (e != null) e.remove();
            activePets.remove(player.getUniqueId());
            activeTargets.remove(e.getUniqueId());
        }
    }
    public void removePetFully(Player player) { despawnPet(player); activePetTypes.remove(player.getUniqueId()); }
    public boolean hasPet(Player p, PetType type) { return config.getStringList(p.getUniqueId().toString()).contains(type.name()); }
    public void buyPet(Player p, PetType type) {
        if (plugin.getBalance(p) < type.price) { p.sendMessage(ChatColor.RED + "Too expensive!"); return; }
        plugin.removeMoney(p, type.price);
        List<String> owned = config.getStringList(p.getUniqueId().toString());
        owned.add(type.name());
        config.set(p.getUniqueId().toString(), owned);
        save();
        p.sendMessage(ChatColor.GREEN + "You bought a " + type.display + "!");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        spawnPet(p, type);
    }
    public void updatePetNametag(LivingEntity pet) {
        Player owner = getPetOwner(pet);
        if (owner == null) return;
        PetType type = activePetTypes.get(owner.getUniqueId());
        int level = getPetLevel(owner, type);
        String name = getPetDisplayName(owner, type);
        pet.setCustomName(name + ChatColor.AQUA + " [Lvl " + level + "] " + ChatColor.RED + NumberUtils.format((int) pet.getHealth()) + "❤");
        pet.setCustomNameVisible(true);
    }
    public void setPetNickname(Player player, PetType type, String nickname) {
        config.set("nicknames." + player.getUniqueId() + "." + type.name(), nickname);
        save();
        if (activePetTypes.get(player.getUniqueId()) == type) updatePetNametag((LivingEntity)activePets.get(player.getUniqueId()));
    }
    public String getPetDisplayName(Player player, PetType type) {
        String nick = config.getString("nicknames." + player.getUniqueId() + "." + type.name());
        return (nick != null && !nick.isEmpty()) ? ChatColor.translateAlternateColorCodes('&', nick) : type.display;
    }
    public void handlePetDeath(Player owner) {
        owner.sendMessage(ChatColor.RED + "Your pet has died! It will respawn in 10 seconds.");
        respawningPlayers.add(owner.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                respawningPlayers.remove(owner.getUniqueId());
                if (owner.isOnline() && activePetTypes.containsKey(owner.getUniqueId())) {
                    spawnPet(owner, activePetTypes.get(owner.getUniqueId()));
                }
            }
        }.runTaskLater(plugin, 200L);
    }
    private void applyVariants(LivingEntity entity, PetType type) {
        if (type.variant != null) {
            try {
                String v = type.variant.toUpperCase();
                if (entity instanceof Parrot) ((Parrot) entity).setVariant(Parrot.Variant.valueOf(v));
                else if (entity instanceof Cat) ((Cat) entity).setCatType(Cat.Type.valueOf(v));
                else if (entity instanceof Fox) ((Fox) entity).setFoxType(Fox.Type.valueOf(v));
                else if (entity instanceof Axolotl) ((Axolotl) entity).setVariant(Axolotl.Variant.valueOf(v));
                else if (entity instanceof Rabbit) ((Rabbit) entity).setRabbitType(Rabbit.Type.valueOf(v));
                else if (entity instanceof Horse) ((Horse) entity).setColor(Horse.Color.valueOf(v));
            } catch (Exception e) {}
        }
    }
    private void save() { try { config.save(file); } catch (IOException e) {} }
    public boolean isExplicitlyTargeting(UUID petId, UUID victimId) {
        return activeTargets.containsKey(petId) && activeTargets.get(petId).equals(victimId);
    }
}