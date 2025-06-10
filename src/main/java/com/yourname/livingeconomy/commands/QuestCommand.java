package com.yourname.livingeconomy.commands;

import com.yourname.livingeconomy.LivingEconomy;
import com.yourname.livingeconomy.PlayerData;
import com.yourname.livingeconomy.QuestBoothManager;
import com.yourname.livingeconomy.QuestManager;
import com.yourname.livingeconomy.DataManager; // Added import
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class QuestCommand implements CommandExecutor {

    private final LivingEconomy plugin;
    private final QuestManager questManager;
    private final QuestBoothManager questBoothManager; // To check if player is at their booth
    private final DataManager dataManager;

    public QuestCommand(LivingEconomy plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager(); // Assuming getter in main class
        this.questBoothManager = plugin.getQuestBoothManager(); // Assuming getter
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        Player player = (Player) sender;
        PlayerData playerData = dataManager.getPlayerData(player);
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Your data is not loaded. Please re-log.");
            return true;
        }

        if (args.length == 0) { // Equivalent to /quests
            displayCurrentQuest(player, playerData);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "info":
            case "progress":
                displayCurrentQuest(player, playerData);
                break;
            case "get":
            case "new":
                handleGetNewQuest(player, playerData);
                break;
            // case "abandon": // TODO: Implement quest abandonment
            //    player.sendMessage(ChatColor.YELLOW + "Quest abandonment is not yet implemented.");
            //    break;
            default:
                // TODO: Lang file for help
                player.sendMessage(ChatColor.RED + "Unknown quest command. Usage: /quests [get]");
                break;
        }
        return true;
    }

    private void displayCurrentQuest(Player player, PlayerData playerData) {
        String activeQuestId = playerData.getActiveQuestId();
        if (activeQuestId == null) {
            // TODO: Lang file
            player.sendMessage(ChatColor.YELLOW + "You do not have an active quest. Visit your booth to get one with /quest get!");
            return;
        }

        Map<String, Object> questDetails = questManager.getQuestDetails(activeQuestId);
        if (questDetails == null) {
            player.sendMessage(ChatColor.RED + "Error: Could not load details for your active quest ID: " + activeQuestId);
            return;
        }

        String questName = (String) questDetails.getOrDefault("name", activeQuestId); // Assuming quests might have display name
        String description = (String) questDetails.getOrDefault("description", "No description provided.");

        // TODO: Lang file
        player.sendMessage(ChatColor.GOLD + "--- Your Active Quest ---");
        player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + questName);
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + description);

        String progressString = questManager.getFormattedQuestProgress(player, playerData, activeQuestId);
        player.sendMessage(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + progressString);
    }

    private void handleGetNewQuest(Player player, PlayerData playerData) {
        Location assignedBoothLoc = questBoothManager.getPlayerAssignedBoothLocation(player);
        if (assignedBoothLoc == null) {
            // TODO: Lang file (from QuestBoothManager too)
            player.sendMessage(ChatColor.RED + "You have not been assigned a quest booth yet. Please enter the bank area to be assigned one.");
            return;
        }

        // Check if player is near their booth (e.g., within 3 blocks)
        if (player.getWorld().equals(assignedBoothLoc.getWorld()) && player.getLocation().distanceSquared(assignedBoothLoc) <= 9) { // 3*3 = 9
            questManager.assignNewQuest(player);
        } else {
            // TODO: Lang file
            player.sendMessage(ChatColor.RED + "You must be at your assigned quest booth to get a new quest.");
            player.sendMessage(ChatColor.GRAY + "Your booth ("+questBoothManager.getPlayerAssignedBoothId(player)+") is at: " +
                                String.format("%.1f, %.1f, %.1f in %s", assignedBoothLoc.getX(), assignedBoothLoc.getY(), assignedBoothLoc.getZ(), assignedBoothLoc.getWorld().getName()));
        }
    }
}
