package com.yourname.livingeconomy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final LivingEconomy plugin;
    private FileConfiguration mainConfig; // config.yml
    private FileConfiguration currenciesConfig;
    private FileConfiguration questsConfig;

    private File currenciesFile;
    private File questsFile;

    public ConfigManager(LivingEconomy plugin) {
        this.plugin = plugin;
        // config.yml is handled by JavaPlugin's getConfig(), saveDefaultConfig(), reloadConfig()
        // We just need to ensure defaults are there and provide access.
        plugin.saveDefaultConfig(); // Saves config.yml from JAR if it doesn't exist in plugin folder
        mainConfig = plugin.getConfig();
        loadDefaultMainConfigValues(); // Ensure our specific default values are present

        // Setup custom configs
        currenciesFile = new File(plugin.getDataFolder(), "currencies.yml");
        questsFile = new File(plugin.getDataFolder(), "quests.yml");

        saveDefaultCustomConfig(currenciesFile, "currencies.yml");
        saveDefaultCustomConfig(questsFile, "quests.yml");

        currenciesConfig = YamlConfiguration.loadConfiguration(currenciesFile);
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);

        loadDefaultCurrenciesConfigValues();
        loadDefaultQuestsConfigValues();
    }

    private void loadDefaultMainConfigValues() {
        mainConfig.addDefault("data.autosave_interval_minutes", 10);
        mainConfig.addDefault("economy.max_loan_amount", 10000.0);
        mainConfig.addDefault("economy.global_tax.rate_percentage", 1.5); // 1.5%
        mainConfig.addDefault("economy.global_tax.interval_hours", 24); // Once a day
        mainConfig.addDefault("economy.loan.interest_rate_percentage_daily", 1.0); // 1% per day
        mainConfig.addDefault("economy.loan.repayment_period_days", 7); // 7 days to repay
        mainConfig.addDefault("economy.loan.max_active_loans", 3); // Max concurrent loans player can have
        // Add more defaults as needed
        mainConfig.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void loadDefaultCurrenciesConfigValues() {
        if (!currenciesConfig.isSet("currencies")) {
            // Example default currency
            currenciesConfig.set("currencies.gold.name", "Gold Coin");
            currenciesConfig.set("currencies.gold.symbol", "G");
            currenciesConfig.set("currencies.gold.texture", "gold_coin.png"); // Example texture path
            // Another example
            currenciesConfig.set("currencies.silver.name", "Silver Piece");
            currenciesConfig.set("currencies.silver.symbol", "S");

            try {
                currenciesConfig.save(currenciesFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default currencies.yml", e);
            }
        }
    }

    private void loadDefaultQuestsConfigValues() {
        if (!questsConfig.isSet("quests")) {
            // Example default quest
            questsConfig.set("quests.miner_dream.type", "BREAK_BLOCK");
            questsConfig.set("quests.miner_dream.target.material", "DIAMOND_ORE");
            questsConfig.set("quests.miner_dream.target.amount", 5);
            questsConfig.set("quests.miner_dream.description", "Mine 5 Diamond Ore blocks.");
            questsConfig.set("quests.miner_dream.rewards.currency.gold.min", 50);
            questsConfig.set("quests.miner_dream.rewards.currency.gold.max", 100);
            questsConfig.set("quests.miner_dream.rewards.items", Collections.singletonList("IRON_PICKAXE:1")); // item_name:amount

            questsConfig.set("quests.monster_hunter.type", "KILL_MOB");
            questsConfig.set("quests.monster_hunter.target.type", "ZOMBIE");
            questsConfig.set("quests.monster_hunter.target.amount", 10);
            questsConfig.set("quests.monster_hunter.description", "Slay 10 Zombies.");
            questsConfig.set("quests.monster_hunter.rewards.currency.silver.min", 20);
            questsConfig.set("quests.monster_hunter.rewards.currency.silver.max", 40);

            try {
                questsConfig.save(questsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default quests.yml", e);
            }
        }
    }


    private void saveDefaultCustomConfig(File configFile, String resourcePath) {
        if (!configFile.exists()) {
            plugin.saveResource(resourcePath, false); // Saves from JAR to plugin folder
             plugin.getLogger().info(configFile.getName() + " not found, created from JAR defaults.");
        } else {
            // If the file exists but is empty, or we want to ensure it's populated
            // This part is tricky without overwriting user changes.
            // Best approach is to use addDefault and copyDefaults like for mainConfig
            // For now, saveResource handles initial creation. We'll add defaults programmatically.
            plugin.getLogger().info(configFile.getName() + " found, loading existing configuration.");
        }
    }

    public void reloadConfigs() {
        // Reload main config (config.yml)
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        loadDefaultMainConfigValues(); // Re-apply defaults in case new keys were added

        // Reload custom configs
        try {
            currenciesConfig = YamlConfiguration.loadConfiguration(currenciesFile);
            // If currencies.yml was deleted, saveResource won't recreate it automatically on load
            // So, we ensure it's there first.
            if (!currenciesFile.exists()) {
                plugin.saveResource("currencies.yml", false);
            }
            currenciesConfig = YamlConfiguration.loadConfiguration(currenciesFile);
            loadDefaultCurrenciesConfigValues();


            if (!questsFile.exists()) {
                plugin.saveResource("quests.yml", false);
            }
            questsConfig = YamlConfiguration.loadConfiguration(questsFile);
            loadDefaultQuestsConfigValues();

            plugin.getLogger().info("Configuration files reloaded.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configuration files", e);
        }
    }

    // Getters for configs
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getCurrenciesConfig() {
        return currenciesConfig;
    }

    public FileConfiguration getQuestsConfig() {
        return questsConfig;
    }

    // Example getters for specific values (can be expanded)
    public int getAutosaveIntervalMinutes() {
        return mainConfig.getInt("data.autosave_interval_minutes", 10);
    }

    public double getMaxLoanAmount() {
        return mainConfig.getDouble("economy.max_loan_amount", 10000.0);
    }

    public double getLoanInterestRateDaily() {
        return mainConfig.getDouble("economy.loan.interest_rate_percentage_daily", 1.0) / 100.0; // Convert to decimal e.g. 0.01
    }

    public int getLoanRepaymentPeriodDays() {
        return mainConfig.getInt("economy.loan.repayment_period_days", 7);
    }

    public int getMaxActiveLoans() {
        return mainConfig.getInt("economy.loan.max_active_loans", 3);
    }

    public Map<String, Object> getCurrencyDetails(String currencyKey) {
        if (currenciesConfig.isConfigurationSection("currencies." + currencyKey)) {
            return currenciesConfig.getConfigurationSection("currencies." + currencyKey).getValues(false);
        }
        return null; // Or return an empty map
    }

    public List<String> getDefinedCurrencyKeys() {
        if (currenciesConfig.isConfigurationSection("currencies")) {
            return List.copyOf(currenciesConfig.getConfigurationSection("currencies").getKeys(false));
        }
        return Collections.emptyList();
    }

    public Map<String, Object> getQuestDetails(String questKey) {
        if (questsConfig.isConfigurationSection("quests." + questKey)) {
            return questsConfig.getConfigurationSection("quests." + questKey).getValues(true); // true for deep copy
        }
        return null;
    }

    public List<String> getDefinedQuestKeys() {
        if (questsConfig.isConfigurationSection("quests")) {
            return List.copyOf(questsConfig.getConfigurationSection("quests").getKeys(false));
        }
        return Collections.emptyList();
    }
}
