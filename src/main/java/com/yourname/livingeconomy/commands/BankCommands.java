package com.yourname.livingeconomy.commands;

import com.yourname.livingeconomy.BankManager;
import com.yourname.livingeconomy.LivingEconomy;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankCommands implements CommandExecutor {

    private final LivingEconomy plugin;
    private final BankManager bankManager;
    // Temporary storage for /le definebank positions
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();


    public BankCommands(LivingEconomy plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("bank")) {
            if (!bankManager.isBankRegionDefined()) {
                player.sendMessage("§cThe bank has not been set up by an admin yet.");
                return true;
            }
            if (!bankManager.isLocationInBank(player.getLocation())) {
                // TODO: Get message from lang file
                player.sendMessage("§cYou are not inside a defined bank region.");
                return true;
            }
            if (bankManager.getBankerNPCUUID() == null) {
                 player.sendMessage("§cThe Banker NPC has not been set by an admin yet.");
                 return true;
            }
            // For now, /bank command directly opens GUI if inside region.
            // Interaction with NPC will be separate.
            plugin.getBankGUI(player).open(player); // Assumes a getter in main plugin class
            return true;
        }
        return false; // Should not happen if mapped correctly in plugin.yml & main class
    }
}
