package me.theprisonbandit.prisonVaults.pets;

import me.theprisonbandit.prisonVaults.PrisonVaults;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PetManager {

    private final PrisonVaults plugin;
    private final File file;
    private final FileConfiguration config;

    // Tracks active pets
    private final Map<UUID, Entity> activePets = new HashMap<>();
    private final Map<UUID, PetType> activePetTypes = new HashMap<>();

    // NEW: Tracks players who are currently typing a nickname
    public final Map<UUID, PetType> renamingPlayers = new HashMap<>();

    private final Map<UUID, Long> petAttackCooldowns = new HashMap<>();
    private final Set<UUID> respawningPlayers = new HashSet<>();

    public PetManager(PrisonVaults plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        if (!file.exists()) plugin.saveResource("pets.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);

        startFollowTask();
    }

    public Entity getPet(Player p) {
        return activePets.get(p.getUniqueId());
    }

    public boolean isPet(Entity entity) {
        return activePets.containsValue(entity);
    }

    public Player getPetOwner(Entity pet) {
        for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
            if (entry.getValue().equals(pet)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }

    public PetType getPetType(Entity entity) {
        for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
            if (entry.getValue().equals(entity)) {
                return activePetTypes.get(entry.getKey());
            }
        }
        return null;
    }

    // --- NICKNAME LOGIC ---

    public void setPetNickname(Player player, PetType type, String nickname) {
        if (!hasPet(player, type)) {
            player.sendMessage(ChatColor.RED + "You do not own a " + type.display + "!");
            return;
        }

        config.set("nicknames." + player.getUniqueId() + "." + type.name(), nickname);
        save();

        if (activePetTypes.get(player.getUniqueId()) == type) {
            Entity pet = activePets.get(player.getUniqueId());
            if (pet instanceof LivingEntity) {
                updatePetNametag((LivingEntity) pet);
            }
        }
        player.sendMessage(ChatColor.GREEN + "Set nickname for " + type.display + " to: " + ChatColor.translateAlternateColorCodes('&', nickname));
    }

    public String getPetDisplayName(Player player, PetType type) {
        String nick = config.getString("nicknames." + player.getUniqueId() + "." + type.name());
        String baseName = ChatColor.GOLD + player.getName() + "'s " + ChatColor.YELLOW + type.display;

        if (nick != null && !nick.isEmpty()) {
            String coloredNick = ChatColor.translateAlternateColorCodes('&', nick);
            return baseName + ChatColor.GRAY + " (" + ChatColor.RESET + coloredNick + ChatColor.GRAY + ")";
        }
        return baseName;
    }

    public void updatePetNametag(LivingEntity pet) {
        Player owner = null;
        PetType type = null;

        for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
            if (entry.getValue().equals(pet)) {
                owner = Bukkit.getPlayer(entry.getKey());
                type = activePetTypes.get(entry.getKey());
                break;
            }
        }

        if (owner == null || type == null) return;

        String displayName = getPetDisplayName(owner, type);
        int hp = (int) pet.getHealth();
        String finalName = displayName + ChatColor.RED + " " + hp + "❤";

        pet.setCustomName(finalName);
        pet.setCustomNameVisible(true);
    }

    // ... (Keep existing methods: handlePetDeath, attackTarget, spawnPet, etc.) ...

    public void handlePetDeath(Player owner) {
        if (respawningPlayers.contains(owner.getUniqueId())) return;
        activePets.remove(owner.getUniqueId());
        PetType type = activePetTypes.get(owner.getUniqueId());
        if (type == null) return;
        owner.sendMessage(ChatColor.RED + "Your pet " + type.display + " died!");
        owner.sendMessage(ChatColor.GRAY + "Respawning in 10 seconds...");
        respawningPlayers.add(owner.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!owner.isOnline()) {
                    respawningPlayers.remove(owner.getUniqueId());
                    return;
                }
                spawnPet(owner, type);
                owner.sendMessage(ChatColor.GREEN + "Your pet has respawned!");
                respawningPlayers.remove(owner.getUniqueId());
            }
        }.runTaskLater(plugin, 200L);
    }

    public void attackTarget(Player owner, LivingEntity target) {
        Entity rawPet = activePets.get(owner.getUniqueId());
        if (rawPet == null || !rawPet.isValid() || !(rawPet instanceof LivingEntity)) return;
        LivingEntity pet = (LivingEntity) rawPet;

        // Cooldown check (1 second between attacks)
        if (petAttackCooldowns.containsKey(owner.getUniqueId())) {
            if (System.currentTimeMillis() < petAttackCooldowns.get(owner.getUniqueId())) return;
        }
        petAttackCooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + 1000);

        Location petLoc = pet.getLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        Vector direction = targetLoc.toVector().subtract(petLoc.toVector()).normalize();

        // Make pet look at target
        Location lookLoc = petLoc.clone();
        lookLoc.setDirection(direction);
        pet.teleport(lookLoc);

        // --- ATTACK LOGIC ---

        // 1. Ranged Attackers
        // REMOVED "|| pet instanceof WitherSkeleton" from here so it uses melee instead!
        if (pet instanceof Skeleton) {
            pet.launchProjectile(Arrow.class, direction.multiply(1.6));
            owner.getWorld().playSound(petLoc, Sound.ENTITY_SKELETON_SHOOT, 1f, 1f);

        } else if (pet instanceof Blaze) {
            pet.launchProjectile(SmallFireball.class, direction.multiply(1.2));
            owner.getWorld().playSound(petLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        } else if (pet instanceof Ghast) {
            pet.launchProjectile(LargeFireball.class, direction.multiply(1.2));
            owner.getWorld().playSound(petLoc, Sound.ENTITY_GHAST_SHOOT, 1f, 1f);

        } else if (pet instanceof Wither) {
            pet.launchProjectile(WitherSkull.class, direction.multiply(1.2));
            owner.getWorld().playSound(petLoc, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

        } else if (pet instanceof Snowman) {
            pet.launchProjectile(Snowball.class, direction.multiply(1.5));
            owner.getWorld().playSound(petLoc, Sound.ENTITY_SNOW_GOLEM_SHOOT, 1f, 1f);

        } else {
            // 2. Melee Attackers (Default)
            // Wither Skeletons will now trigger this block

            // Teleport slightly in front of the target to "hit" them
            Location strikeLoc = target.getLocation().add(target.getLocation().getDirection().multiply(0.5));
            strikeLoc.setDirection(target.getLocation().toVector().subtract(strikeLoc.toVector()));
            pet.teleport(strikeLoc);

            pet.swingMainHand();
            owner.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

            // Calculate Damage
            double dmg = 5.0; // Default damage
            if (pet instanceof IronGolem) dmg = 10.0;
            if (pet instanceof WitherSkeleton) dmg = 8.0; // Wither Skeletons hit harder
            if (pet instanceof Warden) dmg = 15.0;

            target.damage(dmg, pet);

            // Optional: Apply Wither Effect
            if (pet instanceof WitherSkeleton) {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER, 100, 1));
            }
        }
    }

    public void spawnPet(Player player, PetType type) {
        despawnPet(player);
        Entity entity = player.getWorld().spawnEntity(player.getLocation(), type.type);

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            if (living.getAttribute(Attribute.SCALE) != null) living.getAttribute(Attribute.SCALE).setBaseValue(type.scale);
            if (living.getAttribute(Attribute.MAX_HEALTH) != null) living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100.0);
            living.setHealth(100.0);
            living.setInvulnerable(false);
            living.setAI(false);
            living.setSilent(true);
            living.setCollidable(false);

            if (living instanceof Bat) ((Bat) living).setAwake(true);
            if (living instanceof Boss) {
                BossBar bar = ((Boss) living).getBossBar();
                if (bar != null) { bar.setVisible(false); bar.removeAll(); }
            }

            applyVariants(living, type);
            activePets.put(player.getUniqueId(), entity);
            activePetTypes.put(player.getUniqueId(), type);
            updatePetNametag(living);
        }
        player.sendMessage(ChatColor.GREEN + "You summoned your " + type.display + "!");
    }

    private void applyVariants(LivingEntity entity, PetType type) {
        if (type.variant == null) return;
        if (entity instanceof Cat) {
            try {
                String v = type.variant.toUpperCase();
                Cat.Type catType = Cat.Type.TABBY;
                if (v.equals("BLACK")) catType = Cat.Type.BLACK;
                else if (v.equals("ALL_BLACK")) catType = Cat.Type.ALL_BLACK;
                else catType = Cat.Type.valueOf(v);
                ((Cat) entity).setCatType(catType);
                ((Cat) entity).setTamed(true);
            } catch (Exception ignored) {}
        } else if (entity instanceof Parrot) {
            try {
                Parrot.Variant pVariant = Parrot.Variant.valueOf(type.variant);
                ((Parrot) entity).setVariant(pVariant);
            } catch (Exception ignored) {}
        } else if (entity instanceof Fox) {
            try {
                Fox.Type foxType = Fox.Type.valueOf(type.variant);
                ((Fox) entity).setFoxType(foxType);
            } catch (Exception ignored) {}
        }
    }

    public void despawnPet(Player player) {
        if (activePets.containsKey(player.getUniqueId())) {
            Entity e = activePets.get(player.getUniqueId());
            if (e != null && !e.isDead()) e.remove();
            activePets.remove(player.getUniqueId());
        }
    }

    public void removePetFully(Player player) {
        despawnPet(player);
        activePetTypes.remove(player.getUniqueId());
    }

    private void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activePets.keySet()) {
                    if (petAttackCooldowns.containsKey(uuid)) {
                        if (System.currentTimeMillis() < petAttackCooldowns.get(uuid)) continue;
                        else petAttackCooldowns.remove(uuid);
                    }

                    Player p = Bukkit.getPlayer(uuid);
                    Entity pet = activePets.get(uuid);
                    PetType type = activePetTypes.get(uuid);

                    if (p == null || !p.isOnline() || pet == null || pet.isDead()) {
                        if (pet != null && !pet.isDead()) pet.remove();
                        continue;
                    }

                    if (pet instanceof Bat) {
                        Bat bat = (Bat) pet;
                        if (!bat.isAwake()) bat.setAwake(true);
                    }

                    for (Entity nearby : pet.getNearbyEntities(5, 3, 5)) {
                        if (nearby instanceof Creeper) {
                            Creeper creeper = (Creeper) nearby;
                            creeper.setTarget(null);
                            Vector direction = creeper.getLocation().toVector().subtract(pet.getLocation().toVector()).normalize();
                            creeper.setVelocity(direction.multiply(0.5).setY(0.2));
                        }
                    }

                    Location playerLoc = p.getLocation();
                    Location petLoc = pet.getLocation();

                    if (playerLoc.distanceSquared(petLoc) > 400) {
                        pet.teleport(playerLoc);
                        continue;
                    }

                    Location target;
                    if (type.flying) {
                        Vector inverseDir = p.getLocation().getDirection().multiply(-1.5);
                        target = p.getLocation().add(inverseDir);
                        target.add(0, 2.5, 0);
                        target.setYaw(p.getLocation().getYaw());
                    } else {
                        Vector inverseDir = p.getLocation().getDirection().multiply(-1.2);
                        target = p.getLocation().add(inverseDir);
                        target.setY(p.getLocation().getY());
                        target.setDirection(p.getLocation().toVector().subtract(target.toVector()));
                    }

                    if (playerLoc.distanceSquared(petLoc) > 4) {
                        Vector dir = target.toVector().subtract(petLoc.toVector()).normalize().multiply(0.6);
                        Location newLoc = petLoc.clone().add(dir);

                        if (type.flying) newLoc.setYaw(p.getLocation().getYaw());
                        else newLoc.setDirection(p.getLocation().toVector().subtract(newLoc.toVector()));

                        pet.teleport(newLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    public boolean hasPet(Player p, PetType type) {
        List<String> owned = config.getStringList(p.getUniqueId().toString());
        return owned.contains(type.name());
    }

    public void buyPet(Player p, PetType type) {
        if (plugin.getBalance(p) < type.price) {
            p.sendMessage(ChatColor.RED + "You cannot afford this! Cost: $" + type.price);
            return;
        }

        plugin.removeMoney(p, type.price);
        List<String> owned = config.getStringList(p.getUniqueId().toString());
        owned.add(type.name());
        config.set(p.getUniqueId().toString(), owned);
        save();

        p.sendMessage(ChatColor.GREEN + "You purchased " + type.display + "!");
        spawnPet(p, type);
    }

    public void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "Pet Shop");
        for (PetType pet : PetType.values()) {
            ItemStack item = new ItemStack(pet.icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + pet.display);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Scale: " + pet.scale + "x");
            lore.add(ChatColor.GRAY + "Type: " + (pet.flying ? "Flying" : "Ground"));

            if (hasPet(p, pet)) {
                lore.add(ChatColor.YELLOW + "ALREADY OWNED");
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + String.format("%,.0f", pet.price));
                lore.add(ChatColor.YELLOW + "Click to Buy");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close Shop");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        p.openInventory(inv);
    }

    // --- UPDATED OPEN SELECTOR ---
    public void openSelector(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "My Pets");
        List<String> owned = config.getStringList(p.getUniqueId().toString());

        for (String key : owned) {
            try {
                PetType pet = PetType.valueOf(key);
                ItemStack item = new ItemStack(pet.icon);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + pet.display);

                // Add Rename Lore here
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Left-Click to Summon");
                lore.add(ChatColor.GOLD + "Right-Click to Rename");
                meta.setLore(lore);

                item.setItemMeta(meta);
                inv.addItem(item);
            } catch (Exception ignored) {}
        }

        ItemStack despawn = new ItemStack(Material.TNT);
        ItemMeta dm = despawn.getItemMeta();
        dm.setDisplayName(ChatColor.RED + "Despawn Current Pet");
        despawn.setItemMeta(dm);
        inv.setItem(48, despawn);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close Menu");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        p.openInventory(inv);
    }

    private void save() {
        try { config.save(file); } catch (IOException e) {}
    }
}