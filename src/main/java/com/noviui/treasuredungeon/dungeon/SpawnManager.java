package com.noviui.treasuredungeon.dungeon;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Manages spawn locations for mobs and bosses within dungeons
 */
public class SpawnManager {
    
    private final TreasureDungeonPlugin plugin;
    
    // Spawn configuration
    private static final int SPAWN_RADIUS = 15; // Radius around dungeon center to find spawn points
    private static final int MIN_SPAWN_DISTANCE = 3; // Minimum distance between spawn points
    private static final int MAX_SPAWN_HEIGHT_DIFF = 5; // Maximum height difference for spawn points
    private static final int BOSS_SPAWN_RADIUS = 8; // Smaller radius for boss spawn
    
    public SpawnManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Finds suitable spawn locations for wave mobs around the dungeon center
     */
    public List<Location> findMobSpawnLocations(Location dungeonCenter, int mobCount) {
        if (dungeonCenter == null || mobCount <= 0) {
            plugin.getLogger().warning("Invalid parameters for mob spawn location finding");
            return new ArrayList<>();
        }
        
        List<Location> spawnLocations = new ArrayList<>();
        World world = dungeonCenter.getWorld();
        
        if (world == null) {
            plugin.getLogger().warning("World is null for dungeon center");
            return spawnLocations;
        }
        
        try {
            // Try to find spawn locations in a circular pattern
            for (int attempt = 0; attempt < mobCount * 3 && spawnLocations.size() < mobCount; attempt++) {
                Location candidate = generateRandomSpawnLocation(dungeonCenter, SPAWN_RADIUS);
                
                if (isValidSpawnLocation(candidate) && 
                    !isTooCloseToExistingSpawns(candidate, spawnLocations)) {
                    spawnLocations.add(candidate);
                }
            }
            
            // If we couldn't find enough locations, fill with safe defaults
            while (spawnLocations.size() < mobCount) {
                Location fallback = dungeonCenter.clone().add(
                    ThreadLocalRandom.current().nextInt(-5, 6),
                    1,
                    ThreadLocalRandom.current().nextInt(-5, 6)
                );
                
                if (isValidSpawnLocation(fallback)) {
                    spawnLocations.add(fallback);
                } else {
                    // Last resort: spawn at dungeon center + offset
                    spawnLocations.add(dungeonCenter.clone().add(0, 1, 0));
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error finding mob spawn locations", e);
            
            // Emergency fallback: spawn all at dungeon center
            for (int i = 0; i < mobCount; i++) {
                spawnLocations.add(dungeonCenter.clone().add(0, 1, 0));
            }
        }
        
        return spawnLocations;
    }
    
    /**
     * Finds the best spawn location for the boss
     */
    public Location findBossSpawnLocation(Location dungeonCenter) {
        if (dungeonCenter == null) {
            plugin.getLogger().warning("Dungeon center is null for boss spawn");
            return null;
        }
        
        World world = dungeonCenter.getWorld();
        if (world == null) {
            plugin.getLogger().warning("World is null for boss spawn");
            return dungeonCenter.clone().add(0, 1, 0);
        }
        
        try {
            // Try to find an elevated or central position for the boss
            List<Location> candidates = new ArrayList<>();
            
            // Check cardinal directions first
            int[] offsets = {0, 3, -3, 6, -6};
            for (int x : offsets) {
                for (int z : offsets) {
                    if (x == 0 && z == 0) continue; // Skip center for now
                    
                    Location candidate = dungeonCenter.clone().add(x, 0, z);
                    candidate = findSafeSpawnHeight(candidate);
                    
                    if (isValidBossSpawnLocation(candidate)) {
                        candidates.add(candidate);
                    }
                }
            }
            
            // If we found candidates, pick the best one (highest Y level)
            if (!candidates.isEmpty()) {
                return candidates.stream()
                    .max((a, b) -> Double.compare(a.getY(), b.getY()))
                    .orElse(dungeonCenter.clone().add(0, 1, 0));
            }
            
            // Fallback: try center with height adjustment
            Location centerSpawn = findSafeSpawnHeight(dungeonCenter.clone());
            if (isValidBossSpawnLocation(centerSpawn)) {
                return centerSpawn;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error finding boss spawn location", e);
        }
        
        // Last resort: spawn at dungeon center + 1 block up
        return dungeonCenter.clone().add(0, 1, 0);
    }
    
    /**
     * Generates a random spawn location within the specified radius
     */
    private Location generateRandomSpawnLocation(Location center, int radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble() * radius;
        
        int x = (int) (center.getX() + distance * Math.cos(angle));
        int z = (int) (center.getZ() + distance * Math.sin(angle));
        
        Location candidate = new Location(center.getWorld(), x, center.getY(), z);
        return findSafeSpawnHeight(candidate);
    }
    
    /**
     * Finds a safe height for spawning at the given X,Z coordinates
     */
    private Location findSafeSpawnHeight(Location location) {
        if (location == null || location.getWorld() == null) {
            return location;
        }
        
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.max(location.getBlockY() - 5, world.getMinHeight());
        int maxY = Math.min(location.getBlockY() + 10, world.getMaxHeight() - 2);
        
        // Look for a safe spawn location
        for (int y = startY; y <= maxY; y++) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            
            if (isSafeSpawnHeight(candidate)) {
                return candidate;
            }
        }
        
        // If no safe location found, return original with small adjustment
        return new Location(world, x + 0.5, location.getY() + 1, z + 0.5);
    }
    
    /**
     * Checks if a location is safe for spawning (solid ground, air above)
     */
    private boolean isSafeSpawnHeight(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        try {
            World world = location.getWorld();
            Block ground = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
            Block spawn = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Block above = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
            
            // Ground should be solid, spawn location and above should be air or passable
            return ground.getType().isSolid() && 
                   !ground.getType().equals(Material.LAVA) &&
                   !ground.getType().equals(Material.WATER) &&
                   (spawn.getType().isAir() || spawn.isPassable()) &&
                   (above.getType().isAir() || above.isPassable());
                   
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking spawn height safety", e);
            return false;
        }
    }
    
    /**
     * Validates if a location is suitable for mob spawning
     */
    private boolean isValidSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        try {
            // Basic safety checks
            if (!isSafeSpawnHeight(location)) {
                return false;
            }
            
            // Check for dangerous blocks nearby
            World world = location.getWorld();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = world.getBlockAt(
                        location.getBlockX() + dx, 
                        location.getBlockY() - 1, 
                        location.getBlockZ() + dz
                    );
                    
                    if (block.getType().equals(Material.LAVA) || 
                        block.getType().equals(Material.MAGMA_BLOCK)) {
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error validating spawn location", e);
            return false;
        }
    }
    
    /**
     * Validates if a location is suitable for boss spawning (more strict requirements)
     */
    private boolean isValidBossSpawnLocation(Location location) {
        if (!isValidSpawnLocation(location)) {
            return false;
        }
        
        try {
            // Boss needs more space - check 3x3 area
            World world = location.getWorld();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        Block block = world.getBlockAt(
                            location.getBlockX() + dx,
                            location.getBlockY() + dy,
                            location.getBlockZ() + dz
                        );
                        
                        if (!block.getType().isAir() && !block.isPassable()) {
                            return false;
                        }
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error validating boss spawn location", e);
            return false;
        }
    }
    
    /**
     * Checks if a location is too close to existing spawn points
     */
    private boolean isTooCloseToExistingSpawns(Location candidate, List<Location> existingSpawns) {
        if (candidate == null || existingSpawns == null) {
            return false;
        }
        
        for (Location existing : existingSpawns) {
            if (existing != null && candidate.distance(existing) < MIN_SPAWN_DISTANCE) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Creates spawn points in a circular pattern around the center
     */
    public List<Location> createCircularSpawnPattern(Location center, int mobCount, double radius) {
        List<Location> spawns = new ArrayList<>();
        
        if (center == null || mobCount <= 0) {
            return spawns;
        }
        
        try {
            double angleStep = (2 * Math.PI) / mobCount;
            
            for (int i = 0; i < mobCount; i++) {
                double angle = i * angleStep;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                
                Location spawnPoint = new Location(center.getWorld(), x, center.getY(), z);
                spawnPoint = findSafeSpawnHeight(spawnPoint);
                
                if (isValidSpawnLocation(spawnPoint)) {
                    spawns.add(spawnPoint);
                } else {
                    // Fallback to a safe location near center
                    spawns.add(center.clone().add(0, 1, 0));
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating circular spawn pattern", e);
            
            // Emergency fallback
            for (int i = 0; i < mobCount; i++) {
                spawns.add(center.clone().add(0, 1, 0));
            }
        }
        
        return spawns;
    }
    
    /**
     * Gets spawn configuration for debugging
     */
    public String getSpawnConfig() {
        return String.format(
            "SpawnManager Config: radius=%d, minDistance=%d, maxHeightDiff=%d, bossRadius=%d",
            SPAWN_RADIUS, MIN_SPAWN_DISTANCE, MAX_SPAWN_HEIGHT_DIFF, BOSS_SPAWN_RADIUS
        );
    }
}