package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class BankManager {

    private final LivingEconomy plugin;
    private final ConfigManager configManager;
    private Location bankPos1;
    private Location bankPos2;
    private UUID bankerNPC_UUID; // Store UUID of the Banker NPC

    public BankManager(LivingEconomy plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadBankData();
    }

    public void loadBankData() {
        // Load bank region
        if (configManager.getMainConfig().isConfigurationSection("bank.region")) {
            String worldName1 = configManager.getMainConfig().getString("bank.region.pos1.world");
            if (worldName1 != null) {
                World world1 = Bukkit.getWorld(worldName1);
                if (world1 != null) {
                    bankPos1 = new Location(world1,
                            configManager.getMainConfig().getDouble("bank.region.pos1.x"),
                            configManager.getMainConfig().getDouble("bank.region.pos1.y"),
                            configManager.getMainConfig().getDouble("bank.region.pos1.z"));
                } else {
                    plugin.getLogger().warning("Bank region pos1 world '" + worldName1 + "' not found!");
                }
            }

            String worldName2 = configManager.getMainConfig().getString("bank.region.pos2.world");
            if (worldName2 != null) {
                 World world2 = Bukkit.getWorld(worldName2);
                 if (world2 != null) {
                    bankPos2 = new Location(world2,
                            configManager.getMainConfig().getDouble("bank.region.pos2.x"),
                            configManager.getMainConfig().getDouble("bank.region.pos2.y"),
                            configManager.getMainConfig().getDouble("bank.region.pos2.z"));
                 } else {
                    plugin.getLogger().warning("Bank region pos2 world '" + worldName2 + "' not found!");
                 }
            }
            if (bankPos1 != null && bankPos2 != null) {
                 plugin.getLogger().info("Bank region loaded.");
            }
        } else {
            plugin.getLogger().info("Bank region not defined yet.");
        }

        // Load Banker NPC UUID
        String bankerUUIDString = configManager.getMainConfig().getString("bank.banker_npc_uuid");
        if (bankerUUIDString != null && !bankerUUIDString.isEmpty()) {
            try {
                bankerNPC_UUID = UUID.fromString(bankerUUIDString);
                plugin.getLogger().info("Banker NPC UUID loaded: " + bankerNPC_UUID);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid Banker NPC UUID in config: " + bankerUUIDString);
            }
        } else {
             plugin.getLogger().info("Banker NPC not set yet.");
        }
    }

    public void saveBankData() {
        // Save bank region
        if (bankPos1 != null) {
            configManager.getMainConfig().set("bank.region.pos1.world", bankPos1.getWorld().getName());
            configManager.getMainConfig().set("bank.region.pos1.x", bankPos1.getX());
            configManager.getMainConfig().set("bank.region.pos1.y", bankPos1.getY());
            configManager.getMainConfig().set("bank.region.pos1.z", bankPos1.getZ());
        }
        if (bankPos2 != null) {
            configManager.getMainConfig().set("bank.region.pos2.world", bankPos2.getWorld().getName());
            configManager.getMainConfig().set("bank.region.pos2.x", bankPos2.getX());
            configManager.getMainConfig().set("bank.region.pos2.y", bankPos2.getY());
            configManager.getMainConfig().set("bank.region.pos2.z", bankPos2.getZ());
        }

        // Save Banker NPC UUID
        if (bankerNPC_UUID != null) {
            configManager.getMainConfig().set("bank.banker_npc_uuid", bankerNPC_UUID.toString());
        } else {
            configManager.getMainConfig().set("bank.banker_npc_uuid", null); // Remove if not set
        }
        plugin.saveConfig(); // Save changes to config.yml
        plugin.getLogger().info("Bank data saved to config.yml");
    }

    public void setBankPos1(Location pos1) {
        this.bankPos1 = pos1;
        plugin.getLogger().info("Bank position 1 set to: " + pos1.toString());
    }

    public void setBankPos2(Location pos2) {
        this.bankPos2 = pos2;
        plugin.getLogger().info("Bank position 2 set to: " + pos2.toString());
    }

    public Location getBankPos1() {
        return bankPos1;
    }

    public Location getBankPos2() {
        return bankPos2;
    }

    public boolean isBankRegionDefined() {
        return bankPos1 != null && bankPos2 != null && bankPos1.getWorld().equals(bankPos2.getWorld());
    }

    public boolean isLocationInBank(Location loc) {
        if (!isBankRegionDefined() || !loc.getWorld().equals(bankPos1.getWorld())) {
            return false;
        }
        double x1 = Math.min(bankPos1.getX(), bankPos2.getX());
        double y1 = Math.min(bankPos1.getY(), bankPos2.getY());
        double z1 = Math.min(bankPos1.getZ(), bankPos2.getZ());
        double x2 = Math.max(bankPos1.getX(), bankPos2.getX());
        double y2 = Math.max(bankPos1.getY(), bankPos2.getY());
        double z2 = Math.max(bankPos1.getZ(), bankPos2.getZ());
        return loc.getX() >= x1 && loc.getX() <= x2 &&
               loc.getY() >= y1 && loc.getY() <= y2 &&
               loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    public void setBankerNPC(UUID npcUUID) {
        this.bankerNPC_UUID = npcUUID;
        saveBankData(); // Save immediately
        plugin.getLogger().info("Banker NPC UUID set to: " + npcUUID);
    }

    public UUID getBankerNPCUUID() {
        return bankerNPC_UUID;
    }

    public Entity getBankerNPC() {
        if (bankerNPC_UUID == null) return null;
        // Iterate through all worlds and entities to find the NPC by UUID
        // This is inefficient but necessary if the NPC could be in any loaded chunk of any world.
        // Citizens API would be much better here if available.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(bankerNPC_UUID)) {
                    return entity;
                }
            }
        }
        plugin.getLogger().warning("Banker NPC with UUID " + bankerNPC_UUID + " not found in any world.");
        return null;
    }
}
