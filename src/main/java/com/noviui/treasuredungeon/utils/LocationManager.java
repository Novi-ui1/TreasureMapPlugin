package com.noviui.treasuredungeon.utils;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LocationManager {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    
    // Cache for active dungeon locations to prevent conflicts
    private final Set<String> activeDungeonLocations = ConcurrentHashMap.newKeySet();
    
    // Cache for recently used locations (prevents immediate reuse)
    private final Map<String, Long> recentLocationCache = new ConcurrentHashMap<>();
    
    // Maximum attempts to find a valid location
    private static final int MAX_LOCATION_ATTEMPTS = 50;
    
    // Cache cleanup interval (30 minutes)
    private static final long CACHE_CLEANUP_INTERVAL = 30 * 60 * 1000L;
    
    public LocationManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        
        // Start cache cleanup task
        startCacheCleanupTask();
    }
    
    /**
     * Generates a safe location for a new dungeon, ensuring minimum distance from other dungeons
     */
    public CompletableFuture<Location> generateSafeDungeonLocation(World world, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (world == null) {
                throw new IllegalArgumentException("World cannot be null");
            }
            
            try {
                return findSafeLocationSync(world, playerId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to generate safe dungeon location", e);
                throw new RuntimeException("Could not generate safe location", e);
            }
        });
    }
    
    private Location findSafeLocationSync(World world, UUID playerId) {
        int minX = configManager.getMinX();
        int maxX = configManager.getMaxX();
        int minZ = configManager.getMinZ();
        int maxZ = configManager.getMaxZ();
        int minDistance = configManager.getMinDistance();
        
        // Validate configuration
        if (minX >= maxX || minZ >= maxZ) {
            throw new IllegalStateException("Invalid coordinate configuration: min values must be less than max values");
        }
        
        if (minDistance <= 0) {
            throw new IllegalStateException("Minimum distance must be greater than 0");
        }
        
        for (int attempt = 0; attempt < MAX_LOCATION_ATTEMPTS; attempt++) {
            try {
                // Generate random coordinates
                int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
                int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
                
                Location candidate = new Location(world, x, 0, z);
                
                // Check if location is safe
                if (isLocationSafe(candidate, minDistance, playerId)) {
                    // Get proper Y coordinate
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    candidate.setY(y);
                    
                    // Reserve this location
                    reserveLocation(candidate, playerId);
                    
                    return candidate;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during location generation attempt " + (attempt + 1), e);
            }
        }
        
        throw new RuntimeException("Could not find a safe location after " + MAX_LOCATION_ATTEMPTS + " attempts");
    }
    
    /**
     * Checks if a location is safe (far enough from other dungeons)
     */
    private boolean isLocationSafe(Location location, int minDistance, UUID playerId) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        String locationKey = getLocationKey(location);
        
        // Check if this exact location is already in use
        if (activeDungeonLocations.contains(locationKey)) {
            return false;
        }
        
        // Check distance from all active dungeons
        for (String activeLocation : activeDungeonLocations) {
            try {
                Location activeLoc = parseLocationKey(activeLocation, location.getWorld());
                if (activeLoc != null && location.distance(activeLoc) < minDistance) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing active location: " + activeLocation, e);
            }
        }
        
        // Check distance from recent locations (prevents immediate reuse)
        for (Map.Entry<String, Long> entry : recentLocationCache.entrySet()) {
            try {
                Location recentLoc = parseLocationKey(entry.getKey(), location.getWorld());
                if (recentLoc != null && location.distance(recentLoc) < minDistance) {
                    // Check if the recent location is still within cooldown
                    long timeSinceUse = System.currentTimeMillis() - entry.getValue();
                    if (timeSinceUse < CACHE_CLEANUP_INTERVAL) {
                        return false;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing recent location: " + entry.getKey(), e);
            }
        }
        
        // Check against stored dungeon locations in data.yml
        return !isNearStoredDungeons(location, minDistance, playerId);
    }
    
    /**
     * Checks if location is too close to dungeons stored in data.yml
     */
    private boolean isNearStoredDungeons(Location location, int minDistance, UUID excludePlayerId) {
        try {
            var dataConfig = dataManager.getDataConfig();
            if (dataConfig == null || !dataConfig.contains("players")) {
                return false;
            }
            
            var playersSection = dataConfig.getConfigurationSection("players");
            if (playersSection == null) {
                return false;
            }
            
            for (String playerIdStr : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    
                    // Skip the current player's dungeons
                    if (playerId.equals(excludePlayerId)) {
                        continue;
                    }
                    
                    var playerSection = playersSection.getConfigurationSection(playerIdStr);
                    if (playerSection == null) {
                        continue;
                    }
                    
                    // Check all skills for this player
                    for (String skill : playerSection.getKeys(false)) {
                        var skillSection = playerSection.getConfigurationSection(skill);
                        if (skillSection == null || !skillSection.contains("active-dungeon")) {
                            continue;
                        }
                        
                        var dungeonSection = skillSection.getConfigurationSection("active-dungeon");
                        if (dungeonSection == null || !"in-progress".equals(dungeonSection.getString("status"))) {
                            continue;
                        }
                        
                        // Get coordinates
                        var coordsSection = dungeonSection.getConfigurationSection("coords");
                        if (coordsSection == null) {
                            continue;
                        }
                        
                        int x = coordsSection.getInt("x", 0);
                        int z = coordsSection.getInt("z", 0);
                        
                        Location storedLocation = new Location(location.getWorld(), x, 0, z);
                        if (location.distance(storedLocation) < minDistance) {
                            return true;
                        }
                    }
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid player UUID in data.yml: " + playerIdStr, e);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error checking stored dungeons for player: " + playerIdStr, e);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking stored dungeon locations", e);
        }
        
        return false;
    }
    
    /**
     * Reserves a location to prevent conflicts
     */
    public void reserveLocation(Location location, UUID playerId) {
        if (location == null) {
            return;
        }
        
        String locationKey = getLocationKey(location);
        activeDungeonLocations.add(locationKey);
        
        plugin.getLogger().info("Reserved dungeon location " + locationKey + " for player " + playerId);
    }
    
    /**
     * Releases a location when dungeon is completed
     */
    public void releaseLocation(Location location, UUID playerId) {
        if (location == null) {
            return;
        }
        
        String locationKey = getLocationKey(location);
        activeDungeonLocations.remove(locationKey);
        
        // Add to recent cache to prevent immediate reuse
        recentLocationCache.put(locationKey, System.currentTimeMillis());
        
        plugin.getLogger().info("Released dungeon location " + locationKey + " for player " + playerId);
    }
    
    /**
     * Gets a string key for a location
     */
    private String getLocationKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "invalid";
        }
        
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockZ();
    }
    
    /**
     * Parses a location key back to a Location
     */
    private Location parseLocationKey(String locationKey, World world) {
        if (locationKey == null || world == null) {
            return null;
        }
        
        try {
            String[] parts = locationKey.split(":");
            if (parts.length != 3) {
                return null;
            }
            
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            
            return new Location(world, x, 0, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Starts the cache cleanup task
     */
    private void startCacheCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                cleanupCache();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during cache cleanup", e);
            }
        }, 36000L, 36000L); // Every 30 minutes
    }
    
    /**
     * Cleans up old entries from the recent location cache
     */
    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, Long>> iterator = recentLocationCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > CACHE_CLEANUP_INTERVAL) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " old location cache entries");
        }
    }
    
    /**
     * Clears all caches (used during plugin reload/shutdown)
     */
    public void clearCache() {
        activeDungeonLocations.clear();
        recentLocationCache.clear();
        plugin.getLogger().info("Cleared all location caches");
    }
    
    /**
     * Gets the number of active dungeon locations
     */
    public int getActiveDungeonCount() {
        return activeDungeonLocations.size();
    }
    
    /**
     * Gets the number of recent locations in cache
     */
    public int getRecentLocationCount() {
        return recentLocationCache.size();
    }
    
    /**
     * Validates if the current configuration allows for dungeon generation
     */
    public boolean validateConfiguration() {
        try {
            int minX = configManager.getMinX();
            int maxX = configManager.getMaxX();
            int minZ = configManager.getMinZ();
            int maxZ = configManager.getMaxZ();
            int minDistance = configManager.getMinDistance();
            
            // Check coordinate ranges
            if (minX >= maxX || minZ >= maxZ) {
                plugin.getLogger().severe("Invalid coordinate configuration: min values must be less than max values");
                return false;
            }
            
            // Check if area is large enough for minimum distance
            int areaWidth = maxX - minX;
            int areaHeight = maxZ - minZ;
            
            if (areaWidth < minDistance * 2 || areaHeight < minDistance * 2) {
                plugin.getLogger().warning("Configured area might be too small for minimum distance requirements");
                plugin.getLogger().warning("Area: " + areaWidth + "x" + areaHeight + ", Min distance: " + minDistance);
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validating location configuration", e);
            return false;
        }
    }
}