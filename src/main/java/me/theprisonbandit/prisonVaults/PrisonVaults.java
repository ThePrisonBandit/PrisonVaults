package me.theprisonbandit.prisonVaults;

import me.theprisonbandit.prisonVaults.commands.*;
import me.theprisonbandit.prisonVaults.gangs.GangManager;
import me.theprisonbandit.prisonVaults.gangs.MailManager;
import me.theprisonbandit.prisonVaults.kits.KitManager;
import me.theprisonbandit.prisonVaults.listeners.ChatListener;
import me.theprisonbandit.prisonVaults.listeners.GangListener;
import me.theprisonbandit.prisonVaults.listeners.KitShopListener;
import me.theprisonbandit.prisonVaults.listeners.VaultListener;
import me.theprisonbandit.prisonVaults.managers.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    // Economy Limits
    // 999 Decillion (999 followed by 33 zeros)
    private static final double MAX_BALANCE = 999 * Math.pow(10, 33);

    // Economy & Prices
    private File pricesFile;
    private FileConfiguration pricesConfig;
    private final Map<Material, Double> priceMap = new HashMap<>();

    // Ranks
    private File ranksFile;
    private FileConfiguration ranksConfig;
    public final Map<String, Double> rankLadder = new LinkedHashMap<>();

    // Managers
    public ScoreboardManager scoreboardManager;
    public KitManager kitManager;
    public GangManager gangManager; // NEW
    public MailManager mailManager; // NEW

    // --- ENABLE LOGIC ---

    @Override
    public void onEnable() {
        // 1. Load Configurations
        loadPrices();
        loadRanks();

        // 2. Initialize Managers
        this.scoreboardManager = new ScoreboardManager(this);
        this.kitManager = new KitManager(this);
        this.gangManager = new GangManager(this); // NEW
        this.mailManager = new MailManager(this); // NEW

        // 3. Register Commands
        // Core
        this.getCommand("pv").setExecutor(this);
        this.getCommand("sell").setExecutor(new SellCommand(this));
        this.getCommand("balance").setExecutor(new BalanceCommand(this));
        this.getCommand("rankup").setExecutor(new RankupCommand(this));
        this.getCommand("addmoney").setExecutor(new AddMoneyCommand(this));
        this.getCommand("colorify").setExecutor(new ColorifyCommand(this));

        // Kits
        this.getCommand("kit").setExecutor(new KitCommand(this));
        this.getCommand("kits").setExecutor(new KitCommand(this));
        this.getCommand("createkit").setExecutor(new CreateKitCommand(this));
        this.getCommand("buykit").setExecutor(new BuyKitCommand(this));

        // Gangs & Mail (NEW)
        // IMPORTANT: We pass 'this.gangManager' so all commands share the SAME data.
        this.getCommand("gang").setExecutor(new GangCommand(this));
        this.getCommand("gangs").setExecutor(new GangsCommand(this.gangManager));
        this.getCommand("mail").setExecutor(new MailCommands(this));
        this.getCommand("inbox").setExecutor(new MailCommands(this));

        // 4. Register Events
        this.getServer().getPluginManager().registerEvents(new VaultListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        this.getServer().getPluginManager().registerEvents(new KitShopListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GangListener(this), this); // NEW

        getLogger().info("PrisonVaults (Full Core + Gangs) enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (gangManager != null) {
            gangManager.saveGangs();
        }
        getLogger().info("PrisonVaults disabled.");
    }

    // --- COMMAND: /pv <number> ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prisonvaults.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use Vaults.");
            return true;
        }

        int vaultNumber = 1;
        if (args.length > 0) {
            try {
                vaultNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid vault number.");
                return true;
            }
        }

        int maxVaults = getMaxVaults(player);

        if (vaultNumber > maxVaults && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Your rank (" + ChatColor.BLUE + getPlayerRank(player) + ChatColor.RED + ") only allows " +
                    ChatColor.GOLD + maxVaults + ChatColor.RED + " vaults.");
            player.sendMessage(ChatColor.GRAY + "Type /rankup to unlock more!");
            return true;
        }

        if (vaultNumber < 1 || vaultNumber > 104) {
            player.sendMessage(ChatColor.RED + "Vault number must be between 1 and 100.");
            return true;
        }

        openVault(player, vaultNumber);
        return true;
    }

    // --- VAULT METHODS ---

    public void openVault(Player player, int vaultNumber) {
        FileConfiguration playerData = getPlayerData(player.getUniqueId());

        String title = ChatColor.DARK_GRAY + "Vault #" + vaultNumber;
        Inventory vault = Bukkit.createInventory(player, 54, title);

        if (playerData.contains("vaults." + vaultNumber)) {
            List<ItemStack> items = (List<ItemStack>) playerData.getList("vaults." + vaultNumber);
            if (items != null) {
                vault.setContents(items.toArray(new ItemStack[0]));
            }
        }

        player.openInventory(vault);
    }

    public void saveVault(Player player, int vaultNumber, Inventory vault) {
        File file = getPlayerDataFile(player.getUniqueId());
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(file);

        playerData.set("vaults." + vaultNumber, vault.getContents());

        try {
            playerData.save(file);
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Could not save your vault! Contact an admin.");
            e.printStackTrace();
        }
    }

    // --- RANK METHODS ---

    private void loadRanks() {
        ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        rankLadder.clear();
        for (String key : ranksConfig.getKeys(false)) {
            rankLadder.put(key, ranksConfig.getDouble(key));
        }
    }

    public String getPlayerRank(Player player) {
        FileConfiguration data = getPlayerData(player.getUniqueId());
        // Safety Check
        if (rankLadder.isEmpty()) return "A";
        return data.getString("rank", rankLadder.keySet().iterator().next());
    }

    public void setPlayerRank(Player player, String newRank) {
        File file = getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set("rank", newRank);
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public int getMaxVaults(Player player) {
        String currentRank = getPlayerRank(player);
        List<String> ranks = new ArrayList<>(rankLadder.keySet());
        int index = ranks.indexOf(currentRank);
        if (index == -1) index = 0;
        return (index + 1) * 4;
    }

    // --- ECONOMY METHODS ---

    public double getBalance(Player player) {
        FileConfiguration data = getPlayerData(player.getUniqueId());
        return data.getDouble("economy.balance", 0.0);
    }

    public void addMoney(Player player, double amount) {
        File file = getPlayerDataFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        double current = data.getDouble("economy.balance", 0.0);
        double newBalance = current + amount;

        // Cap Logic: If exceeds 999 Decillion, cap it.
        if (newBalance > MAX_BALANCE) {
            newBalance = MAX_BALANCE;
        }

        data.set("economy.balance", newBalance);

        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void removeMoney(Player player, double amount) {
        addMoney(player, -amount);
    }

    public double getItemPrice(Material material) {
        return priceMap.getOrDefault(material, 0.0);
    }

    private void loadPrices() {
        pricesFile = new File(getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            saveResource("prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        for (String key : pricesConfig.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat != null) {
                priceMap.put(mat, pricesConfig.getDouble(key));
            }
        }
    }

    // --- DATA FILE HELPERS ---

    public File getPlayerDataFile(UUID uuid) {
        File folder = new File(getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, uuid.toString() + ".yml");
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        return YamlConfiguration.loadConfiguration(getPlayerDataFile(uuid));
    }
}