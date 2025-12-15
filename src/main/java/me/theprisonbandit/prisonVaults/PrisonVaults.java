package me.theprisonbandit.prisonVaults;

import me.theprisonbandit.prisonVaults.commands.*;
import me.theprisonbandit.prisonVaults.gangs.GangManager;
import me.theprisonbandit.prisonVaults.gangs.MailManager;
import me.theprisonbandit.prisonVaults.kits.KitManager;
import me.theprisonbandit.prisonVaults.listeners.*;
import me.theprisonbandit.prisonVaults.managers.*;
import me.theprisonbandit.prisonVaults.pets.PetAttackListener;
import me.theprisonbandit.prisonVaults.pets.PetListener;
import me.theprisonbandit.prisonVaults.pets.PetManager;
import me.theprisonbandit.prisonVaults.tasks.AnimationTask;
import me.theprisonbandit.prisonVaults.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PrisonVaults extends JavaPlugin implements CommandExecutor {

    // --- VARIABLES ---
    private static final double MAX_BALANCE = 999 * Math.pow(10, 33); // 999 Decillion Cap

    private File pricesFile;
    private FileConfiguration pricesConfig;
    private final Map<Material, Double> priceMap = new HashMap<>();

    private File ranksFile;
    private FileConfiguration ranksConfig;
    public final Map<String, Double> rankLadder = new LinkedHashMap<>();

    // Managers
    public JobScheduleManager jobScheduleManager;
    public ScoreboardManager scoreboardManager;
    public KitManager kitManager;
    public GangManager gangManager;
    public MailManager mailManager;
    public CooldownManager cooldownManager;
    public JobManager jobManager;

    // Staff Managers
    public RankManager rankManager;
    public StaffMailManager staffMailManager;

    // Permission Manager
    public PermissionManager permissionManager;

    // Compass Manager
    public CompassManager compassManager;

    // Update Checker
    public UpdateChecker updateChecker;

    // Chat Channel Manager
    public ChatChannelManager chatChannelManager;

    // Pet Manager
    public PetManager petManager;

    @Override
    public void onEnable() {
        // 1. Load Configurations
        loadPrices();
        loadRanks();
        saveDefaultConfig();

        // 2. Initialize Managers
        this.jobScheduleManager = new JobScheduleManager(this);

        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreboardManager.startUpdater();
        this.kitManager = new KitManager(this);
        this.gangManager = new GangManager(this);
        this.mailManager = new MailManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.jobManager = new JobManager(this);

        // Staff Managers
        this.rankManager = new RankManager(this);
        this.staffMailManager = new StaffMailManager(this);

        // Permission Manager
        this.permissionManager = new PermissionManager(this);

        // Compass Manager
        this.compassManager = new CompassManager(this);

        // Update Checker
        this.updateChecker = new UpdateChecker(this);

        // Chat Channel Manager
        this.chatChannelManager = new ChatChannelManager(this);

        // Pet Manager
        this.petManager = new PetManager(this);

        // 3. Register Commands
        this.getCommand("pv").setExecutor(this);
        this.getCommand("prisonvaults").setExecutor(this);
        this.getCommand("pay").setExecutor(this);

        this.getCommand("sell").setExecutor(new SellCommand(this));
        this.getCommand("balance").setExecutor(new BalanceCommand(this));
        this.getCommand("rankup").setExecutor(new RankupCommand(this));
        this.getCommand("addmoney").setExecutor(new AddMoneyCommand(this));
        this.getCommand("rob").setExecutor(new RobCommand(this));
        this.getCommand("colorify").setExecutor(new ColorifyCommand(this));

        // Kits
        this.getCommand("kit").setExecutor(new KitCommand(this));
        this.getCommand("kits").setExecutor(new KitCommand(this));
        this.getCommand("createkit").setExecutor(new CreateKitCommand(this));
        this.getCommand("buykit").setExecutor(new BuyKitCommand(this));

        // NEW: Reset Cooldown Command
        this.getCommand("resetcooldown").setExecutor(new ResetCooldownCommand(this));

        // Gangs & Mail
        this.getCommand("gang").setExecutor(new GangCommand(this));
        this.getCommand("gangs").setExecutor(new GangsCommand(this.gangManager));
        this.getCommand("mail").setExecutor(new MailCommands(this));
        this.getCommand("inbox").setExecutor(new MailCommands(this));

        // Jobs & Profile
        this.getCommand("job").setExecutor(new JobCommand(this));

        // Profile Commands
        ProfileCommand profileCmd = new ProfileCommand(this);
        this.getCommand("myprofile").setExecutor(profileCmd);
        this.getCommand("whois").setExecutor(profileCmd);
        this.getCommand("setbio").setExecutor(profileCmd);
        this.getCommand("setdesc").setExecutor(profileCmd);

        // Configurations & Scoreboard
        this.getCommand("pvconfig").setExecutor(new ConfigCommand(this));
        this.getCommand("pvscoreboard").setExecutor(new ScoreboardCommand(this));

        // Shop & Help
        this.getCommand("pvshop").setExecutor(new ShopCommand(this));
        this.getCommand("pvhelp").setExecutor(new HelpCommand());

        // Staff Commands
        this.getCommand("staff").setExecutor(new StaffManagerCommand(this));
        this.getCommand("setstaff").setExecutor(new SetStaffCommand(this));
        this.getCommand("staffmail").setExecutor(new StaffMailCommand(this));

        // Punishment Commands
        PunishCommands punishCmd = new PunishCommands(this);
        this.getCommand("pvkick").setExecutor(punishCmd);
        this.getCommand("pvban").setExecutor(punishCmd);
        this.getCommand("pvwarn").setExecutor(punishCmd);
        this.getCommand("pvpardon").setExecutor(punishCmd);

        // Permission Command
        this.getCommand("pvperm").setExecutor(new PermsCommand(this));

        // Compass Command
        this.getCommand("pvcompass").setExecutor(new CompassCommand(this));

        // Info Command
        this.getCommand("pvinfo").setExecutor(new InfoCommand(this));

        // Channel Commands
        this.getCommand("staffchat").setExecutor(new ChannelCommand(this, ChatChannelManager.Channel.STAFF));
        this.getCommand("gangchat").setExecutor(new ChannelCommand(this, ChatChannelManager.Channel.GANG));

        // Pet Commands
        PetCommand petCmd = new PetCommand(this);
        this.getCommand("pets").setExecutor(petCmd);
        this.getCommand("petshop").setExecutor(petCmd);

        // Announcement Command
        this.getCommand("pvannounce").setExecutor(new AnnounceCommand(this));

        // 4. Register Events
        this.getServer().getPluginManager().registerEvents(new VaultListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        this.getServer().getPluginManager().registerEvents(new KitShopListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GangListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PickpocketListener(this), this);
        this.getServer().getPluginManager().registerEvents(new JobListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ProfileListener(this), this);

        // Register Staff & Ability Listeners
        this.getServer().getPluginManager().registerEvents(new StaffManagementListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.staffMailManager, this);
        this.getServer().getPluginManager().registerEvents(new KitAbilityListener(this), this);

        // Register Permission Manager Listener
        this.getServer().getPluginManager().registerEvents(new PermissionListener(this), this);

        // Update Checker
        this.updateChecker = new UpdateChecker(this);
        this.getServer().getPluginManager().registerEvents(this.updateChecker, this);

        // Pet Listener
        this.getServer().getPluginManager().registerEvents(new PetListener(this), this);

        // Pet Attack Listener
        this.getServer().getPluginManager().registerEvents(new PetAttackListener(this), this);

        // 5. Start Animation Task
        new AnimationTask(this).runTaskTimer(this, 0L, 1L);

        getLogger().info("PrisonVaults enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Cleanup Pets
        if (petManager != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                petManager.despawnPet(p);
            }
        }

        if (gangManager != null) gangManager.saveGangs();
        if (cooldownManager != null) cooldownManager.saveCooldowns();
        if (compassManager != null) compassManager.removeAll();
        getLogger().info("PrisonVaults disabled.");
    }

    // --- COMMAND EXECUTOR (PV & PAY) ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // RELOAD
        if (label.equalsIgnoreCase("prisonvaults")) {
            if (!sender.hasPermission("prisonvaults.admin")) return true;
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadAllConfigs();
                sender.sendMessage(ChatColor.GREEN + "Configs reloaded.");
                if (sender instanceof Player) SoundUtils.playSound((Player) sender, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                return true;
            }
        }

        // PAY
        if (label.equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Offline.");
                SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(ChatColor.RED + "Cannot pay self.");
                SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            try {
                double amount = Double.parseDouble(args[1]);
                if (amount <= 0) return true;
                if (getBalance(player) < amount) {
                    player.sendMessage(ChatColor.RED + "Insufficient funds.");
                    SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                removeMoney(player, amount);
                addMoney(target, amount);
                player.sendMessage(ChatColor.GREEN + "Paid.");
                target.sendMessage(ChatColor.GREEN + "Received.");
                SoundUtils.playDualSound(player, target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                scoreboardManager.updateScoreboard(player);
                scoreboardManager.updateScoreboard(target);
            } catch (Exception e) {}
            return true;
        }

        // PV
        if (label.equalsIgnoreCase("pv") && sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission("prisonvaults.use")) {
                SoundUtils.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            int v = 1;
            if (args.length > 0) try { v = Integer.parseInt(args[0]); } catch (Exception e){}
            if (v > getMaxVaults(p) && !p.isOp()) {
                p.sendMessage(ChatColor.RED + "Max: " + getMaxVaults(p));
                SoundUtils.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return true;
            }
            SoundUtils.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            openVault(p, v);
        }
        return true;
    }

    // --- METHODS ---
    public void openVault(Player player, int vaultNumber) {
        FileConfiguration d = getPlayerData(player.getUniqueId());
        Inventory v = Bukkit.createInventory(player, 54, ChatColor.DARK_GRAY + "Vault #" + vaultNumber);
        if (d.contains("vaults." + vaultNumber)) {
            List<ItemStack> i = (List<ItemStack>) d.getList("vaults." + vaultNumber);
            if (i != null) v.setContents(i.toArray(new ItemStack[0]));
        }
        player.openInventory(v);
    }
    public void openVault(Player viewer, int vaultNumber, Player owner, String title) {
        FileConfiguration d = getPlayerData(owner.getUniqueId());
        Inventory v = Bukkit.createInventory(null, 54, title);
        if (d.contains("vaults." + vaultNumber)) {
            List<ItemStack> i = (List<ItemStack>) d.getList("vaults." + vaultNumber);
            if (i != null) v.setContents(i.toArray(new ItemStack[0]));
        }
        viewer.openInventory(v);
    }
    public void saveVault(Player player, int vaultNumber, Inventory vault) {
        File f = getPlayerDataFile(player.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("vaults." + vaultNumber, vault.getContents());
        try { d.save(f); } catch (IOException e) {}
    }

    // --- RANK & BALANCE ---

    // 1. Get Rank
    public String getPlayerRank(OfflinePlayer player) {
        FileConfiguration d = getPlayerData(player.getUniqueId());
        if (rankLadder.isEmpty()) return "A";
        return d.getString("rank", rankLadder.keySet().iterator().next());
    }
    public String getPlayerRank(Player player) { return getPlayerRank((OfflinePlayer) player); }

    public void setPlayerRank(Player p, String r) {
        File f = getPlayerDataFile(p.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("rank", r);
        try { d.save(f); } catch (IOException e) {}
    }

    public int getMaxVaults(Player p) {
        String r = getPlayerRank(p);
        List<String> rs = new ArrayList<>(rankLadder.keySet());
        int i = rs.indexOf(r);
        if (i == -1) i = 0;
        return (i + 1) * 4;
    }

    // 2. Get Balance
    public double getBalance(OfflinePlayer player) {
        FileConfiguration d = getPlayerData(player.getUniqueId());
        return d.getDouble("economy.balance", 0.0);
    }
    public double getBalance(Player player) { return getBalance((OfflinePlayer) player); }

    public void addMoney(Player player, double amount) {
        File f = getPlayerDataFile(player.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        double currentBalance = d.getDouble("economy.balance", 0.0);
        if (currentBalance + amount > MAX_BALANCE) {
            d.set("economy.balance", MAX_BALANCE);
        } else {
            d.set("economy.balance", currentBalance + amount);
        }
        try { d.save(f); } catch (IOException e) {}
    }

    public void removeMoney(Player player, double amount) {
        File f = getPlayerDataFile(player.getUniqueId());
        FileConfiguration d = YamlConfiguration.loadConfiguration(f);
        d.set("economy.balance", d.getDouble("economy.balance", 0.0) - amount);
        try { d.save(f); } catch (IOException e) {}
    }

    public double getItemPrice(Material material) { return priceMap.getOrDefault(material, 0.0); }
    public void reloadAllConfigs() { reloadConfig(); loadPrices(); loadRanks(); }
    public FileConfiguration getPlayerData(UUID uuid) { return YamlConfiguration.loadConfiguration(getPlayerDataFile(uuid)); }
    public File getPlayerDataFile(UUID uuid) { return new File(getDataFolder(), "data/" + uuid + ".yml"); }
    private void loadPrices() {
        pricesFile = new File(getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) saveResource("prices.yml", false);
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
        priceMap.clear();
        for (String k : pricesConfig.getKeys(false)) {
            Material m = Material.matchMaterial(k);
            if (m != null) priceMap.put(m, pricesConfig.getDouble(k));
        }
    }
    private void loadRanks() {
        ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) saveResource("ranks.yml", false);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        rankLadder.clear();
        for (String k : ranksConfig.getKeys(false)) rankLadder.put(k, ranksConfig.getDouble(k));
    }

    public JobScheduleManager getJobScheduleManager() {
        return jobScheduleManager;
    }
}