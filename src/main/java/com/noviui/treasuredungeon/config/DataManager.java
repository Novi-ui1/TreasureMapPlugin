package com.noviui.treasuredungeon.config;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataManager {
    
    private final TreasureDungeonPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean saveInProgress = false;
    
    public DataManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadData() {
        lock.writeLock().lock();
        try {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
            
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            if (!dataFile.exists()) {
                try {
                    plugin.saveResource("data.yml", false);
                } catch (Exception e) {
                    // Create empty file if resource doesn't exist
                    try {
                        dataFile.createNewFile();
                    } catch (IOException ioException) {
                        throw new RuntimeException("Could not create data.yml file", ioException);
                    }
                }
            }
            
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            
            // Validate data structure
            if (!dataConfig.contains("players")) {
                dataConfig.createSection("players");
                saveData();
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load data.yml", e);
            throw new RuntimeException("Critical error loading data", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void saveData() {
        lock.readLock().lock();
        try {
            if (saveInProgress) {
                plugin.getLogger().warning("Save already in progress, skipping duplicate save request");
                return;
            }
            
            saveInProgress = true;
            
            if (dataConfig == null || dataFile == null) {
                plugin.getLogger().warning("Cannot save data: dataConfig or dataFile is null");
                return;
            }
            
            // Create backup before saving
            createBackup();
            
            dataConfig.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
            restoreBackup();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during data save", e);
        } finally {
            saveInProgress = false;
            lock.readLock().unlock();
        }
    }
    
    public CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                saveData();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in async data save", e);
                throw new RuntimeException("Async save failed", e);
            }
        });
    }
    
    private void createBackup() {
        try {
            if (dataFile.exists()) {
                File backupFile = new File(plugin.getDataFolder(), "data.yml.backup");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                
                java.nio.file.Files.copy(dataFile.toPath(), backupFile.toPath());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not create data backup", e);
        }
    }
    
    private void restoreBackup() {
        try {
            File backupFile = new File(plugin.getDataFolder(), "data.yml.backup");
            if (backupFile.exists()) {
                java.nio.file.Files.copy(backupFile.toPath(), dataFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Restored data from backup due to save error");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not restore data backup", e);
        }
    }
    
    // Player cooldown methods
    public void setCooldown(UUID playerId, String skill, long expireTime) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters for setCooldown: playerId=" + playerId + ", skill=" + skill);
            return;
        }
        
        lock.writeLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
            dataConfig.set(path, Instant.ofEpochMilli(expireTime).toString());
            
            // Async save to prevent blocking
            saveDataAsync();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting cooldown for player " + playerId + " skill " + skill, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean isOnCooldown(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
            String expireTimeStr = dataConfig.getString(path);
            
            if (expireTimeStr == null || expireTimeStr.trim().isEmpty()) {
                return false;
            }
            
            Instant expireTime = Instant.parse(expireTimeStr);
            return Instant.now().isBefore(expireTime);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking cooldown for player " + playerId + " skill " + skill, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public long getCooldownRemaining(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return 0;
        }
        
        lock.readLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".cooldown-expires";
            String expireTimeStr = dataConfig.getString(path);
            
            if (expireTimeStr == null || expireTimeStr.trim().isEmpty()) {
                return 0;
            }
            
            Instant expireTime = Instant.parse(expireTimeStr);
            long remaining = expireTime.toEpochMilli() - System.currentTimeMillis();
            return Math.max(0, remaining);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting cooldown remaining for player " + playerId + " skill " + skill, e);
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Map tracking methods
    public void setMapReceived(UUID playerId, String skill, boolean received) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters for setMapReceived: playerId=" + playerId + ", skill=" + skill);
            return;
        }
        
        lock.writeLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".received";
            dataConfig.set(path, received);
            
            // Async save to prevent blocking
            saveDataAsync();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting map received for player " + playerId + " skill " + skill, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean hasReceivedMap(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".received";
            return dataConfig.getBoolean(path, false);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking map received for player " + playerId + " skill " + skill, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Active dungeon methods
    public void setActiveDungeon(UUID playerId, String skill, String world, int x, int y, int z, String dungeonType) {
        if (playerId == null || skill == null || skill.trim().isEmpty() || 
            world == null || world.trim().isEmpty() || dungeonType == null || dungeonType.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters for setActiveDungeon");
            return;
        }
        
        lock.writeLock().lock();
        try {
            String basePath = "players." + playerId.toString() + "." + skill + ".active-dungeon";
            dataConfig.set(basePath + ".world", world);
            dataConfig.set(basePath + ".coords.x", x);
            dataConfig.set(basePath + ".coords.y", y);
            dataConfig.set(basePath + ".coords.z", z);
            dataConfig.set(basePath + ".status", "in-progress");
            dataConfig.set(basePath + ".dungeon-type", dungeonType);
            dataConfig.set(basePath + ".created-at", System.currentTimeMillis());
            
            // Async save to prevent blocking
            saveDataAsync();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting active dungeon for player " + playerId + " skill " + skill, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean hasActiveDungeon(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".active-dungeon.status";
            return "in-progress".equals(dataConfig.getString(path));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking active dungeon for player " + playerId + " skill " + skill, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void clearActiveDungeon(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters for clearActiveDungeon: playerId=" + playerId + ", skill=" + skill);
            return;
        }
        
        lock.writeLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".active-dungeon";
            dataConfig.set(path, null);
            
            // Async save to prevent blocking
            saveDataAsync();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error clearing active dungeon for player " + playerId + " skill " + skill, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int[] getDungeonCoords(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return new int[]{0, 64, 0};
        }
        
        lock.readLock().lock();
        try {
            String basePath = "players." + playerId.toString() + "." + skill + ".active-dungeon.coords";
            int x = dataConfig.getInt(basePath + ".x", 0);
            int y = dataConfig.getInt(basePath + ".y", 64);
            int z = dataConfig.getInt(basePath + ".z", 0);
            return new int[]{x, y, z};
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting dungeon coords for player " + playerId + " skill " + skill, e);
            return new int[]{0, 64, 0};
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public String getDungeonType(UUID playerId, String skill) {
        if (playerId == null || skill == null || skill.trim().isEmpty()) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            String path = "players." + playerId.toString() + "." + skill + ".active-dungeon.dungeon-type";
            return dataConfig.getString(path);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting dungeon type for player " + playerId + " skill " + skill, e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public FileConfiguration getDataConfig() {
        lock.readLock().lock();
        try {
            return dataConfig;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Cleanup old dungeon data (dungeons older than 24 hours)
     */
    public void cleanupOldDungeons() {
        lock.writeLock().lock();
        try {
            if (!dataConfig.contains("players")) {
                return;
            }
            
            var playersSection = dataConfig.getConfigurationSection("players");
            if (playersSection == null) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            long maxAge = 24 * 60 * 60 * 1000L; // 24 hours
            int cleanedCount = 0;
            
            for (String playerIdStr : playersSection.getKeys(false)) {
                var playerSection = playersSection.getConfigurationSection(playerIdStr);
                if (playerSection == null) continue;
                
                for (String skill : playerSection.getKeys(false)) {
                    var skillSection = playerSection.getConfigurationSection(skill);
                    if (skillSection == null || !skillSection.contains("active-dungeon")) continue;
                    
                    var dungeonSection = skillSection.getConfigurationSection("active-dungeon");
                    if (dungeonSection == null) continue;
                    
                    long createdAt = dungeonSection.getLong("created-at", currentTime);
                    if (currentTime - createdAt > maxAge) {
                        skillSection.set("active-dungeon", null);
                        cleanedCount++;
                    }
                }
            }
            
            if (cleanedCount > 0) {
                saveDataAsync();
                plugin.getLogger().info("Cleaned up " + cleanedCount + " old dungeon entries");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during dungeon cleanup", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}