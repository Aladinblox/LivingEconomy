package com.yourname.livingeconomy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.List;
import com.yourname.livingeconomy.Loan; // Assuming Loan.java is in this package

public class PlayerData {

    private final UUID playerUUID;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final LivingEconomy plugin;

    // Data fields
    private Map<String, Double> bankBalances; // Currency name -> Amount in bank
    private Map<String, Double> walletBalances; // Currency name -> Amount in wallet (moneybag)

    // Quest Data
    private String activeQuestId;
    private Map<String, Object> questProgress; // e.g., {"DIAMOND_ORE_broken": 5, "ZOMBIES_killed": 2}

    // Loan Data
    private List<Loan> activeLoans;
    // TODO: Add fields for loans, quest status, owned shops, etc. as those features are developed.
    // For example:
    // private List<Loan> activeLoans;
    // private Quest currentQuest;
    // private List<String> ownedShopIDs;

    public PlayerData(LivingEconomy plugin, UUID playerUUID) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata/" + playerUUID.toString() + ".yml");
        this.bankBalances = new HashMap<>();
        this.walletBalances = new HashMap<>();
        this.activeQuestId = null;
        this.questProgress = new HashMap<>();
        this.activeLoans = new ArrayList<>();
        // Initialize other fields here
        // this.ownedShopIDs = new ArrayList<>();

        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                // Create the directories if they don't exist
                File parentDir = dataFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                dataFile.createNewFile();
                plugin.getLogger().info("Created new player data file for " + playerUUID.toString());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player data file for " + playerUUID.toString(), e);
                return;
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load bankBalances
        if (dataConfig.isConfigurationSection("bankBalances")) {
            for (String currencyKey : dataConfig.getConfigurationSection("bankBalances").getKeys(false)) {
                bankBalances.put(currencyKey, dataConfig.getDouble("bankBalances." + currencyKey));
            }
        }
        // Load wallet balances
        if (dataConfig.isConfigurationSection("walletBalances")) {
            for (String currencyKey : dataConfig.getConfigurationSection("walletBalances").getKeys(false)) {
                walletBalances.put(currencyKey, dataConfig.getDouble("walletBalances." + currencyKey));
            }
        }

        // Load quest data
        this.activeQuestId = dataConfig.getString("activeQuest.id");
        if (dataConfig.isConfigurationSection("activeQuest.progress")) {
            questProgress.clear(); // Clear old progress before loading
            for (String key : dataConfig.getConfigurationSection("activeQuest.progress").getKeys(false)) {
                questProgress.put(key, dataConfig.get("activeQuest.progress." + key));
            }
        }
        // Load loans
        this.activeLoans.clear();
        List<Map<?, ?>> loanMaps = dataConfig.getMapList("activeLoans");
        if (loanMaps != null) {
            for (Map<?, ?> map : loanMaps) {
                this.activeLoans.add(new Loan((Map<String, Object>)map));
            }
        }

        // TODO: Load other data (loans, quests, shops)
    }

    public void save() {
        if (dataConfig == null || dataFile == null) {
            plugin.getLogger().severe("Could not save player data for " + playerUUID.toString() + ": dataConfig or dataFile is null.");
            return;
        }
        // Save bankBalances
        dataConfig.set("bankBalances", null); // Clear old balances before saving new ones
        for (Map.Entry<String, Double> entry : bankBalances.entrySet()) {
            dataConfig.set("bankBalances." + entry.getKey(), entry.getValue());
        }
        // Save wallet balances
        dataConfig.set("walletBalances", null); // Clear old wallet balances
        for (Map.Entry<String, Double> entry : walletBalances.entrySet()) {
            dataConfig.set("walletBalances." + entry.getKey(), entry.getValue());
        }

        // Save quest data
        if (activeQuestId != null) {
            dataConfig.set("activeQuest.id", activeQuestId);
            dataConfig.set("activeQuest.progress", questProgress);
        } else {
            dataConfig.set("activeQuest.id", null);
            dataConfig.set("activeQuest.progress", null);
        }
        // Save loans
        List<Map<String, Object>> loanMapsToSave = new ArrayList<>();
        for (Loan loan : this.activeLoans) {
            loanMapsToSave.add(loan.toMap());
        }
        dataConfig.set("activeLoans", loanMapsToSave);

        // TODO: Save other data (loans, quests, shops)

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data to " + dataFile.getName(), e);
        }
    }

    // Getter and Setter for bankBalances
    public Map<String, Double> getBankBalances() {
        return bankBalances;
    }

    public double getBankBalance(String currency) {
        return bankBalances.getOrDefault(currency, 0.0);
    }

    public void setBankBalance(String currency, double amount) {
        bankBalances.put(currency, amount);
    }

    public void depositToBankDirect(String currency, double amount) {
        bankBalances.put(currency, getBankBalance(currency) + amount);
    }

    public boolean withdrawFromBankDirect(String currency, double amount) {
        double currentBankBalance = getBankBalance(currency);
        if (currentBankBalance >= amount) {
            bankBalances.put(currency, currentBankBalance - amount);
            return true;
        }
        return false;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    // Getters and Setters for walletBalances
    public Map<String, Double> getWalletBalances() {
        return walletBalances;
    }

    public double getWalletBalance(String currency) {
        return walletBalances.getOrDefault(currency, 0.0);
    }

    public void setWalletBalance(String currency, double amount) {
        walletBalances.put(currency, amount);
    }

    public void giveToWallet(String currency, double amount) {
        walletBalances.put(currency, getWalletBalance(currency) + amount);
    }

    public boolean takeFromWallet(String currency, double amount) {
        double currentWalletBalance = getWalletBalance(currency);
        if (currentWalletBalance >= amount) {
            walletBalances.put(currency, currentWalletBalance - amount);
            return true;
        }
        return false;
    }

    // Combined operations
    public boolean depositToBank(String currency, double amount) {
        if (takeFromWallet(currency, amount)) {
            depositToBankDirect(currency, amount);
            return true;
        }
        return false; // Insufficient wallet funds
    }

    public boolean withdrawFromBank(String currency, double amount) {
        if (withdrawFromBankDirect(currency, amount)) {
            giveToWallet(currency, amount);
            return true;
        }
        return false; // Insufficient bank funds
    }

    // Quest Data Getters/Setters
    public String getActiveQuestId() {
        return activeQuestId;
    }

    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
    }

    public Map<String, Object> getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(Map<String, Object> questProgress) {
        this.questProgress = questProgress;
    }

    public void updateQuestProgress(String key, Object value) {
        this.questProgress.put(key, value);
    }

    public Object getQuestProgressValue(String key) {
        return this.questProgress.get(key);
    }

    public int getQuestProgressInt(String key) {
        return ((Number) this.questProgress.getOrDefault(key, 0)).intValue();
    }

    public void incrementQuestProgress(String key, int amount) {
        this.questProgress.put(key, getQuestProgressInt(key) + amount);
    }

    public void clearActiveQuest() {
        this.activeQuestId = null;
        this.questProgress.clear();
    }

    // Loan Data Methods
    public List<Loan> getActiveLoans() {
        return activeLoans;
    }

    public void addLoan(Loan loan) {
        this.activeLoans.add(loan);
    }

    public void removeLoan(String loanId) {
        this.activeLoans.removeIf(loan -> loan.getLoanId().equals(loanId));
    }

    public Loan getLoanById(String loanId) {
        for (Loan loan : activeLoans) {
            if (loan.getLoanId().equals(loanId)) return loan;
        }
        return null;
    }

    // TODO: Add getters and setters for other data fields (loans, quests, shops)
}
