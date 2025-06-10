package com.yourname.livingeconomy;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.google.gson.Gson; // For parsing complex item meta if needed, or use Bukkit methods
import com.google.gson.reflect.TypeToken; // For parsing complex item meta
import java.lang.reflect.Type;

public class QuestManager {

    private final LivingEconomy plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final Random random = new Random();

    public QuestManager(LivingEconomy plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    public boolean assignNewQuest(Player player) {
        PlayerData playerData = dataManager.getPlayerData(player);
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Your data is not loaded. Please try again.");
            return false;
        }

        if (playerData.getActiveQuestId() != null) {
            player.sendMessage(ChatColor.YELLOW + "You already have an active quest: " + playerData.getActiveQuestId());
            // TODO: Lang file for "use /quests to see progress"
            player.sendMessage(ChatColor.YELLOW + "Use /quests to see your progress or /quest abandon (TODO) to get a new one.");
            return false;
        }

        List<String> availableQuestKeys = configManager.getDefinedQuestKeys();
        if (availableQuestKeys.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No quests are defined by the server admin yet.");
            return false;
        }

        // Simple random assignment for now.
        // TODO: Add logic for uniqueness or prerequisites if needed later.
        String newQuestId = availableQuestKeys.get(random.nextInt(availableQuestKeys.size()));

        playerData.setActiveQuestId(newQuestId);
        playerData.getQuestProgress().clear(); // Clear any old progress for the new quest

        Map<String, Object> questDetails = getQuestDetails(newQuestId);
        if (questDetails == null) {
            player.sendMessage(ChatColor.RED + "Error assigning quest: Could not load quest details for " + newQuestId);
            playerData.clearActiveQuest();
            return false;
        }

        // Initialize progress based on quest type if needed (e.g. set targets)
        // For now, progress map starts empty and specific listeners will populate it.
        // Example: if type is BREAK_BLOCK, store target material and amount needed.
        // This information is already in quests.yml, QuestManager can fetch it.

        playerData.save(); // Save immediately after assigning a new quest

        // TODO: Lang file
        String questName = (String) questDetails.getOrDefault("name", newQuestId); // Assuming quests might have a display name
        String description = (String) questDetails.getOrDefault("description", "No description.");
        player.sendMessage(ChatColor.GREEN + "New Quest Assigned: " + ChatColor.YELLOW + questName);
        player.sendMessage(ChatColor.GRAY + description);
        return true;
    }

    public Map<String, Object> getQuestDetails(String questId) {
        if (questId == null) return null;
        return configManager.getQuestDetails(questId);
    }

    public String getFormattedQuestProgress(Player player, PlayerData playerData, String questId) {
        Map<String, Object> questDetails = getQuestDetails(questId);
        if (questDetails == null) {
            return ChatColor.RED + "Unknown quest ID: " + questId;
        }

        String type = (String) questDetails.get("type");
        Map<String, Object> target = (Map<String, Object>) questDetails.get("target");
        if (type == null || target == null) {
            return ChatColor.RED + "Quest '" + questId + "' is improperly configured (missing type or target).";
        }

        int requiredAmount = ((Number) target.getOrDefault("amount", 0)).intValue();
        String progressKey = ""; // This key needs to match what event listeners will use to save progress
        String targetName = "";

        switch (type.toUpperCase()) {
            case "BREAK_BLOCK":
                targetName = (String) target.getOrDefault("material", "UNKNOWN_MATERIAL");
                progressKey = targetName + "_broken"; // e.g., DIAMOND_ORE_broken
                break;
            case "KILL_MOB":
                targetName = (String) target.getOrDefault("entity_type", "UNKNOWN_MOB");
                progressKey = targetName + "_killed"; // e.g., ZOMBIE_killed
                break;
            case "CRAFT_ITEM":
                targetName = (String) target.getOrDefault("material", "UNKNOWN_ITEM"); // Or "item" if more complex
                progressKey = targetName + "_crafted"; // e.g., BREAD_crafted
                break;
            // TODO: Add FARM, FISH, TRAVEL etc.
            default:
                return ChatColor.YELLOW + "Progress tracking for quest type '" + type + "' is not fully implemented yet.";
        }

        int currentAmount = playerData.getQuestProgressInt(progressKey);
        return String.format("%s: %d / %d", targetName, currentAmount, requiredAmount);
    }

    public void handleProgress(Player player, PlayerData playerData, String eventQuestType, String eventTargetName, int amount) {
        String activeQuestId = playerData.getActiveQuestId();
        if (activeQuestId == null) return;

        Map<String, Object> questDetails = getQuestDetails(activeQuestId);
        if (questDetails == null) return;

        String questType = (String) questDetails.get("type");
        Map<String, Object> questTarget = (Map<String, Object>) questDetails.get("target");

        if (questTarget == null || !eventQuestType.equalsIgnoreCase(questType)) {
            return;
        }

        String targetMaterialOrEntity = "";
        String progressKey = "";

        switch (questType.toUpperCase()) {
            case "BREAK_BLOCK":
                targetMaterialOrEntity = (String) questTarget.get("material");
                progressKey = targetMaterialOrEntity + "_broken";
                break;
            case "KILL_MOB":
                targetMaterialOrEntity = (String) questTarget.get("entity_type");
                progressKey = targetMaterialOrEntity + "_killed";
                break;
            case "CRAFT_ITEM":
                targetMaterialOrEntity = (String) questTarget.get("material"); // Can be item name too
                progressKey = targetMaterialOrEntity + "_crafted";
                break;
            default:
                return; // Unsupported quest type for this handler
        }

        if (!eventTargetName.equalsIgnoreCase(targetMaterialOrEntity)) {
            return; // Event target does not match quest target
        }

        playerData.incrementQuestProgress(progressKey, amount);
        int currentAmount = playerData.getQuestProgressInt(progressKey);
        int requiredAmount = ((Number) questTarget.getOrDefault("amount", 0)).intValue();

        // TODO: Lang file for progress update message (optional)
        // player.sendMessage(ChatColor.GREEN + "Quest progress: " + currentAmount + "/" + requiredAmount + " " + targetMaterialOrEntity);

        if (currentAmount >= requiredAmount) {
            completeQuest(player, playerData, activeQuestId);
        } else {
            playerData.save(); // Save progress if not completed yet
        }
    }

    public void completeQuest(Player player, PlayerData playerData, String questId) {
        Map<String, Object> questDetails = getQuestDetails(questId);
        if (questDetails == null) return;

        String questName = (String) questDetails.getOrDefault("name", questId);
        player.sendTitle(ChatColor.GOLD + "Quest Completed!", ChatColor.YELLOW + questName, 10, 70, 20);

        // Distribute Rewards
        if (questDetails.containsKey("rewards")) {
            Map<String, Object> rewards = (Map<String, Object>) questDetails.get("rewards");
            // Currency Rewards
            if (rewards.containsKey("currency")) {
                Map<String, Object> currencyRewards = (Map<String, Object>) rewards.get("currency");
                for (Map.Entry<String, Object> entry : currencyRewards.entrySet()) {
                    String currencyKey = entry.getKey();
                    double amountToGive;
                    if (entry.getValue() instanceof Map) { // Min/Max range
                        Map<String, Object> range = (Map<String, Object>) entry.getValue();
                        double min = ((Number) range.getOrDefault("min", 0.0)).doubleValue();
                        double max = ((Number) range.getOrDefault("max", 0.0)).doubleValue();
                        amountToGive = min + (random.nextDouble() * (max - min));
                    } else { // Fixed amount
                        amountToGive = ((Number) entry.getValue()).doubleValue();
                    }
                    playerData.depositToBankDirect(currencyKey, amountToGive); // Directly to bank
                    // TODO: Lang file for reward message
                    player.sendMessage(ChatColor.GREEN + "You received " + String.format("%.2f", amountToGive) + " " + currencyKey + " (Banked)." );
                }
            }
            // Item Rewards
            if (rewards.containsKey("items")) {
                List<String> itemRewardStrings = (List<String>) rewards.get("items");
                for (String itemString : itemRewardStrings) {
                    giveItemReward(player, itemString);
                }
            }
            // TODO: Experience rewards, Command rewards
        }

        playerData.clearActiveQuest();
        playerData.save(); // Save after completion and reward
        // TODO: Lang file for "return to booth for new quest"
        player.sendMessage(ChatColor.AQUA + "You can now get a new quest from your booth.");
    }

    private void giveItemReward(Player player, String itemString) {
        // Format: MATERIAL_NAME:amount[:{json_for_meta}] OR MATERIAL_NAME:amount[:display_name]
        // Example: DIAMOND_SWORD:1, COOKED_BEEF:16, STONE:1:{display:"Magic Stone",lore:["A very magic stone"]}
        String[] parts = itemString.split(":", 3);
        Material material = Material.matchMaterial(parts[0].toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Invalid material in item reward: " + parts[0]);
            return;
        }
        int amount = 1;
        if (parts.length > 1) amount = Integer.parseInt(parts[1]);

        ItemStack itemStack = new ItemStack(material, amount);
        if (parts.length > 2) {
            String metaString = parts[2];
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (metaString.startsWith("{")) { // Assume JSON for more complex meta
                    try {
                        Gson gson = new Gson();
                        Type type = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> metaMap = gson.fromJson(metaString, type);
                        if (metaMap.containsKey("display")) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (String)metaMap.get("display")));
                        if (metaMap.containsKey("lore")) {
                            List<String> loreLines = (List<String>)metaMap.get("lore");
                            List<String> coloredLore = new ArrayList<>();
                            for(String line : loreLines) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                            meta.setLore(coloredLore);
                        }
                        // TODO: Add enchantments, etc. from metaMap if needed
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error parsing item meta JSON: " + metaString + " Error: " + e.getMessage());
                    }
                } else { // Assume it is just a display name
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', metaString));
                }
                itemStack.setItemMeta(meta);
            }
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
        if (!leftover.isEmpty()) {
            // TODO: Lang file
            player.sendMessage(ChatColor.RED + "Some item rewards could not fit in your inventory and were dropped on the ground!");
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
        player.sendMessage(ChatColor.GREEN + "You received item: " + material.name() + " x" + amount);
    }
}
