package com.yourname.livingeconomy.listeners;

import com.yourname.livingeconomy.LivingEconomy;
import com.yourname.livingeconomy.PlayerData;
import com.yourname.livingeconomy.QuestManager;
import com.yourname.livingeconomy.DataManager; // Added import
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class QuestEventListener implements Listener {

    private final LivingEconomy plugin;
    private final QuestManager questManager;
    private final DataManager dataManager;

    public QuestEventListener(LivingEconomy plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager();
        this.dataManager = plugin.getDataManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        PlayerData playerData = dataManager.getPlayerData(player);
        if (playerData == null || playerData.getActiveQuestId() == null) return;

        questManager.handleProgress(player, playerData, "BREAK_BLOCK", material.name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller(); // This can be null if not killed by a player

        if (killer == null) return;

        PlayerData playerData = dataManager.getPlayerData(killer);
        if (playerData == null || playerData.getActiveQuestId() == null) return;

        String entityTypeName = entity.getType().name();
        questManager.handleProgress(killer, playerData, "KILL_MOB", entityTypeName, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack craftedItem = event.getRecipe().getResult();
        Material itemType = craftedItem.getType();
        int amountCrafted = craftedItem.getAmount(); // Default, shift-click can vary

        // Adjust amount for shift-clicking (simplified check)
        if (event.isShiftClick()) {
            int maxCraftable = 0;
            int itemsChecked = 0;
            int smallestStack = Integer.MAX_VALUE;
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (item != null && item.getType() != Material.AIR) {
                    smallestStack = Math.min(smallestStack, item.getAmount());
                    itemsChecked++;
                }
            }
            if(itemsChecked > 0) maxCraftable = smallestStack * craftedItem.getAmount();

            //This is a basic way, real shift click calculation is more complex
            //and depends on inventory space, for now we'll take the result amount
            //or a simple multiplier if matrix has items.
             if (maxCraftable > 0 && itemsChecked > 0) { // A basic heuristic
                 // True calculation is complex, Spigot doesn't give direct amount for shift-click post-event.
                 // We will use the crafted item's amount, and if shift click, assume they craft as many as the smallest ingredient stack allows for one result.
                 // This is not perfect but a common workaround.
                 // Let's assume for now `craftedItem.getAmount()` is the amount for ONE craft operation.
                 // If they shift-clicked, they are trying to make many.
                 // We will count each click as one "operation" for simplicity or use a fixed multiplier.
                 // For now, we use `craftedItem.getAmount()` as the quantity for this single craft event.
             }
        }


        PlayerData playerData = dataManager.getPlayerData(player);
        if (playerData == null || playerData.getActiveQuestId() == null) return;

        questManager.handleProgress(player, playerData, "CRAFT_ITEM", itemType.name(), amountCrafted);
    }
    // TODO: Add FARM (PlayerHarvestBlockEvent might be better for some crops)
}
