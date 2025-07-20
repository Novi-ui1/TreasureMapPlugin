package com.noviui.treasuredungeon.dungeon;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles dynamic dungeon construction with animations
 */
public class DungeonBuilder {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    
    // Active construction tracking
    private final ConcurrentHashMap<UUID, BukkitTask> activeConstructions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Location, UUID> dungeonLocations = new ConcurrentHashMap<>();
    
    // Construction settings
    private static final int BUILD_HEIGHT_START = -60;
    private static final int BLOCKS_PER_TICK = 5; // Blocks to place per tick for animation
    private static final int ANIMATION_DELAY = 2; // Ticks between animation steps
    
    public DungeonBuilder(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
    }
    
    /**
     * Builds a dungeon at the specified location with animation
     */
    public void buildDungeon(Location bellLocation, String dungeonType, UUID initiatorId, List<Player> participants) {
        if (bellLocation == null || dungeonType == null || initiatorId == null) {
            plugin.getLogger().warning("Invalid parameters for dungeon building");
            return;
        }
        
        try {
            // Remove the bell
            bellLocation.getBlock().setType(Material.AIR);
            
            // Calculate build location (Y = -60)
            Location buildLocation = new Location(
                bellLocation.getWorld(),
                bellLocation.getBlockX(),
                BUILD_HEIGHT_START,
                bellLocation.getBlockZ()
            );
            
            // Mark location as occupied
            dungeonLocations.put(buildLocation, initiatorId);
            
            // Get schematic file
            String schematicName = configManager.getDungeonTypeDungeonSchematic(dungeonType);
            if (schematicName == null || schematicName.trim().isEmpty()) {
                plugin.getLogger().warning("No schematic configured for dungeon type: " + dungeonType);
                buildSimpleDungeon(buildLocation, dungeonType, initiatorId, participants);
                return;
            }
            
            // Check if schematic exists
            File schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName);
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found: " + schematicName);
                buildSimpleDungeon(buildLocation, dungeonType, initiatorId, participants);
                return;
            }
            
            // Start animated construction
            startAnimatedConstruction(buildLocation, schematicName, dungeonType, initiatorId, participants);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error building dungeon", e);
            
            // Cleanup on error
            dungeonLocations.remove(bellLocation);
            
            Player initiator = Bukkit.getPlayer(initiatorId);
            if (initiator != null) {
                String errorMessage = languageManager.getMessage("dungeon-build-failed");
                initiator.sendMessage(languageManager.getPrefix() + errorMessage);
            }
        }
    }
    
    /**
     * Starts animated construction of the dungeon
     */
    private void startAnimatedConstruction(Location buildLocation, String schematicName, 
                                         String dungeonType, UUID initiatorId, List<Player> participants) {
        
        // Notify participants
        String buildingMessage = languageManager.getMessage("dungeon-building");
        for (Player participant : participants) {
            if (participant != null && participant.isOnline()) {
                participant.sendMessage(languageManager.getPrefix() + buildingMessage);
                participant.playSound(participant.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            }
        }
        
        // Create construction task
        BukkitTask constructionTask = new BukkitRunnable() {
            private int step = 0;
            private final int maxSteps = 20; // Total animation steps
            
            @Override
            public void run() {
                try {
                    if (step >= maxSteps) {
                        // Construction complete
                        this.cancel();
                        activeConstructions.remove(initiatorId);
                        onConstructionComplete(buildLocation, dungeonType, initiatorId, participants);
                        return;
                    }
                    
                    // Animate construction step
                    animateConstructionStep(buildLocation, step, maxSteps, participants);
                    step++;
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in construction animation", e);
                    this.cancel();
                    activeConstructions.remove(initiatorId);
                }
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_DELAY);
        
        activeConstructions.put(initiatorId, constructionTask);
    }
    
    /**
     * Animates a single construction step
     */
    private void animateConstructionStep(Location buildLocation, int step, int maxSteps, List<Player> participants) {
        try {
            World world = buildLocation.getWorld();
            if (world == null) return;
            
            // Calculate construction area for this step
            int radius = (step * 10) / maxSteps; // Gradually expand radius
            int height = (step * 20) / maxSteps; // Gradually build upward
            
            // Place blocks in a pattern
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = 0; y <= height; y++) {
                        Location blockLoc = buildLocation.clone().add(x, y, z);
                        
                        // Simple pattern - can be replaced with actual schematic loading
                        if (shouldPlaceBlock(x, y, z, radius, height)) {
                            Material material = getBlockMaterial(x, y, z, step);
                            blockLoc.getBlock().setType(material);
                            
                            // Particle effects
                            world.spawnParticle(Particle.BLOCK_CRACK, 
                                blockLoc.clone().add(0.5, 0.5, 0.5), 
                                5, 0.2, 0.2, 0.2, 0.1, material.createBlockData());
                        }
                    }
                }
            }
            
            // Sound effects
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.playSound(buildLocation, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f + (step * 0.1f));
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in construction step animation", e);
        }
    }
    
    /**
     * Determines if a block should be placed at the given coordinates
     */
    private boolean shouldPlaceBlock(int x, int y, int z, int radius, int height) {
        // Simple dungeon structure logic
        double distance = Math.sqrt(x * x + z * z);
        
        // Outer walls
        if (distance >= radius - 1 && distance <= radius) {
            return y <= height;
        }
        
        // Floor
        if (y == 0 && distance <= radius) {
            return true;
        }
        
        // Inner structures
        if (y <= height / 2 && distance <= radius / 2) {
            return Math.random() < 0.3; // Random inner structures
        }
        
        return false;
    }
    
    /**
     * Gets the appropriate material for a block at given coordinates
     */
    private Material getBlockMaterial(int x, int y, int z, int step) {
        // Floor
        if (y == 0) {
            return Material.STONE_BRICKS;
        }
        
        // Walls
        double distance = Math.sqrt(x * x + z * z);
        if (distance >= 8) {
            return Material.COBBLESTONE;
        }
        
        // Inner structures
        if (step > 10) {
            return Material.MOSSY_COBBLESTONE;
        }
        
        return Material.STONE;
    }
    
    /**
     * Called when construction is complete
     */
    private void onConstructionComplete(Location buildLocation, String dungeonType, 
                                     UUID initiatorId, List<Player> participants) {
        try {
            // Notify participants
            String completeMessage = languageManager.getMessage("dungeon-build-complete");
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + completeMessage);
                    participant.playSound(buildLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                }
            }
            
            // Spectacular completion effects
            World world = buildLocation.getWorld();
            if (world != null) {
                // Firework-like particle explosion
                for (int i = 0; i < 50; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 10;
                    double height = Math.random() * 15;
                    
                    Location particleLoc = buildLocation.clone().add(
                        Math.cos(angle) * radius,
                        height,
                        Math.sin(angle) * radius
                    );
                    
                    world.spawnParticle(Particle.FIREWORKS_SPARK, particleLoc, 1, 0, 0, 0, 0.1);
                }
            }
            
            // Start the dungeon
            plugin.getDungeonManager().startDungeonAtLocation(buildLocation, dungeonType, initiatorId, participants);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error completing dungeon construction", e);
        }
    }
    
    /**
     * Builds a simple dungeon when schematic is not available
     */
    private void buildSimpleDungeon(Location buildLocation, String dungeonType, 
                                  UUID initiatorId, List<Player> participants) {
        try {
            World world = buildLocation.getWorld();
            if (world == null) return;
            
            // Create a simple 15x15 arena
            int size = 15;
            int height = 10;
            
            // Floor
            for (int x = -size/2; x <= size/2; x++) {
                for (int z = -size/2; z <= size/2; z++) {
                    Location floorLoc = buildLocation.clone().add(x, 0, z);
                    floorLoc.getBlock().setType(Material.STONE_BRICKS);
                }
            }
            
            // Walls
            for (int x = -size/2; x <= size/2; x++) {
                for (int y = 1; y <= height; y++) {
                    // Front and back walls
                    buildLocation.clone().add(x, y, -size/2).getBlock().setType(Material.COBBLESTONE);
                    buildLocation.clone().add(x, y, size/2).getBlock().setType(Material.COBBLESTONE);
                }
            }
            
            for (int z = -size/2; z <= size/2; z++) {
                for (int y = 1; y <= height; y++) {
                    // Left and right walls
                    buildLocation.clone().add(-size/2, y, z).getBlock().setType(Material.COBBLESTONE);
                    buildLocation.clone().add(size/2, y, z).getBlock().setType(Material.COBBLESTONE);
                }
            }
            
            // Add some decorative elements
            buildLocation.clone().add(0, 1, 0).getBlock().setType(Material.TORCH);
            buildLocation.clone().add(5, 1, 5).getBlock().setType(Material.TORCH);
            buildLocation.clone().add(-5, 1, 5).getBlock().setType(Material.TORCH);
            buildLocation.clone().add(5, 1, -5).getBlock().setType(Material.TORCH);
            buildLocation.clone().add(-5, 1, -5).getBlock().setType(Material.TORCH);
            
            // Start the dungeon
            plugin.getDungeonManager().startDungeonAtLocation(buildLocation, dungeonType, initiatorId, participants);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error building simple dungeon", e);
        }
    }
    
    /**
     * Destroys a dungeon with animation
     */
    public void destroyDungeon(Location dungeonLocation, List<Player> participants) {
        if (dungeonLocation == null) {
            plugin.getLogger().warning("Cannot destroy dungeon: location is null");
            return;
        }
        
        try {
            // Remove from tracking
            dungeonLocations.remove(dungeonLocation);
            
            // Notify participants
            String destroyMessage = languageManager.getMessage("dungeon-destroying");
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + destroyMessage);
                }
            }
            
            // Start destruction animation
            new BukkitRunnable() {
                private int step = 0;
                private final int maxSteps = 15;
                
                @Override
                public void run() {
                    if (step >= maxSteps) {
                        this.cancel();
                        onDestructionComplete(dungeonLocation, participants);
                        return;
                    }
                    
                    animateDestructionStep(dungeonLocation, step, maxSteps, participants);
                    step++;
                }
            }.runTaskTimer(plugin, 0L, 3L);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error destroying dungeon", e);
        }
    }
    
    /**
     * Animates a single destruction step
     */
    private void animateDestructionStep(Location dungeonLocation, int step, int maxSteps, List<Player> participants) {
        try {
            World world = dungeonLocation.getWorld();
            if (world == null) return;
            
            // Calculate destruction area for this step (from outside in)
            int maxRadius = 15;
            int currentRadius = maxRadius - (step * maxRadius / maxSteps);
            
            // Remove blocks in current radius
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    for (int y = 0; y <= 20; y++) {
                        double distance = Math.sqrt(x * x + z * z);
                        
                        if (distance >= currentRadius && distance < currentRadius + 2) {
                            Location blockLoc = dungeonLocation.clone().add(x, y, z);
                            Block block = blockLoc.getBlock();
                            
                            if (!block.getType().isAir()) {
                                // Particle effects before destruction
                                world.spawnParticle(Particle.BLOCK_CRACK, 
                                    blockLoc.clone().add(0.5, 0.5, 0.5), 
                                    10, 0.3, 0.3, 0.3, 0.1, block.getBlockData());
                                
                                // Remove block
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
            
            // Sound effects
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.playSound(dungeonLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.8f + (step * 0.1f));
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in destruction step animation", e);
        }
    }
    
    /**
     * Called when destruction is complete
     */
    private void onDestructionComplete(Location dungeonLocation, List<Player> participants) {
        try {
            // Final cleanup - ensure all blocks are removed
            World world = dungeonLocation.getWorld();
            if (world != null) {
                for (int x = -20; x <= 20; x++) {
                    for (int z = -20; z <= 20; z++) {
                        for (int y = -5; y <= 25; y++) {
                            Location cleanupLoc = dungeonLocation.clone().add(x, y, z);
                            Block block = cleanupLoc.getBlock();
                            
                            // Only remove non-natural blocks
                            if (isArtificialBlock(block.getType())) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
            
            // Notify participants
            String completeMessage = languageManager.getMessage("dungeon-destroyed");
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + completeMessage);
                    participant.playSound(dungeonLocation, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error completing dungeon destruction", e);
        }
    }
    
    /**
     * Checks if a block type is artificial (placed by dungeon)
     */
    private boolean isArtificialBlock(Material material) {
        return material == Material.STONE_BRICKS ||
               material == Material.COBBLESTONE ||
               material == Material.MOSSY_COBBLESTONE ||
               material == Material.STONE ||
               material == Material.TORCH ||
               material == Material.CHEST ||
               material.name().contains("STAIRS") ||
               material.name().contains("SLAB");
    }
    
    /**
     * Checks if a location has an active dungeon
     */
    public boolean hasDungeonAt(Location location) {
        return dungeonLocations.containsKey(location);
    }
    
    /**
     * Gets the owner of a dungeon at the specified location
     */
    public UUID getDungeonOwner(Location location) {
        return dungeonLocations.get(location);
    }
    
    /**
     * Cleanup method for plugin shutdown
     */
    public void cleanup() {
        try {
            // Cancel all active constructions
            for (BukkitTask task : activeConstructions.values()) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
            activeConstructions.clear();
            dungeonLocations.clear();
            
            plugin.getLogger().info("Dungeon builder cleanup completed");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during dungeon builder cleanup", e);
        }
    }
}