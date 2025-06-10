package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BankGUI implements InventoryHolder {

    private final Inventory inventory;
    private final LivingEconomy plugin;

    public BankGUI(LivingEconomy plugin, Player player) {
        this.plugin = plugin;
        // TODO: Get title from language file
        this.inventory = Bukkit.createInventory(this, 27, "Player Bank"); // 3 rows
        initializeItems(player);
    }

    private void initializeItems(Player player) {
        // Placeholder items - functionality to be added later
        inventory.setItem(10, createGuiItem(Material.GOLD_INGOT, "§6Deposit/Withdraw", "§7Manage your currencies."));
        inventory.setItem(13, createGuiItem(Material.WRITABLE_BOOK, "§bLoan Office", "§7View and manage loans."));
        inventory.setItem(16, createGuiItem(Material.PAPER, "§eBalance Overview", "§7See a summary of your assets."));

        // Fill empty spots with glass panes for aesthetics
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
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

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
