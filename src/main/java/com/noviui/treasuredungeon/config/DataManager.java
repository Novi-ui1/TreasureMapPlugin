package com.noviui.treasuredungeon.config;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {
    
    private final TreasureDungeonPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public DataManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }
    
    // Player cooldown methods
    public void setCooldown(UUID playerId, String skill, long expireTime) {
        String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
        dataConfig.set(path, Instant.ofEpochMilli(expireTime).toString());
        saveData();
    }
    
    public boolean isOnCooldown(UUID playerId, String skill) {
        String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
        String expireTimeStr = dataConfig.getString(path);
        
        if (expireTimeStr == null) {
            return false;
        }
        
        try {
            Instant expireTime = Instant.parse(expireTimeStr);
            return Instant.now().isBefore(expireTime);
        } catch (Exception e) {
            return false;
        }
    }
    
    public long getCooldownRemaining(UUID playerId, String skill) {
        String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
        String expireTimeStr = dataConfig.getString(path);
        
        if (expireTimeStr == null) {
            return 0;
        }
        
        try {
            Instant expireTime = Instant.parse(expireTimeStr);
            long remaining = expireTime.toEpochMilli() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Map tracking methods
    public void setMapReceived(UUID playerId, String skill, boolean received) {
        String path = "players." + playerId.toString() + "." + skill + ".received";
        dataConfig.set(path, received);
        saveData();
    }
    
    public boolean hasReceivedMap(UUID playerId, String skill) {
        String path = "players." + playerId.toString() + "." + skill + ".received";
        return dataConfig.getBoolean(path, false);
    }
    
    // Active dungeon methods
    public void setActiveDungeon(UUID playerId, String skill, String world, int x, int y, int z) {
        String basePath = "players." + playerId.toString() + "." + skill + ".active-dungeon";
        dataConfig.set(basePath + ".world", world);
        dataConfig.set(basePath + ".coords.x", x);
        dataConfig.set(basePath + ".coords.y", y);
        dataConfig.set(basePath + ".coords.z", z);
        dataConfig.set(basePath + ".status", "in-progress");
        saveData();
    }
    
    public boolean hasActiveDungeon(UUID playerId, String skill) {
        String path = "players." + playerId.toString() + "." + skill + ".active-dungeon.status";
        return "in-progress".equals(dataConfig.getString(path));
    }
    
    public void clearActiveDungeon(UUID playerId, String skill) {
        String path = "players." + playerId.toString() + "." + skill + ".active-dungeon";
        dataConfig.set(path, null);
        saveData();
    }
    
    public int[] getDungeonCoords(UUID playerId, String skill) {
        String basePath = "players." + playerId.toString() + "." + skill + ".active-dungeon.coords";
        int x = dataConfig.getInt(basePath + ".x", 0);
        int y = dataConfig.getInt(basePath + ".y", 64);
        int z = dataConfig.getInt(basePath + ".z", 0);
        return new int[]{x, y, z};
    }
    
    public FileConfiguration getDataConfig() {
        return dataConfig;
    }
}