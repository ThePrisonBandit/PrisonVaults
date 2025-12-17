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

    private final PrisonVaults plugin;
    private final File file;
    private final FileConfiguration config;

    private final Map<UUID, Entity> activePets = new HashMap<>();
    private final Map<UUID, PetType> activePetTypes = new HashMap<>();
    public final Map<UUID, PetType> renamingPlayers = new HashMap<>();
    private final Map<UUID, Long> petAttackCooldowns = new HashMap<>();
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
            living.setSilent(true);
            living.setCollidable(false);

            if (living instanceof Mob) {
                Mob mob = (Mob) living;
                mob.setTarget(null);
                mob.setAware(true);
            }

            if (living instanceof Warden) {
                ((Warden) living).clearAnger(player);
            }
            if (living instanceof Bat) ((Bat) living).setAwake(true);

            if (living instanceof Phantom) {
                ((Phantom) living).setSize(1);
            }

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

    private void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
                    Player owner = Bukkit.getPlayer(entry.getKey());
                    Entity rawPet = entry.getValue();

                    if (owner == null || !owner.isOnline()) {
                        if (rawPet != null) rawPet.remove();
                        continue;
                    }
                    if (rawPet == null || rawPet.isDead()) continue;

                    if (rawPet instanceof Mob) {
                        Mob pet = (Mob) rawPet;

                        if (pet.getTarget() != null && pet.getTarget().equals(owner)) {
                            pet.setTarget(null);
                        }

                        if (pet instanceof Warden) {
                            ((Warden) pet).clearAnger(owner);
                        }

                        double dist = pet.getLocation().distance(owner.getLocation());
                        if (dist > 20) {
                            pet.teleport(owner.getLocation());
                            pet.setTarget(null);
                            continue;
                        }

                        LivingEntity currentTarget = pet.getTarget();
                        // CHECK: Use PVEManager to see if target is valid
                        boolean isFightingEnemy = (currentTarget != null &&
                                !currentTarget.equals(owner) &&
                                !currentTarget.isDead() &&
                                plugin.pveManager.canPetAttack(owner, currentTarget));

                        if (!isFightingEnemy && dist > 3.5) {
                            movePetToOwner(pet, owner);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private void movePetToOwner(Mob pet, Player owner) {
        boolean isCustomMovement = (pet instanceof Wither || pet instanceof Blaze || pet instanceof Ghast ||
                pet instanceof Skeleton || pet instanceof Warden || pet instanceof Enderman ||
                pet instanceof Phantom || pet instanceof Bee || pet instanceof Axolotl);

        if (isCustomMovement) {
            Location petLoc = pet.getLocation();
            Location ownerLoc = owner.getLocation();
            Vector dir = ownerLoc.toVector().subtract(petLoc.toVector()).normalize();

            double speed = 0.35;

            if (pet instanceof Flying || pet instanceof Blaze || pet instanceof Ghast || pet instanceof Wither ||
                    pet instanceof Bat || pet instanceof Phantom || pet instanceof Bee) {

                speed = 0.5;
                if (petLoc.getY() < ownerLoc.getY() + 2) {
                    dir.setY(0.4);
                }
            }
            else if (pet instanceof Axolotl) {
                if (!pet.isInWater()) {
                    speed = 0.5;
                    if (ownerLoc.getY() > petLoc.getY() + 0.5) dir.setY(0.4);
                } else {
                    speed = 0.4;
                }
            }
            else {
                if (pet.isOnGround() && ownerLoc.getY() > petLoc.getY() + 0.5) {
                    dir.setY(0.4);
                }
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

        if (petAttackCooldowns.containsKey(owner.getUniqueId())) {
            if (System.currentTimeMillis() < petAttackCooldowns.get(owner.getUniqueId())) return;
        }
        petAttackCooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + 1000);

        pet.setTarget(target);

        if (pet instanceof Warden) {
            ((Warden) pet).setAnger(target, 150);
        }

        int level = getPetLevel(owner, activePetTypes.get(owner.getUniqueId()));
        double baseDmg = 5.0;

        Location targetLoc = target.getLocation().add(0, target.getHeight()/2, 0);
        Vector dir = targetLoc.toVector().subtract(pet.getLocation().toVector()).normalize();

        if (pet instanceof Skeleton || pet instanceof Blaze || pet instanceof Snowman || pet instanceof Wither || pet instanceof Ghast) {

            if (pet instanceof Skeleton) {
                pet.launchProjectile(Arrow.class, dir.multiply(1.6));
            }
            else if (pet instanceof Blaze) {
                SmallFireball sf = pet.launchProjectile(SmallFireball.class, dir.multiply(1.2));
                sf.setIsIncendiary(false);
            }
            else if (pet instanceof Snowman) {
                pet.launchProjectile(Snowball.class, dir.multiply(1.5));
            }
            else if (pet instanceof Wither) {
                WitherSkull ws = pet.launchProjectile(WitherSkull.class, dir.multiply(1.5));
                ws.setCharged(true);
            }
            else if (pet instanceof Ghast) {
                LargeFireball lf = pet.launchProjectile(LargeFireball.class, dir.multiply(1.0));
                lf.setIsIncendiary(false);
            }

            double finalDmg = getScaledDamage(level, baseDmg);
            target.damage(finalDmg, pet);
        }
    }

    public int getPetLevel(Player p, PetType type) {
        return config.getInt("stats." + p.getUniqueId() + "." + type.name() + ".level", 1);
    }
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

    // --- GUI ---
    public void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "Pet Shop");
        for (PetType type : PetType.values()) {
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
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(49, close);
        p.openInventory(inv);
    }

    public void openSelector(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "My Pets");
        List<String> owned = config.getStringList(p.getUniqueId().toString());
        for (String key : owned) {
            try {
                PetType pet = PetType.valueOf(key);
                ItemStack item = new ItemStack(pet.icon);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + pet.display);
                int lvl = getPetLevel(p, pet);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.AQUA + "Level: " + lvl);
                lore.add(ChatColor.RED + "Health: " + NumberUtils.format(getScaledHealth(lvl)));
                lore.add(ChatColor.DARK_GRAY + "----------------");
                lore.add(ChatColor.YELLOW + "Left-Click to Summon");
                lore.add(ChatColor.GOLD + "Right-Click to Rename");
                meta.setLore(lore);
                item.setItemMeta(meta);
                inv.addItem(item);
            } catch (Exception ignored) {}
        }

        // Use PVEManager for status
        PVEManager.Mode mode = plugin.pveManager.getMode(p);
        ItemStack toggle = new ItemStack(mode == PVEManager.Mode.PVP ? Material.DIAMOND_SWORD : Material.GRASS_BLOCK);
        ItemMeta tm = toggle.getItemMeta();
        tm.setDisplayName(ChatColor.GOLD + "Target Mode: " + (mode == PVEManager.Mode.PVP ? ChatColor.RED + "PvP" : ChatColor.GREEN + "PvE"));
        List<String> tLore = new ArrayList<>();
        tLore.add(ChatColor.GRAY + "Click to toggle.");
        tLore.add(ChatColor.GRAY + "Current: " + (mode == PVEManager.Mode.PVP ? "Attack Players" : "Attack Mobs Only"));
        tm.setLore(tLore);
        toggle.setItemMeta(tm);
        inv.setItem(47, toggle);
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(49, close);
        p.openInventory(inv);
    }

    // --- UTILS ---
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
        }
    }
    public void removePetFully(Player player) {
        despawnPet(player);
        activePetTypes.remove(player.getUniqueId());
    }
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
        pet.setCustomName(name + ChatColor.AQUA + " [Lvl " + level + "] " + ChatColor.RED + NumberUtils.format(pet.getHealth()) + "❤");
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
            } catch (Exception e) {}
        }
    }
    private void save() { try { config.save(file); } catch (IOException e) {} }
}