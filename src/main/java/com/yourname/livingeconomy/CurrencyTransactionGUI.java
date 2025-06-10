package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.conversations.*; // For chat input

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; // Added HashMap import
import java.util.List;
import java.util.Map;

public class CurrencyTransactionGUI implements InventoryHolder {

    private final Inventory inventory;
    private final LivingEconomy plugin;
    private final Player player;
    private final ConfigManager configManager;
    private final PlayerData playerData;
    private final Map<Integer, String> slotToCurrencyKeyMap = new HashMap<>(); // slot -> currencyKey

    public enum TransactionType { DEPOSIT, WITHDRAW }

    public CurrencyTransactionGUI(LivingEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.configManager = plugin.getConfigManager();
        this.playerData = plugin.getDataManager().getPlayerData(player);
        // TODO: Lang file for title
        this.inventory = Bukkit.createInventory(this, 54, "Bank: Deposit/Withdraw Currencies");
        initializeItems();
    }

    private void initializeItems() {
        List<String> currencyKeys = configManager.getDefinedCurrencyKeys();
        if (currencyKeys.isEmpty()) {
            inventory.setItem(22, createGuiItem(Material.BARRIER, "§cNo Currencies Defined", "§7Ask an admin to configure currencies."));
            return;
        }

        int slot = 0;
        for (String currencyKey : currencyKeys) {
            if (slot >= 54) break; // Max inventory size

            Map<String, Object> details = configManager.getCurrencyDetails(currencyKey);
            String name = (String) details.getOrDefault("name", currencyKey);
            String symbol = (String) details.getOrDefault("symbol", "");
            // String texturePath = (String) details.get("texture"); // For custom item textures later

            // TODO: Use texturePath to make custom item if available, else use GOLD_NUGGET or similar
            Material displayMaterial = Material.GOLD_NUGGET;

            ItemStack currencyItem = createGuiItem(displayMaterial, "§6" + name + " (" + symbol + ")",
                "§7Bank: §e" + String.format("%.2f", playerData.getBankBalance(currencyKey)),
                "§7Wallet: §e" + String.format("%.2f", playerData.getWalletBalance(currencyKey)),
                "",
                "§aLeft-Click to Deposit",
                "§cRight-Click to Withdraw"
            );
            inventory.setItem(slot, currencyItem);
            slotToCurrencyKeyMap.put(slot, currencyKey);
            slot++;
        }
    }

    protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getCurrencyKeyForSlot(int slot) {
        return slotToCurrencyKeyMap.get(slot);
    }

    public void startTransaction(String currencyKey, TransactionType type) {
        player.closeInventory(); // Close GUI for chat input
        // TODO: Lang file
        String promptMsg = "§6Enter amount of " + currencyKey + " to " + type.toString().toLowerCase() + ": (or type 'cancel')";

        ConversationFactory factory = new ConversationFactory(plugin);
        Conversation conv = factory.withFirstPrompt(new NumericPrompt() {
            @Override
            public String getPromptText(ConversationContext context) {
                return promptMsg;
            }

            @Override
            protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                double amount = input.doubleValue();
                if (amount <= 0) {
                    context.getForWhom().sendRawMessage("§cAmount must be positive.");
                    return this; // Re-prompt
                }

                boolean success = false;
                if (type == TransactionType.DEPOSIT) {
                    success = playerData.depositToBank(currencyKey, amount);
                    if (success) {
                        context.getForWhom().sendRawMessage("§aSuccessfully deposited " + amount + " " + currencyKey + ".");
                    } else {
                        context.getForWhom().sendRawMessage("§cNot enough " + currencyKey + " in your wallet to deposit.");
                    }
                } else { // WITHDRAW
                    success = playerData.withdrawFromBank(currencyKey, amount);
                     if (success) {
                        context.getForWhom().sendRawMessage("§aSuccessfully withdrew " + amount + " " + currencyKey + ".");
                    } else {
                        context.getForWhom().sendRawMessage("§cNot enough " + currencyKey + " in your bank to withdraw.");
                    }
                }
                if (success) playerData.save(); // Save data after successful transaction
                // Re-open main bank GUI or currency transaction GUI after completion? For now, just message.
                // plugin.getBankGUI(player).open(player); // Example: re-open main bank GUI
                return Prompt.END_OF_CONVERSATION;
            }

            @Override
            protected String getFailedValidationText(ConversationContext context, String invalidInput) {
                if (invalidInput.equalsIgnoreCase("cancel")) {
                    return null; // Ends conversation
                }
                return "§cInvalid number: " + invalidInput + ". Please enter a valid amount or 'cancel'.";
            }

            @Override
            protected boolean isInputValid(ConversationContext context, String input) {
                 if (input.equalsIgnoreCase("cancel")) {
                    return true;
                }
                try {
                    double val = Double.parseDouble(input);
                    return val > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

        }).withLocalEcho(false)
          .withEscapeSequence("cancel")
          .withTimeout(30) // seconds
          .thatExcludesNonPlayersWithMessage("Console cannot perform transactions.")
          .addConversationAbandonedListener(event -> {
              if (event.gracefulExit()) {
                  // player.sendMessage("Transaction complete or cancelled.");
              } else {
                  player.sendMessage("§cTransaction timed out or was abandoned.");
              }
              // Optionally re-open a GUI here
              // plugin.getBankGUI(player).open(player);
          })
          .buildConversation(player);
        conv.begin();
    }
}
