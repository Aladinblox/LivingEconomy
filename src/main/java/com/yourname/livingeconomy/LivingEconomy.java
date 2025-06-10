package com.yourname.livingeconomy;

import org.bukkit.plugin.java.JavaPlugin;
import com.yourname.livingeconomy.DataManager;
import com.yourname.livingeconomy.ConfigManager;
import com.yourname.livingeconomy.BankManager;
import com.yourname.livingeconomy.QuestBoothManager;
import com.yourname.livingeconomy.QuestManager;
import com.yourname.livingeconomy.LoanManager;
import com.yourname.livingeconomy.BankGUI;
import com.yourname.livingeconomy.commands.BankCommands;
import com.yourname.livingeconomy.commands.QuestCommand;
import com.yourname.livingeconomy.commands.LECoreAdminCommands;
import com.yourname.livingeconomy.commands.MoneybagCommand;
import com.yourname.livingeconomy.listeners.BankInteractionListener;
import com.yourname.livingeconomy.listeners.QuestEventListener;
import org.bukkit.entity.Player; // Added for getBankGUI

public class LivingEconomy extends JavaPlugin {

    private DataManager dataManager;
    private ConfigManager configManager;
    private BankManager bankManager;
    private QuestBoothManager questBoothManager;
    private QuestManager questManager;
    private LoanManager loanManager;

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BankGUI getBankGUI(Player player) {
        return new BankGUI(this, player);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public QuestBoothManager getQuestBoothManager() {
        return questBoothManager;
    }

    @Override
    public void onEnable() {
        getLogger().info("LivingEconomy has been enabled!");

        // Initialize ConfigManager
        this.configManager = new ConfigManager(this);

        // Initialize BankManager
        this.bankManager = new BankManager(this, configManager);

        // Initialize QuestBoothManager
        this.questBoothManager = new QuestBoothManager(this, bankManager);

        // Initialize QuestManager
        this.questManager = new QuestManager(this);

        // Initialize LoanManager
        this.loanManager = new LoanManager(this);

        // Initialize DataManager
        this.dataManager = new DataManager(this);

        // Register Commands
        getCommand("bank").setExecutor(new BankCommands(this, bankManager));
        getCommand("le").setExecutor(new LECoreAdminCommands(this, bankManager, questBoothManager));
        getCommand("moneybag").setExecutor(new MoneybagCommand(this));
        getCommand("quests").setExecutor(new QuestCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new BankInteractionListener(this, bankManager), this);
        getServer().getPluginManager().registerEvents(questBoothManager, this); // QuestBoothManager is a Listener
        getServer().getPluginManager().registerEvents(new QuestEventListener(this), this);
        // DataManager is already a listener and registers itself.

        // Create default config files and directories if they don't exist
        // saveDefaultConfig(); // This is now handled by ConfigManager's constructor
        // ConfigManager now handles creation of config.yml, currencies.yml, quests.yml
        // and ensures their default values are loaded/copied from JAR.
        // Language files are also expected to be in JAR and can be managed similarly if needed,
        // but for now, en_US.yml is created by ConfigManager if not present (via saveResource call indirectly or directly).
        // The initial setup also created LivingEconomy/languages/en_US.yml which might be redundant if
        // LanguageManager takes over. For now, ConfigManager ensures defaults for core configs.

        getLogger().info("Default configurations initialized by ConfigManager.");
        getLogger().info("Default directories and files are being ensured by the plugin structure and managers.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LivingEconomy has been disabled.");
        // Shutdown DataManager
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
