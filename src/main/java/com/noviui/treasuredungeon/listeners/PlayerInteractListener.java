package com.noviui.treasuredungeon.listeners;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.dungeon.DungeonManager;
import com.noviui.treasuredungeon.map.MapManager;
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
import java.util.concurrent.ThreadLocalRandom;

public class PlayerInteractListener implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final MapManager mapManager;
    private final DungeonManager dungeonManager;
    
    public PlayerInteractListener(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.mapManager = plugin.getMapManager();
        this.dungeonManager = plugin.getDungeonManager();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) {
            return;
        }
        
        // Check if it's a treasure map
        String skill = mapManager.getTreasureMapSkill(item);
        if (skill != null) {
            event.setCancelled(true);
            handleTreasureMapUse(player, skill, item);
            return;
        }
        
        // Check if it's a bell interaction
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BELL) {
            handleBellInteraction(player, event.getClickedBlock().getLocation());
        }
    }
    
    private void handleTreasureMapUse(Player player, String skill, ItemStack mapItem) {
        String worldName = configManager.getTreasureWorldName();
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
        
        // Generate random coordinates within limits
        int minX = configManager.getMinX();
        int maxX = configManager.getMaxX();
        int minZ = configManager.getMinZ();
        int maxZ = configManager.getMaxZ();
        
        int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
        int y = treasureWorld.getHighestBlockYAt(x, z) + 1;
        
        // Select random dungeon type
        String dungeonType = dungeonManager.selectRandomDungeonType();
        if (dungeonType == null) {
            String message = "§cNo dungeon types configured!";
            player.sendMessage(languageManager.getPrefix() + message);
            return;
        }
        
        // Store coordinates in data
        dataManager.setActiveDungeon(player.getUniqueId(), skill, worldName, x, y, z, dungeonType);
        
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
        placeholders.put("x", String.valueOf(x));
        placeholders.put("z", String.valueOf(z));
        String coordsMessage = languageManager.getMessage("coordinates-set", placeholders);
        player.sendMessage(languageManager.getPrefix() + coordsMessage);
        
        // Teleport
        String teleportMessage = languageManager.getMessage("teleporting");
        player.sendMessage(languageManager.getPrefix() + teleportMessage);
        
        player.teleport(spawnLocation);
        
        // Remove the map item
        if (mapItem.getAmount() > 1) {
            mapItem.setAmount(mapItem.getAmount() - 1);
        } else {
            player.getInventory().remove(mapItem);
        }
    }
    
    private void handleBellInteraction(Player player, Location bellLocation) {
        // Find which skill this bell belongs to
        String skill = dungeonManager.getBellSkill(player.getUniqueId(), bellLocation);
        if (skill == null) {
            return;
        }
        
        // Ring the bell and start dungeon
        String message = languageManager.getMessage("bell-rung");
        player.sendMessage(languageManager.getPrefix() + message);
        
        dungeonManager.startDungeon(player, skill, bellLocation);
    }
}