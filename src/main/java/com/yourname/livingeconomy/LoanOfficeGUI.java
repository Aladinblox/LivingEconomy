package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // For slot to loanId map

public class LoanOfficeGUI implements InventoryHolder {

    private final Inventory inventory;
    private final LivingEconomy plugin;
    private final Player player;
    private final ConfigManager configManager;
    private final PlayerData playerData;
    public final Map<Integer, String> slotToLoanIdMap = new HashMap<>(); // slot -> loanId for repayment

    public LoanOfficeGUI(LivingEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.configManager = plugin.getConfigManager();
        this.playerData = plugin.getDataManager().getPlayerData(player);
        // TODO: Lang file title
        this.inventory = Bukkit.createInventory(this, 54, "Bank: Loan Office"); // 6 rows
        initializeItems();
    }

    private void initializeItems() {
        // Info Item: Max Loan, Interest, Period
        ItemStack infoItem = createGuiItem(Material.BOOK, "§6Loan Information",
                "§eMax Loan Amount: §f" + String.format("%.2f", configManager.getMaxLoanAmount()),
                "§eDaily Interest Rate: §f" + String.format("%.2f%%", configManager.getLoanInterestRateDaily() * 100),
                "§eRepayment Period: §f" + configManager.getLoanRepaymentPeriodDays() + " days",
                "§eMax Active Loans: §f" + configManager.getMaxActiveLoans()
        );
        inventory.setItem(4, infoItem); // Top center

        // Request New Loan Item
        boolean canTakeMoreLoans = playerData.getActiveLoans().size() < configManager.getMaxActiveLoans();
        if (canTakeMoreLoans) {
            ItemStack requestLoanItem = createGuiItem(Material.EMERALD_BLOCK, "§aRequest New Loan",
                    "§7Click to apply for a new loan.");
            inventory.setItem(22, requestLoanItem); // Center
        } else {
            ItemStack maxLoansItem = createGuiItem(Material.REDSTONE_BLOCK, "§cMax Loans Reached",
                    "§7You cannot take any more loans at this time.",
                    "§7Repay an existing loan to free up a slot.");
            inventory.setItem(22, maxLoansItem);
        }


        // Display Active Loans (start from slot 27, row 4)
        List<Loan> activeLoans = playerData.getActiveLoans();
        if (activeLoans.isEmpty()) {
            ItemStack noLoansItem = createGuiItem(Material.GLASS_PANE, "§7No Active Loans", "§7You currently have no outstanding loans.");
            inventory.setItem(31, noLoansItem); // Approx center of loan area
        } else {
            int currentSlot = 27; // Start of 4th row
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Loan loan : activeLoans) {
                if (currentSlot >= 54) break; // Full inventory

                String dueDateString = sdf.format(new Date(loan.getDueDateMillis()));
                List<String> lore = new ArrayList<>(Arrays.asList(
                        "§ePrincipal: §f" + String.format("%.2f", loan.getPrincipalAmount()),
                        "§eAmount Due: §f" + String.format("%.2f", loan.getAmountDue()),
                        "§eDue Date: §f" + dueDateString,
                        loan.isOverdue() ? "§cOVERDUE!" : "§aStatus: Active"
                ));
                if (!loan.isRepaid()) { // Should always be true for active loans list
                    lore.add("");
                    lore.add("§bClick to Repay Loan");
                }

                ItemStack loanItem = createGuiItem(loan.isOverdue() ? Material.RED_WOOL : Material.GREEN_WOOL,
                        "§6Loan ID: §7" + loan.getLoanId().substring(0, 8) + "...",
                        lore.toArray(new String[0])
                );
                inventory.setItem(currentSlot, loanItem);
                slotToLoanIdMap.put(currentSlot, loan.getLoanId());
                currentSlot++;
            }
        }
         // Fill empty spots with glass panes
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
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
}
