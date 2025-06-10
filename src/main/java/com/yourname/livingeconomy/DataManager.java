package com.yourname.livingeconomy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager implements Listener {

    private final LivingEconomy plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final long autoSaveIntervalTicks; // In ticks

    public DataManager(LivingEconomy plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        // TODO: Make autoSaveInterval configurable from config.yml
        // For now, default to 10 minutes (10 * 60 * 20 ticks)
        this.autoSaveIntervalTicks = plugin.getConfig().getLong("data.autosave_interval_minutes", 10) * 60 * 20;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start autosave task
        startAutoSaveTask();

        // Ensure playerdata directory exists
        File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        PlayerData pData = new PlayerData(plugin, playerUUID);
        // pData.load(); // PlayerData constructor calls load()
        playerDataMap.put(playerUUID, pData);
        plugin.getLogger().info("Loaded data for player " + player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        PlayerData pData = playerDataMap.get(playerUUID);
        if (pData != null) {
            pData.save();
            playerDataMap.remove(playerUUID);
            plugin.getLogger().info("Saved data for player " + player.getName());
        }
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void saveAllOnlinePlayerData() {
        plugin.getLogger().info("Starting global player data save...");
        int count = 0;
        for (PlayerData pData : playerDataMap.values()) {
            pData.save();
            count++;
        }
        plugin.getLogger().info("Successfully saved data for " + count + " online players.");
    }

    private void startAutoSaveTask() {
        if (autoSaveIntervalTicks <= 0) {
            plugin.getLogger().info("Autosave task is disabled (interval <= 0).");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllOnlinePlayerData();
            }
        }.runTaskTimerAsynchronously(plugin, autoSaveIntervalTicks, autoSaveIntervalTicks);
        plugin.getLogger().info("Player data autosave task started. Interval: " + (autoSaveIntervalTicks / 20 / 60) + " minutes.");
    }

    // Call this method in onDisable of the main plugin class
    public void shutdown() {
        plugin.getLogger().info("Saving all player data before shutdown...");
        saveAllOnlinePlayerData();
        playerDataMap.clear();
    }
}
