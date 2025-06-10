package com.yourname.livingeconomy.listeners;

import com.yourname.livingeconomy.BankGUI;
import com.yourname.livingeconomy.CurrencyTransactionGUI;
import com.yourname.livingeconomy.LoanOfficeGUI;
import com.yourname.livingeconomy.Loan; // Also needed for type casting if we process here
import com.yourname.livingeconomy.BankManager;
import com.yourname.livingeconomy.LivingEconomy;
import com.yourname.livingeconomy.PlayerData; // Added for LoanOfficeGUI interaction
import org.bukkit.Material; // Added for LoanOfficeGUI interaction
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryHolder;

public class BankInteractionListener implements Listener {

    private final LivingEconomy plugin;
    private final BankManager bankManager;

    public BankInteractionListener(LivingEconomy plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (bankManager.getBankerNPCUUID() != null && clickedEntity.getUniqueId().equals(bankManager.getBankerNPCUUID())) {
            event.setCancelled(true); // Prevent default NPC interaction
            if (bankManager.isLocationInBank(player.getLocation()) || !bankManager.isBankRegionDefined()) { // Also allow if region not defined, for setup
                plugin.getBankGUI(player).open(player);
            } else {
                // TODO: lang file
                player.sendMessage("§cYou must be in the bank region to interact with the Banker.");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof BankGUI) {
            event.setCancelled(true);
            BankGUI bankGui = (BankGUI) holder;

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (displayName.contains("Deposit/Withdraw")) {
                new CurrencyTransactionGUI(plugin, player).open();
            } else if (displayName.contains("Loan Office")) {
                new LoanOfficeGUI(plugin, player).open();
            } else if (displayName.contains("Balance Overview")) {
                player.sendMessage("§eBalance Overview: Coming Soon!"); // Placeholder
            }
        } else if (holder instanceof CurrencyTransactionGUI) {
            event.setCancelled(true);
            CurrencyTransactionGUI currencyGui = (CurrencyTransactionGUI) holder;
            org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem(); // Fully qualify ItemStack

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String currencyKey = currencyGui.getCurrencyKeyForSlot(event.getRawSlot());
            if (currencyKey == null) return;

            if (event.isLeftClick()) { // Deposit
                currencyGui.startTransaction(currencyKey, CurrencyTransactionGUI.TransactionType.DEPOSIT);
            } else if (event.isRightClick()) { // Withdraw
                currencyGui.startTransaction(currencyKey, CurrencyTransactionGUI.TransactionType.WITHDRAW);
            }
        } else if (holder instanceof LoanOfficeGUI) {
            event.setCancelled(true);
            LoanOfficeGUI loanGui = (LoanOfficeGUI) holder;
            org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) return;

            String itemName = clickedItem.getItemMeta().getDisplayName();
            PlayerData pData = plugin.getDataManager().getPlayerData(player);

            if (itemName.contains("Request New Loan")) {
                // Start conversation for loan amount
                plugin.getLoanManager().requestLoanConversation(player, pData); // Assuming LoanManager exists
            } else if (itemName.contains("Loan ID")) {
                String loanId = loanGui.slotToLoanIdMap.get(event.getRawSlot());
                if (loanId != null) {
                    plugin.getLoanManager().repayLoanConversation(player, pData, loanId); // Assuming LoanManager exists
                }
            }
        }
    }
}
