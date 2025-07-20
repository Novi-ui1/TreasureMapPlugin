package com.noviui.treasuredungeon.listeners;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.dungeon.DungeonManager;
import com.noviui.treasuredungeon.map.MapManager;
import com.noviui.treasuredungeon.utils.LocationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerInteractListener implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final MapManager mapManager;
    private final DungeonManager dungeonManager;
    private final LocationManager locationManager;
    
    public PlayerInteractListener(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.mapManager = plugin.getMapManager();
        this.dungeonManager = plugin.getDungeonManager();
        this.locationManager = plugin.getLocationManager();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isFullyInitialized()) {
            return;
        }
        
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || player == null) {
            return;
        }
        
        try {
            // Check if it's a treasure map
            String skill = mapManager.getTreasureMapSkill(item);
            if (skill != null) {
                event.setCancelled(true);
                handleTreasureMapUse(player, skill, item);
                return;
            }
            
            // Check if it's a bell interaction
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BELL) {
                event.setCancelled(true);
                handleBellInteraction(player, event.getClickedBlock().getLocation());
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player interaction for " + player.getName(), e);
            
            String errorMessage = languageManager.getMessage("error-occurred");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
    
    private void handleTreasureMapUse(Player player, String skill, ItemStack mapItem) {
        if (player == null || skill == null || mapItem == null) {
            plugin.getLogger().warning("Invalid parameters in handleTreasureMapUse");
            return;
        }
        
        try {
            String worldName = configManager.getTreasureWorldName();
            if (worldName == null || worldName.trim().isEmpty()) {
                String message = languageManager.getMessage("world-not-configured");
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
            
            World treasureWorld = plugin.getServer().getWorld(worldName);
            if (treasureWorld == null) {
                String message = languageManager.getMessage("world-not-found");
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
            
            // Check if player already has an active dungeon
            if (dataManager.hasActiveDungeon(player.getUniqueId(), skill)) {
                String message = languageManager.getMessage("dungeon-in-progress");
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
            
            // Validate location configuration
            if (!locationManager.validateConfiguration()) {
                String message = languageManager.getMessage("invalid-configuration");
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
            
            // Send initial message
            String processingMessage = languageManager.getMessage("processing-map");
            player.sendMessage(languageManager.getPrefix() + processingMessage);
            
            // Generate location asynchronously
            locationManager.generateSafeDungeonLocation(treasureWorld, player.getUniqueId())
                .thenAccept(location -> {
                    // This runs on async thread, schedule sync task for Bukkit operations
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        try {
                            completeTreasureMapUse(player, skill, mapItem, location, treasureWorld);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Error completing treasure map use for " + player.getName(), e);
                            
                            String errorMessage = languageManager.getMessage("error-occurred");
                            player.sendMessage(languageManager.getPrefix() + errorMessage);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    // Handle location generation failure
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().log(Level.SEVERE, "Failed to generate safe location for " + player.getName(), throwable);
                        
                        String errorMessage = languageManager.getMessage("location-generation-failed");
                        player.sendMessage(languageManager.getPrefix() + errorMessage);
                    });
                    return null;
                });
                
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in handleTreasureMapUse for " + player.getName(), e);
            
            String errorMessage = languageManager.getMessage("error-occurred");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
    
    private void completeTreasureMapUse(Player player, String skill, ItemStack mapItem, Location location, World treasureWorld) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Player is null or offline in completeTreasureMapUse");
            return;
        }
        
        // Select random dungeon type
        String dungeonType = dungeonManager.selectRandomDungeonType();
        if (dungeonType == null) {
            String message = languageManager.getMessage("no-dungeon-types");
            player.sendMessage(languageManager.getPrefix() + message);
            return;
        }
        
        // Store coordinates in data
        dataManager.setActiveDungeon(player.getUniqueId(), skill, treasureWorld.getName(), 
            location.getBlockX(), location.getBlockY(), location.getBlockZ(), dungeonType);
        
        // Store dungeon type for this player
        dungeonManager.setPlayerDungeonType(player.getUniqueId(), dungeonType);
        
        // Teleport player to treasure world spawn
        Location spawnLocation = new Location(treasureWorld,
            configManager.getTreasureSpawnX(),
            configManager.getTreasureSpawnY(),
            configManager.getTreasureSpawnZ());
        
        // Send messages
        String message = languageManager.getMessage("map-used");
        player.sendMessage(languageManager.getPrefix() + message);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.valueOf(location.getBlockX()));
        placeholders.put("z", String.valueOf(location.getBlockZ()));
        placeholders.put("dungeon_type", dungeonType);
        
        String coordsMessage = languageManager.getMessage("coordinates-set", placeholders);
        player.sendMessage(languageManager.getPrefix() + coordsMessage);
        
        // Teleport
        String teleportMessage = languageManager.getMessage("teleporting");
        player.sendMessage(languageManager.getPrefix() + teleportMessage);
        
        CompletableFuture.runAsync(() -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    player.teleport(spawnLocation);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error teleporting player " + player.getName(), e);
                }
            });
        });
        
        // Remove the map item
        try {
            if (mapItem.getAmount() > 1) {
                mapItem.setAmount(mapItem.getAmount() - 1);
            } else {
                player.getInventory().remove(mapItem);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error removing map item from " + player.getName(), e);
        }
    }
    
    private void handleBellInteraction(Player player, Location bellLocation) {
        if (player == null || bellLocation == null) {
            plugin.getLogger().warning("Invalid parameters in handleBellInteraction");
            return;
        }
        
        try {
            // Detect nearby players for party formation
            dungeonManager.getPartyManager().detectNearbyPlayers(player, bellLocation);
            
            String message = languageManager.getMessage("bell-activated");
            player.sendMessage(languageManager.getPrefix() + message);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling bell interaction for " + player.getName(), e);
            
            String errorMessage = languageManager.getMessage("error-occurred");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
}