package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class QuestBoothManager implements Listener {

    private final LivingEconomy plugin;
    private final BankManager bankManager;
    private File boothsFile;
    private FileConfiguration boothsConfig;

    // Booth data
    private final Map<String, Location> definedBooths = new HashMap<>(); // boothId -> Location
    private final Map<UUID, String> playerAssignedBooths = new HashMap<>(); // playerUUID -> boothId
    private final Set<String> occupiedBoothIds = new HashSet<>(); // Set of boothIds currently in use

    public QuestBoothManager(LivingEconomy plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;

        boothsFile = new File(plugin.getDataFolder(), "bank_booths.yml");
        if (!boothsFile.exists()) {
            try {
                boothsFile.createNewFile();
                plugin.getLogger().info("Created bank_booths.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create bank_booths.yml", e);
            }
        }
        boothsConfig = YamlConfiguration.loadConfiguration(boothsFile);
        loadBooths();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadBooths() {
        definedBooths.clear();
        if (boothsConfig.isConfigurationSection("booths")) {
            for (String boothId : boothsConfig.getConfigurationSection("booths").getKeys(false)) {
                String path = "booths." + boothId;
                String worldName = boothsConfig.getString(path + ".world");
                if (worldName == null) {
                    plugin.getLogger().warning("Booth " + boothId + " is missing world information in bank_booths.yml. Skipping.");
                    continue;
                }
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World '" + worldName + "' for booth " + boothId + " not found. Skipping booth.");
                    continue;
                }
                Location loc = new Location(world,
                        boothsConfig.getDouble(path + ".x"),
                        boothsConfig.getDouble(path + ".y"),
                        boothsConfig.getDouble(path + ".z"),
                        (float) boothsConfig.getDouble(path + ".yaw", 0.0),
                        (float) boothsConfig.getDouble(path + ".pitch", 0.0)
                );
                definedBooths.put(boothId, loc);
            }
        }
        plugin.getLogger().info("Loaded " + definedBooths.size() + " quest booths from bank_booths.yml.");
    }

    public void saveBooths() {
        // Clear existing booth data to prevent duplicates if IDs change (though they shouldn't)
        boothsConfig.set("booths", null);
        for (Map.Entry<String, Location> entry : definedBooths.entrySet()) {
            String boothId = entry.getKey();
            Location loc = entry.getValue();
            String path = "booths." + boothId;
            boothsConfig.set(path + ".world", loc.getWorld().getName());
            boothsConfig.set(path + ".x", loc.getX());
            boothsConfig.set(path + ".y", loc.getY());
            boothsConfig.set(path + ".z", loc.getZ());
            boothsConfig.set(path + ".yaw", loc.getYaw());
            boothsConfig.set(path + ".pitch", loc.getPitch());
        }
        try {
            boothsConfig.save(boothsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save bank_booths.yml", e);
        }
    }

    public void reloadBooths() {
        boothsFile = new File(plugin.getDataFolder(), "bank_booths.yml");
         if (!boothsFile.exists()) {
            try {
                boothsFile.createNewFile();
            } catch (IOException e) {
                 plugin.getLogger().log(Level.SEVERE, "Could not create bank_booths.yml on reload", e);
                 return;
            }
        }
        boothsConfig = YamlConfiguration.loadConfiguration(boothsFile);
        loadBooths();
    }


    public boolean addBooth(String boothId, Location location) {
        if (definedBooths.containsKey(boothId)) {
            return false; // Booth ID already exists
        }
        definedBooths.put(boothId, location);
        saveBooths();
        plugin.getLogger().info("Added new quest booth: " + boothId + " at " + location.toString());
        return true;
    }

    public boolean removeBooth(String boothId) {
        if (!definedBooths.containsKey(boothId)) {
            return false; // Booth ID doesn't exist
        }
        definedBooths.remove(boothId);
        // Make sure no player is currently assigned to this booth if it's removed
        playerAssignedBooths.values().removeIf(id -> id.equals(boothId));
        occupiedBoothIds.remove(boothId);
        saveBooths();
        plugin.getLogger().info("Removed quest booth: " + boothId);
        return true;
    }

    public Map<String, Location> getDefinedBooths() {
        return new HashMap<>(definedBooths); // Return a copy
    }

    public Location getPlayerAssignedBoothLocation(Player player) {
        String boothId = playerAssignedBooths.get(player.getUniqueId());
        if (boothId != null) {
            return definedBooths.get(boothId);
        }
        return null;
    }

    public String getPlayerAssignedBoothId(Player player) {
        return playerAssignedBooths.get(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Player hasn't moved to a new block
        }

        if (bankManager.isLocationInBank(player.getLocation())) {
            if (!playerAssignedBooths.containsKey(player.getUniqueId())) {
                assignBoothToPlayer(player);
            }
        }
        // Note: Booths are not unassigned when leaving the bank region, only on player quit.
    }

    // Also check on join in case player logs in inside bank
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Attempt to reassign if they had one or if they are in bank
        // For now, session-based means new assignment on join if in bank
        if (bankManager.isLocationInBank(player.getLocation())) {
             if (!playerAssignedBooths.containsKey(player.getUniqueId())) { // Should be true on join
                assignBoothToPlayer(player);
            }
        }
    }


    private void assignBoothToPlayer(Player player) {
        if (!bankManager.isBankRegionDefined()) {
            // plugin.getLogger().fine("Cannot assign booth to " + player.getName() + ", bank region not defined.");
            return; // No bank, no booths to assign
        }

        for (Map.Entry<String, Location> entry : definedBooths.entrySet()) {
            String boothId = entry.getKey();
            if (!occupiedBoothIds.contains(boothId)) {
                playerAssignedBooths.put(player.getUniqueId(), boothId);
                occupiedBoothIds.add(boothId);
                // TODO: lang file message
                player.sendMessage("§aYou have been assigned Quest Booth: §e" + boothId);
                plugin.getLogger().info("Assigned booth " + boothId + " to player " + player.getName());
                // TODO: Later, place sign/NPC here using entry.getValue() (the booth's location)
                return;
            }
        }
        // TODO: lang file message
        player.sendMessage("§cAll quest booths are currently occupied. Please try again later.");
        plugin.getLogger().info("Could not assign booth to " + player.getName() + ", all booths occupied.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String assignedBoothId = playerAssignedBooths.remove(player.getUniqueId());
        if (assignedBoothId != null) {
            occupiedBoothIds.remove(assignedBoothId);
            plugin.getLogger().info("Booth " + assignedBoothId + " is now available (player " + player.getName() + " left).");
        }
    }
}
