package com.noviui.treasuredungeon.dungeon;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.integration.IntegrationManager;
import com.noviui.treasuredungeon.utils.LocationManager;
import com.noviui.treasuredungeon.utils.TimeUtils;
import io.lumine.mythic.api.MythicApi;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class DungeonManager {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final IntegrationManager integrationManager;
    private final LocationManager locationManager;
    private final SpawnManager spawnManager;
    private final PartyManager partyManager;
    
    // Active dungeons tracking
    private final Map<UUID, String> playerActiveDungeon = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerDungeonType = new ConcurrentHashMap<>();
    private final Map<UUID, PartyManager.DungeonDifficulty> playerDifficulty = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Location>> playerBellLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> bossDamageTracker = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    
    public DungeonManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.integrationManager = plugin.getIntegrationManager();
        this.locationManager = plugin.getLocationManager();
        this.spawnManager = new SpawnManager(plugin);
        this.partyManager = new PartyManager(plugin);
    }
    
    public void startDungeonAtLocation(Location buildLocation, String dungeonType, UUID initiatorId, List<Player> participants) {
        if (buildLocation == null || dungeonType == null || initiatorId == null || participants == null) {
            plugin.getLogger().warning("Invalid parameters in startDungeonAtLocation");
            return;
        }
        
        try {
            // Mark dungeon as active for all participants
            for (Player participant : participants) {
                if (participant != null) {
                    String skill = getPlayerSkill(participant.getUniqueId());
                    if (skill != null) {
                        playerActiveDungeon.put(participant.getUniqueId(), skill);
                        playerDungeonType.put(participant.getUniqueId(), dungeonType);
                    }
                }
            }
            
            // Send start message
            String message = languageManager.getMessage("dungeon-started");
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + message);
                }
            }
            
            // Start waves with scaled difficulty
            startWavesWithDifficulty(participants, dungeonType, buildLocation);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting dungeon at location", e);
            
            // Cleanup on error
            for (Player participant : participants) {
                if (participant != null) {
                    String skill = getPlayerSkill(participant.getUniqueId());
                    if (skill != null) {
                        cleanupPlayerDungeon(participant.getUniqueId(), skill);
                    }
                }
            }
        }
    }
    
    private void startWavesWithDifficulty(List<Player> participants, String dungeonType, Location location) {
        if (participants == null || participants.isEmpty() || dungeonType == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in startWavesWithDifficulty");
            return;
        }
        
        UUID initiatorId = participants.get(0).getUniqueId();
        PartyManager.DungeonDifficulty difficulty = playerDifficulty.get(initiatorId);
        
        if (difficulty == null) {
            // Default difficulty for solo
            difficulty = new PartyManager.DungeonDifficulty("Solo", 1.0, 1.0, 1.0);
        }
        
        try {
            int waveCount = configManager.getDungeonTypeWaveCount(dungeonType);
            
            if (waveCount <= 0) {
                plugin.getLogger().warning("Invalid wave count for dungeon type: " + dungeonType);
                spawnBossWithDifficulty(participants, dungeonType, location, difficulty);
                return;
            }
            
            BukkitTask task = new BukkitRunnable() {
                int currentWave = 1;
                
                @Override
                public void run() {
                    try {
                        // Check if any participants are still online and in the right world
                        List<Player> onlineParticipants = new ArrayList<>();
                        for (Player participant : participants) {
                            if (participant != null && participant.isOnline() && 
                                participant.getWorld().equals(location.getWorld())) {
                                onlineParticipants.add(participant);
                            }
                        }
                        
                        if (onlineParticipants.isEmpty()) {
                            this.cancel();
                            for (Player participant : participants) {
                                if (participant != null) {
                                    String skill = getPlayerSkill(participant.getUniqueId());
                                    if (skill != null) {
                                        cleanupPlayerDungeon(participant.getUniqueId(), skill);
                                    }
                                }
                            }
                            return;
                        }
                        
                        if (currentWave <= waveCount) {
                            spawnWaveWithDifficulty(onlineParticipants, dungeonType, currentWave, location, difficulty);
                            currentWave++;
                        } else {
                            // All waves completed, spawn boss
                            this.cancel();
                            spawnBossWithDifficulty(onlineParticipants, dungeonType, location, difficulty);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error in wave task", e);
                        this.cancel();
                        for (Player participant : participants) {
                            if (participant != null) {
                                String skill = getPlayerSkill(participant.getUniqueId());
                                if (skill != null) {
                                    cleanupPlayerDungeon(participant.getUniqueId(), skill);
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()));
            
            activeTasks.put(initiatorId, task);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting waves with difficulty", e);
            for (Player participant : participants) {
                if (participant != null) {
                    String skill = getPlayerSkill(participant.getUniqueId());
                    if (skill != null) {
                        cleanupPlayerDungeon(participant.getUniqueId(), skill);
                    }
                }
            }
        }
    }
    
    private void spawnWaveWithDifficulty(List<Player> participants, String dungeonType, int wave, 
                                       Location location, PartyManager.DungeonDifficulty difficulty) {
        if (participants == null || participants.isEmpty() || dungeonType == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in spawnWaveWithDifficulty");
            return;
        }
        
        try {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("wave", String.valueOf(wave));
            placeholders.put("difficulty", difficulty.getName());
            
            String message = languageManager.getMessage("wave-starting", placeholders);
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + message);
                }
            }
            
            // Get mobs for this wave
            List<String> baseMobs = configManager.getDungeonTypeWaveMobs(dungeonType, wave);
            
            if (baseMobs != null && !baseMobs.isEmpty() && integrationManager.isMythicMobsEnabled()) {
                // Scale mob count based on difficulty
                int scaledMobCount = (int) Math.ceil(baseMobs.size() * difficulty.getMobMultiplier());
                List<String> scaledMobs = new ArrayList<>();
                
                // Duplicate mobs to reach scaled count
                for (int i = 0; i < scaledMobCount; i++) {
                    scaledMobs.add(baseMobs.get(i % baseMobs.size()));
                }
                
                // Find spawn locations for scaled wave
                List<Location> spawnLocations = spawnManager.findMobSpawnLocations(location, scaledMobs.size());
                
                int spawnIndex = 0;
                for (String mobId : scaledMobs) {
                    if (mobId != null && !mobId.trim().isEmpty()) {
                        Location spawnLoc = spawnIndex < spawnLocations.size() ? 
                            spawnLocations.get(spawnIndex) : location;
                        spawnMythicMob(mobId, spawnLoc);
                        spawnIndex++;
                    }
                }
            } else {
                plugin.getLogger().warning("No mobs configured for wave " + wave + " of dungeon type " + dungeonType);
            }
            
            // Schedule wave completion message
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Map<String, String> completePlaceholders = new HashMap<>();
                    completePlaceholders.put("wave", String.valueOf(wave));
                    
                    String completeMessage = languageManager.getMessage("wave-completed", completePlaceholders);
                    for (Player participant : participants) {
                        if (participant != null && participant.isOnline()) {
                            participant.sendMessage(languageManager.getPrefix() + completeMessage);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error sending wave completion message", e);
                }
            }, Math.max(20L, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()) - 20L));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error spawning wave with difficulty", e);
        }
    }
    
    private void spawnBossWithDifficulty(List<Player> participants, String dungeonType, 
                                       Location location, PartyManager.DungeonDifficulty difficulty) {
        if (participants == null || participants.isEmpty() || dungeonType == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in spawnBossWithDifficulty");
            return;
        }
        
        try {
            String bossMessage = languageManager.getMessage("boss-incoming");
            for (Player participant : participants) {
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(languageManager.getPrefix() + bossMessage);
                }
            }
            
            // Delay before boss spawn
            String spawnDelay = configManager.getDungeonTypeBossSpawnDelay(dungeonType);
            long delayTicks = TimeUtils.parseTimeToTicks(spawnDelay);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Check if any participants are still online
                    List<Player> onlineParticipants = new ArrayList<>();
                    for (Player participant : participants) {
                        if (participant != null && participant.isOnline()) {
                            onlineParticipants.add(participant);
                        }
                    }
                    
                    if (onlineParticipants.isEmpty()) {
                        return;
                    }
                    
                    String bossId = configManager.getDungeonTypeBoss(dungeonType);
                    if (bossId != null && !bossId.trim().isEmpty() && integrationManager.isMythicMobsEnabled()) {
                        // Find optimal boss spawn location
                        Location bossSpawnLocation = spawnManager.findBossSpawnLocation(location);
                        Entity boss = spawnMythicMob(bossId, bossSpawnLocation);
                        if (boss != null) {
                            // Initialize damage tracking
                            Map<UUID, Double> damageMap = new ConcurrentHashMap<>();
                            for (Player participant : onlineParticipants) {
                                damageMap.put(participant.getUniqueId(), 0.0);
                            }
                            bossDamageTracker.put(boss.getUniqueId(), damageMap);
                            
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("boss", bossId);
                            placeholders.put("difficulty", difficulty.getName());
                            String spawnedMessage = languageManager.getMessage("boss-spawned", placeholders);
                            
                            for (Player participant : onlineParticipants) {
                                participant.sendMessage(languageManager.getPrefix() + spawnedMessage);
                            }
                        } else {
                            plugin.getLogger().warning("Failed to spawn boss " + bossId);
                        }
                    } else {
                        plugin.getLogger().warning("No boss configured for dungeon type " + dungeonType);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in boss spawn task", e);
                }
            }, Math.max(20L, delayTicks));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error spawning boss with difficulty", e);
        }
    }
    
    public String getPlayerSkill(UUID playerId) {
        // This would need to be implemented to track which skill triggered the dungeon
        // For now, return a default or check active dungeons
        for (String skill : configManager.getEnabledSkills()) {
            if (dataManager.hasActiveDungeon(playerId, skill)) {
                return skill;
            }
        }
        return null;
    }
    
    public void setPartyDifficulty(UUID initiatorId, PartyManager.DungeonDifficulty difficulty) {
        if (initiatorId != null && difficulty != null) {
            playerDifficulty.put(initiatorId, difficulty);
        }
    }
    
    public String getPlayerDungeonType(UUID playerId) {
        return playerDungeonType.get(playerId);
    }
    
    public PartyManager getPartyManager() {
        return partyManager;
    }
    
    public void startDungeon(Player player, String skill, Location bellLocation) {
        if (player == null || skill == null || bellLocation == null) {
            plugin.getLogger().warning("Invalid parameters in startDungeon");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        try {
            // Remove bell
            bellLocation.getBlock().setType(Material.AIR);
            
            // Load dungeon schematic
            String dungeonType = playerDungeonType.get(playerId);
            if (dungeonType == null) {
                plugin.getLogger().warning("No dungeon type found for player " + player.getName());
                return;
            }
            
            String dungeonSchematic = configManager.getDungeonTypeDungeonSchematic(dungeonType);
            if (dungeonSchematic != null && !dungeonSchematic.trim().isEmpty() && integrationManager.isWorldEditEnabled()) {
                loadSchematic(dungeonSchematic, bellLocation);
            }
            
            // Mark dungeon as active
            playerActiveDungeon.put(playerId, skill);
            
            // Send start message
            String message = languageManager.getMessage("dungeon-started");
            player.sendMessage(languageManager.getPrefix() + message);
            
            // Start waves
            startWaves(player, skill, bellLocation);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting dungeon for player " + player.getName(), e);
            
            // Cleanup on error
            cleanupPlayerDungeon(playerId, skill);
            
            String errorMessage = languageManager.getMessage("dungeon-start-failed");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
    
    private void startWaves(Player player, String skill, Location location) {
        if (player == null || skill == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in startWaves");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String dungeonType = playerDungeonType.get(playerId);
        
        if (dungeonType == null) {
            plugin.getLogger().warning("No dungeon type found for player " + player.getName());
            return;
        }
        
        try {
            int waveCount = configManager.getDungeonTypeWaveCount(dungeonType);
            
            if (waveCount <= 0) {
                plugin.getLogger().warning("Invalid wave count for dungeon type: " + dungeonType);
                spawnBoss(player, dungeonType, location);
                return;
            }
            
            BukkitTask task = new BukkitRunnable() {
                int currentWave = 1;
                
                @Override
                public void run() {
                    try {
                        // Check if player is still online and in the right world
                        if (!player.isOnline() || !player.getWorld().equals(location.getWorld())) {
                            this.cancel();
                            cleanupPlayerDungeon(playerId, skill);
                            return;
                        }
                        
                        if (currentWave <= waveCount) {
                            spawnWave(player, dungeonType, currentWave, location);
                            currentWave++;
                        } else {
                            // All waves completed, spawn boss
                            this.cancel();
                            spawnBoss(player, dungeonType, location);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error in wave task for player " + player.getName(), e);
                        this.cancel();
                        cleanupPlayerDungeon(playerId, skill);
                    }
                }
            }.runTaskTimer(plugin, 0L, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()));
            
            activeTasks.put(playerId, task);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting waves for player " + player.getName(), e);
            cleanupPlayerDungeon(playerId, skill);
        }
    }
    
    private void spawnWave(Player player, String dungeonType, int wave, Location location) {
        if (player == null || !player.isOnline() || dungeonType == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in spawnWave");
            return;
        }
        
        try {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("wave", String.valueOf(wave));
            
            String message = languageManager.getMessage("wave-starting", placeholders);
            player.sendMessage(languageManager.getPrefix() + message);
            
            // Get mobs for this wave
            List<String> mobs = configManager.getDungeonTypeWaveMobs(dungeonType, wave);
            
            if (mobs != null && !mobs.isEmpty() && integrationManager.isMythicMobsEnabled()) {
                // Find spawn locations for this wave
                List<Location> spawnLocations = spawnManager.findMobSpawnLocations(location, mobs.size());
                
                int spawnIndex = 0;
                for (String mobId : mobs) {
                    if (mobId != null && !mobId.trim().isEmpty()) {
                        Location spawnLoc = spawnIndex < spawnLocations.size() ? 
                            spawnLocations.get(spawnIndex) : location;
                        spawnMythicMob(mobId, spawnLoc);
                        spawnIndex++;
                    }
                }
            } else {
                plugin.getLogger().warning("No mobs configured for wave " + wave + " of dungeon type " + dungeonType);
            }
            
            // Schedule wave completion message
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (player.isOnline()) {
                        String completeMessage = languageManager.getMessage("wave-completed", placeholders);
                        player.sendMessage(languageManager.getPrefix() + completeMessage);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error sending wave completion message", e);
                }
            }, Math.max(20L, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()) - 20L));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error spawning wave " + wave + " for player " + player.getName(), e);
        }
    }
    
    private void spawnBoss(Player player, String dungeonType, Location location) {
        if (player == null || !player.isOnline() || dungeonType == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in spawnBoss");
            return;
        }
        
        try {
            String bossMessage = languageManager.getMessage("boss-incoming");
            player.sendMessage(languageManager.getPrefix() + bossMessage);
            
            // Delay before boss spawn
            String spawnDelay = configManager.getDungeonTypeBossSpawnDelay(dungeonType);
            long delayTicks = TimeUtils.parseTimeToTicks(spawnDelay);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Check if player is still online
                    if (!player.isOnline()) {
                        return;
                    }
                    
                    String bossId = configManager.getDungeonTypeBoss(dungeonType);
                    if (bossId != null && !bossId.trim().isEmpty() && integrationManager.isMythicMobsEnabled()) {
                        // Find optimal boss spawn location
                        Location bossSpawnLocation = spawnManager.findBossSpawnLocation(location);
                        Entity boss = spawnMythicMob(bossId, bossSpawnLocation);
                        if (boss != null) {
                            // Initialize damage tracking
                            bossDamageTracker.put(boss.getUniqueId(), new ConcurrentHashMap<>());
                            
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("boss", bossId);
                            String spawnedMessage = languageManager.getMessage("boss-spawned", placeholders);
                            player.sendMessage(languageManager.getPrefix() + spawnedMessage);
                        } else {
                            plugin.getLogger().warning("Failed to spawn boss " + bossId + " for player " + player.getName());
                        }
                    } else {
                        plugin.getLogger().warning("No boss configured for dungeon type " + dungeonType);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in boss spawn task for player " + player.getName(), e);
                }
            }, Math.max(20L, delayTicks));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error spawning boss for player " + player.getName(), e);
        }
    }
    
    private Entity spawnMythicMob(String mobId, Location location) {
        if (!integrationManager.isMythicMobsEnabled()) {
            plugin.getLogger().warning("Attempted to spawn MythicMob but MythicMobs is not enabled");
            return null;
        }
        
        if (mobId == null || mobId.trim().isEmpty() || location == null) {
            plugin.getLogger().warning("Invalid parameters for MythicMob spawn: mobId=" + mobId + ", location=" + location);
            return null;
        }
        
        try {
            MythicApi mythicApi = MythicBukkit.inst().getAPIHelper();
            return mythicApi.spawnMythicMob(mobId, location);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to spawn MythicMob: " + mobId, e);
            return null;
        }
    }
    
    public void handleBossDefeat(LivingEntity boss, Player killer) {
        if (boss == null || killer == null) {
            plugin.getLogger().warning("Invalid parameters in handleBossDefeat");
            return;
        }
        
        UUID bossId = boss.getUniqueId();
        Map<UUID, Double> damageMap = bossDamageTracker.get(bossId);
        
        try {
            // Send boss defeated message
            String message = languageManager.getMessage("boss-defeated");
            killer.sendMessage(languageManager.getPrefix() + message);
            
            // Show damage ranking
            if (configManager.isDamageTrackingEnabled() && damageMap != null) {
                showDamageRanking(killer, damageMap);
            }
            
            // Spawn loot chest
            spawnLootChest(killer, boss.getLocation());
            
            // Clean up dungeon
            String skill = playerActiveDungeon.get(killer.getUniqueId());
            if (skill != null) {
                completeDungeon(killer, skill);
            }
            
            // Remove damage tracking
            bossDamageTracker.remove(bossId);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling boss defeat for player " + killer.getName(), e);
        }
    }
    
    private void showDamageRanking(Player player, Map<UUID, Double> damageMap) {
        if (player == null || !player.isOnline() || damageMap == null || damageMap.isEmpty()) {
            return;
        }
        
        try {
            // Sort by damage
            List<Map.Entry<UUID, Double>> sortedEntries = new ArrayList<>(damageMap.entrySet());
            sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            
            // Show top 3
            player.sendMessage("");
            player.sendMessage(languageManager.getMessage("damage-ranking-title"));
            
            for (int i = 0; i < Math.min(3, sortedEntries.size()); i++) {
                Map.Entry<UUID, Double> entry = sortedEntries.get(i);
                Player damagePlayer = Bukkit.getPlayer(entry.getKey());
                String playerName = damagePlayer != null ? damagePlayer.getName() : "Unknown";
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("position", String.valueOf(i + 1));
                placeholders.put("player", playerName);
                placeholders.put("damage", String.valueOf(entry.getValue().intValue()));
                
                String rankingEntry = languageManager.getMessage("damage-ranking-entry", placeholders);
                player.sendMessage(rankingEntry);
            }
            
            // Show personal damage if not in top 3
            if (configManager.showPersonalDamage()) {
                Double personalDamage = damageMap.get(player.getUniqueId());
                if (personalDamage != null && sortedEntries.stream().limit(3)
                        .noneMatch(entry -> entry.getKey().equals(player.getUniqueId()))) {
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("damage", String.valueOf(personalDamage.intValue()));
                    String personalMessage = languageManager.getMessage("personal-damage", placeholders);
                    player.sendMessage(personalMessage);
                }
            }
            
            // Schedule message cleanup
            long showDuration = TimeUtils.parseTimeToTicks(configManager.getDamageShowDuration());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (player.isOnline()) {
                        for (int i = 0; i < 10; i++) {
                            player.sendMessage("");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error clearing damage ranking messages", e);
                }
            }, Math.max(20L, showDuration));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error showing damage ranking for player " + player.getName(), e);
        }
    }
    
    private void spawnLootChest(Player player, Location location) {
        if (player == null || !player.isOnline() || location == null) {
            plugin.getLogger().warning("Invalid parameters in spawnLootChest");
            return;
        }
        
        try {
            // Place chest
            location.getBlock().setType(Material.CHEST);
            
            String dungeonType = playerDungeonType.get(player.getUniqueId());
            if (dungeonType != null) {
                // Execute loot commands
                String lootType = configManager.getDungeonTypeLootType(dungeonType);
                if ("commands".equals(lootType)) {
                    List<String> commands = configManager.getDungeonTypeLootCommands(dungeonType);
                    if (commands != null && !commands.isEmpty()) {
                        for (String command : commands) {
                            if (command != null && !command.trim().isEmpty()) {
                                try {
                                    String processedCommand = command.replace("{player}", player.getName());
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.WARNING, "Error executing loot command: " + command, e);
                                }
                            }
                        }
                    }
                }
            }
            
            // Send message
            String message = languageManager.getMessage("chest-spawned");
            player.sendMessage(languageManager.getPrefix() + message);
            
            // Schedule chest cleanup
            long timeout = TimeUtils.parseTimeToTicks(configManager.getChestTimeout());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (location.getBlock().getType() == Material.CHEST) {
                        location.getBlock().setType(Material.AIR);
                        if (player.isOnline()) {
                            String timeoutMessage = languageManager.getMessage("chest-timeout");
                            player.sendMessage(languageManager.getPrefix() + timeoutMessage);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error during chest cleanup", e);
                }
            }, Math.max(20L, timeout));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error spawning loot chest for player " + player.getName(), e);
        }
    }
    
    private void completeDungeon(Player player, String skill) {
        if (player == null || skill == null) {
            plugin.getLogger().warning("Invalid parameters in completeDungeon");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        try {
            // Get dungeon location before clearing data
            int[] coords = dataManager.getDungeonCoords(playerId, skill);
            Location dungeonLocation = new Location(player.getWorld(), coords[0], coords[1], coords[2]);
            
            // Release location in LocationManager
            locationManager.releaseLocation(dungeonLocation, playerId);
            
            // Clear active dungeon
            cleanupPlayerDungeon(playerId, skill);
            
            // Allow new map generation
            dataManager.setMapReceived(playerId, skill, false);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error completing dungeon for player " + player.getName(), e);
        }
    }
    
    private void cleanupPlayerDungeon(UUID playerId, String skill) {
        if (playerId == null || skill == null) {
            return;
        }
        
        try {
            // Clear active dungeon
            playerActiveDungeon.remove(playerId);
            playerDungeonType.remove(playerId);
            dataManager.clearActiveDungeon(playerId, skill);
            
            // Cancel any active tasks
            BukkitTask task = activeTasks.get(playerId);
            if (task != null && !task.isCancelled()) {
                task.cancel();
                activeTasks.remove(playerId);
            }
            
            // Clear bell location
            Map<String, Location> bellMap = playerBellLocations.get(playerId);
            if (bellMap != null) {
                bellMap.remove(skill);
                if (bellMap.isEmpty()) {
                    playerBellLocations.remove(playerId);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during player dungeon cleanup for " + playerId, e);
        }
    }
    
    public void loadSchematic(String schematicName, Location location) {
        if (!integrationManager.isWorldEditEnabled()) {
            plugin.getLogger().warning("Attempted to load schematic but WorldEdit is not enabled");
            return;
        }
        
        if (schematicName == null || schematicName.trim().isEmpty() || location == null) {
            plugin.getLogger().warning("Invalid parameters for schematic loading: name=" + schematicName + ", location=" + location);
            return;
        }
        
        try {
            File schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName);
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found: " + schematicName);
                return;
            }
            
            // WorldEdit integration would go here
            // This is a simplified placeholder
            plugin.getLogger().info("Loading schematic: " + schematicName + " at " + location);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load schematic: " + schematicName, e);
        }
    }
    
    public void setBellLocation(UUID playerId, String skill, Location location) {
        if (playerId == null || skill == null || location == null) {
            plugin.getLogger().warning("Invalid parameters in setBellLocation");
            return;
        }
        
        playerBellLocations.computeIfAbsent(playerId, k -> new HashMap<>()).put(skill, location);
    }
    
    public String getBellSkill(UUID playerId, Location bellLocation) {
        if (playerId == null || bellLocation == null) {
            return null;
        }
        
        Map<String, Location> bellMap = playerBellLocations.get(playerId);
        if (bellMap == null) {
            return null;
        }
        
        try {
            for (Map.Entry<String, Location> entry : bellMap.entrySet()) {
                Location storedLocation = entry.getValue();
                if (storedLocation != null && storedLocation.getWorld().equals(bellLocation.getWorld()) &&
                    storedLocation.distance(bellLocation) < 5) {
                    return entry.getKey();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding bell skill for player " + playerId, e);
        }
        
        return null;
    }
    
    public String selectRandomDungeonType() {
        try {
            Set<String> dungeonTypes = configManager.getDungeonTypes();
            if (dungeonTypes == null || dungeonTypes.isEmpty()) {
                plugin.getLogger().warning("No dungeon types configured");
                return null;
            }
            
            // Calculate total weight
            int totalWeight = 0;
            Map<String, Integer> weights = new HashMap<>();
            
            for (String dungeonType : dungeonTypes) {
                if (dungeonType != null && !dungeonType.trim().isEmpty()) {
                    int weight = Math.max(1, configManager.getDungeonTypeWeight(dungeonType));
                    weights.put(dungeonType, weight);
                    totalWeight += weight;
                }
            }
            
            if (totalWeight == 0 || weights.isEmpty()) {
                plugin.getLogger().warning("No valid dungeon types with weights found");
                return null;
            }
            
            // Weighted random selection
            int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);
            int currentWeight = 0;
            
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                currentWeight += entry.getValue();
                if (randomValue < currentWeight) {
                    return entry.getKey();
                }
            }
            
            // Fallback (should never reach here)
            return weights.keySet().iterator().next();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error selecting random dungeon type", e);
            return null;
        }
    }
    
    public void setPlayerDungeonType(UUID playerId, String dungeonType) {
        if (playerId == null || dungeonType == null || dungeonType.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters in setPlayerDungeonType");
            return;
        }
        
        playerDungeonType.put(playerId, dungeonType);
    }
    
    public void addBossDamage(UUID bossId, UUID playerId, double damage) {
        if (bossId == null || playerId == null || damage < 0) {
            return;
        }
        
        Map<UUID, Double> damageMap = bossDamageTracker.get(bossId);
        if (damageMap != null) {
            damageMap.merge(playerId, damage, Double::sum);
        }
    }
    
    public void cleanup() {
        try {
            // Cancel all active tasks
            for (BukkitTask task : activeTasks.values()) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
            activeTasks.clear();
            
            // Clear all tracking data
            playerActiveDungeon.clear();
            playerDungeonType.clear();
            playerBellLocations.clear();
            bossDamageTracker.clear();
            
            plugin.getLogger().info("Dungeon manager cleanup completed");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during dungeon manager cleanup", e);
        }
    }
}