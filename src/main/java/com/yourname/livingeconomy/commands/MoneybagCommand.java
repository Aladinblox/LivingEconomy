package com.yourname.livingeconomy.commands;

import com.yourname.livingeconomy.LivingEconomy;
import com.yourname.livingeconomy.PlayerData;
import com.yourname.livingeconomy.ConfigManager; // For currency details
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MoneybagCommand implements CommandExecutor {

    private final LivingEconomy plugin;
    private final ConfigManager configManager;

    public MoneybagCommand(LivingEconomy plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // TODO: Lang file
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getDataManager().getPlayerData(player);
        if (playerData == null) {
            // This should ideally not happen if DataManager is working correctly
            player.sendMessage(ChatColor.RED + "Your data could not be loaded. Please try re-logging.");
            return true;
        }

        // TODO: Lang file for header
        player.sendMessage(ChatColor.GOLD + "--- Your Moneybag ---");
        Map<String, Double> walletBalances = playerData.getWalletBalances();

        if (walletBalances.isEmpty()) {
            // TODO: Lang file
            player.sendMessage(ChatColor.GRAY + "Your moneybag is empty.");
            return true;
        }

        for (Map.Entry<String, Double> entry : walletBalances.entrySet()) {
            String currencyKey = entry.getKey();
            double amount = entry.getValue();

            Map<String, Object> currencyDetails = configManager.getCurrencyDetails(currencyKey);
            String currencyName = currencyKey;
            String currencySymbol = "";

            if (currencyDetails != null) {
                currencyName = (String) currencyDetails.getOrDefault("name", currencyKey);
                currencySymbol = (String) currencyDetails.getOrDefault("symbol", "");
            }

            // TODO: Lang file for format "money_format: "&e%amount% %currency_symbol% (%currency_name%)"
            String formattedAmount = String.format("%.2f", amount); // Format to 2 decimal places
            player.sendMessage(ChatColor.YELLOW + formattedAmount + " " + currencySymbol + ChatColor.GRAY + " (" + currencyName + ")");
        }
        return true;
    }
}
