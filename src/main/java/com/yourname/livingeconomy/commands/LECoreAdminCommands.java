package com.yourname.livingeconomy.commands;

import com.yourname.livingeconomy.BankManager;
import com.yourname.livingeconomy.QuestBoothManager;
import com.yourname.livingeconomy.LivingEconomy;
import org.bukkit.ChatColor;
import org.bukkit.World; // Added for booth commands
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LECoreAdminCommands implements CommandExecutor {

    private final LivingEconomy plugin;
    private final BankManager bankManager;
    private final QuestBoothManager questBoothManager;
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public LECoreAdminCommands(LivingEconomy plugin, BankManager bankManager, QuestBoothManager questBoothManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
        this.questBoothManager = questBoothManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("livingeconomy.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (subCommand) {
                case "definebank":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le definebank <pos1|pos2|save>");
                        return true;
                    }
                    String action = args[1].toLowerCase();
                    if (action.equals("pos1")) {
                        pos1Map.put(player.getUniqueId(), player.getLocation());
                        player.sendMessage(ChatColor.GREEN + "Bank position 1 set to your current location. Use /le definebank pos2 to set the second position.");
                    } else if (action.equals("pos2")) {
                        pos2Map.put(player.getUniqueId(), player.getLocation());
                        player.sendMessage(ChatColor.GREEN + "Bank position 2 set to your current location. Use /le definebank save to finalize.");
                    } else if (action.equals("save")) {
                        Location p1 = pos1Map.get(player.getUniqueId());
                        Location p2 = pos2Map.get(player.getUniqueId());
                        if (p1 == null || p2 == null) {
                            player.sendMessage(ChatColor.RED + "You must set both pos1 and pos2 before saving.");
                            return true;
                        }
                        if (!p1.getWorld().equals(p2.getWorld())) {
                            player.sendMessage(ChatColor.RED + "Both bank positions must be in the same world.");
                            return true;
                        }
                        bankManager.setBankPos1(p1);
                        bankManager.setBankPos2(p2);
                        bankManager.saveBankData();
                        pos1Map.remove(player.getUniqueId());
                        pos2Map.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Bank region successfully defined and saved!");
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le definebank <pos1|pos2|save>");
                    }
                    return true;
                case "setnpc":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le setnpc <banker | bounty_board_TODO>");
                        return true;
                    }
                    String npcType = args[1].toLowerCase();
                    if (npcType.equals("banker")) {
                        // Attempt to get the entity the player is looking at
                        Entity targetEntity = getTargetEntity(player, 5); // 5 block range
                        if (targetEntity != null && targetEntity.getType() != EntityType.PLAYER) { // Don't set player as NPC
                            bankManager.setBankerNPC(targetEntity.getUniqueId());
                            player.sendMessage(ChatColor.GREEN + "Banker NPC set to the entity you are looking at (UUID: " + targetEntity.getUniqueId() + ")");
                        } else {
                            player.sendMessage(ChatColor.RED + "Could not find a valid NPC to target. Look at an entity (not a player) within 5 blocks.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Unknown NPC type. Available: banker");
                    }
                    return true;
                 // ... other admin subcommands like reload, checkbal, give ...
                case "reload":
                    plugin.getConfigManager().reloadConfigs();
                    bankManager.loadBankData(); // Reload bank data after config reload
                    questBoothManager.reloadBooths();
                    // TODO: reload other managers if they cache config values
                    sender.sendMessage(ChatColor.GREEN + "LivingEconomy configurations reloaded.");
                    return true;
                case "addbooth":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le addbooth <boothID> [here|x y z world]");
                        return true;
                    }
                    String boothId = args[1];
                    Location boothLoc;
                    if (args.length == 2 || (args.length == 3 && args[2].equalsIgnoreCase("here"))) {
                        boothLoc = player.getLocation();
                    } else if (args.length == 6) {
                        try {
                            double x = Double.parseDouble(args[2]);
                            double y = Double.parseDouble(args[3]);
                            double z = Double.parseDouble(args[4]);
                            World world = org.bukkit.Bukkit.getWorld(args[5]); // Fully qualified
                            if (world == null) {
                                player.sendMessage(ChatColor.RED + "World '" + args[5] + "' not found.");
                                return true;
                            }
                            boothLoc = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Invalid coordinates.");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le addbooth <boothID> [here|x y z world]");
                        return true;
                    }
                    if (questBoothManager.addBooth(boothId, boothLoc)) {
                        player.sendMessage(ChatColor.GREEN + "Quest booth '" + boothId + "' added at " + String.format("%.1f, %.1f, %.1f in %s", boothLoc.getX(), boothLoc.getY(), boothLoc.getZ(), boothLoc.getWorld().getName()));
                    } else {
                        player.sendMessage(ChatColor.RED + "Quest booth ID '" + boothId + "' already exists.");
                    }
                    return true;
                case "removebooth":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /le removebooth <boothID>");
                        return true;
                    }
                    String boothIdToRemove = args[1];
                    if (questBoothManager.removeBooth(boothIdToRemove)) {
                        player.sendMessage(ChatColor.GREEN + "Quest booth '" + boothIdToRemove + "' removed.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Quest booth ID '" + boothIdToRemove + "' not found.");
                    }
                    return true;
                case "listbooths":
                    Map<String, Location> booths = questBoothManager.getDefinedBooths();
                    if (booths.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "No quest booths defined yet.");
                        return true;
                    }
                    player.sendMessage(ChatColor.GOLD + "--- Defined Quest Booths ---");
                    for (Map.Entry<String, Location> entry : booths.entrySet()) {
                        Location l = entry.getValue();
                        player.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + ChatColor.WHITE + String.format("%.1f, %.1f, %.1f @ %s (Yaw: %.1f)", l.getX(), l.getY(), l.getZ(), l.getWorld().getName(), l.getYaw()));
                    }
                    return true;
            }
        } else {
             if (subCommand.equals("reload")) {
                plugin.getConfigManager().reloadConfigs();
                bankManager.loadBankData();
                questBoothManager.reloadBooths();
                sender.sendMessage(ChatColor.GREEN + "LivingEconomy configurations reloaded from console.");
                return true;
            }
            sender.sendMessage("Some /le subcommands can only be run by a player.");
            return true;
        }


        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- LivingEconomy Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/le definebank <pos1|pos2|save>" + ChatColor.GRAY + " - Define bank region.");
        sender.sendMessage(ChatColor.YELLOW + "/le setnpc <banker>" + ChatColor.GRAY + " - Set an NPC's function.");
        sender.sendMessage(ChatColor.YELLOW + "/le reload" + ChatColor.GRAY + " - Reloads configuration files.");
        sender.sendMessage(ChatColor.YELLOW + "/le addbooth <id> [here|x y z world]" + ChatColor.GRAY + " - Add a quest booth.");
        sender.sendMessage(ChatColor.YELLOW + "/le removebooth <id>" + ChatColor.GRAY + " - Remove a quest booth.");
        sender.sendMessage(ChatColor.YELLOW + "/le listbooths" + ChatColor.GRAY + " - List defined quest booths.");
        // Add more help lines as commands are implemented
    }

    // Helper to get entity player is looking at.
    private Entity getTargetEntity(Player player, int range) {
        Entity found = null;
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            // Basic ray cast check (simplified)
            Location eye = player.getEyeLocation();
            Location targetLoc = entity.getLocation().add(0, entity.getHeight()/2, 0); // center of entity
            if (player.hasLineOfSight(entity) && eye.toVector().distanceSquared(targetLoc.toVector()) < range*range) {
                 // A more precise check would involve iterating along the sight vector.
                 // For simplicity, if line of sight is true and it's close, assume it's the target.
                 // This can be improved with more complex ray tracing if needed.
                if (found == null || eye.toVector().distanceSquared(targetLoc.toVector()) < eye.toVector().distanceSquared(found.getLocation().add(0,found.getHeight()/2,0).toVector())) {
                    found = entity;
                }
            }
        }
        return found;
    }
}
