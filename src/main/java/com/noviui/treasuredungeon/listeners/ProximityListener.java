package com.noviui.treasuredungeon.listeners;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.dungeon.DungeonManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProximityListener implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final DungeonManager dungeonManager;
    
    private final Set<UUID> notifiedPlayers = new HashSet<>();
    private final Set<UUID> bellSpawnedPlayers = new HashSet<>();
    
    public ProximityListener(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.dungeonManager = plugin.getDungeonManager();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Check if player is in treasure world
        String treasureWorldName = configManager.getTreasureWorldName();
        if (!to.getWorld().getName().equals(treasureWorldName)) {
            return;
        }
        
        // Check for active dungeons
        for (String skill : configManager.getEnabledSkills()) {
            if (!dataManager.hasActiveDungeon(player.getUniqueId(), skill)) {
                continue;
            }
            
            int[] coords = dataManager.getDungeonCoords(player.getUniqueId(), skill);
            Location dungeonLocation = new Location(to.getWorld(), coords[0], coords[1], coords[2]);
            
            double distance = to.distance(dungeonLocation);
            
            // Notify when approaching (50 blocks)
            if (distance <= 50 && !notifiedPlayers.contains(player.getUniqueId())) {
                String message = languageManager.getMessage("approaching-treasure");
                player.sendMessage(languageManager.getPrefix() + message);
                notifiedPlayers.add(player.getUniqueId());
            }
            
            // Spawn bell when very close (10 blocks)
            if (distance <= 10 && !bellSpawnedPlayers.contains(player.getUniqueId())) {
                spawnBell(player, skill, dungeonLocation);
                bellSpawnedPlayers.add(player.getUniqueId());
            }
        }
    }
    
    private void spawnBell(Player player, String skill, Location location) {
        // Get the dungeon type for this player
        String dungeonType = dataManager.getDungeonType(player.getUniqueId(), skill);
        if (dungeonType == null) {
            plugin.getLogger().warning("No dungeon type found for player " + player.getName() + " skill " + skill);
            return;
        }
        
        // Load bell schematic
        String bellSchematic = configManager.getDungeonTypeBellSchematic(dungeonType);
        if (bellSchematic != null && plugin.getIntegrationManager().isWorldEditEnabled()) {
            dungeonManager.loadSchematic(bellSchematic, location);
        } else {
            // Fallback: place a simple bell
            location.getBlock().setType(org.bukkit.Material.BELL);
        }
        
        // Send message
        String message = languageManager.getMessage("bell-found");
        player.sendMessage(languageManager.getPrefix() + message);
        
        // Store bell location for this player
        dungeonManager.setBellLocation(player.getUniqueId(), skill, location);
    }
    
    public void clearNotifications(UUID playerId) {
        notifiedPlayers.remove(playerId);
        bellSpawnedPlayers.remove(playerId);
    }
}