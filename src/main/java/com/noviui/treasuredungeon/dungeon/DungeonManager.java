package com.noviui.treasuredungeon.dungeon;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.integration.IntegrationManager;
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

public class DungeonManager {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final IntegrationManager integrationManager;
    
    // Active dungeons tracking
    private final Map<UUID, String> playerActiveDungeon = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerDungeonType = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Location>> playerBellLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> bossDamageTracker = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    
    public DungeonManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.integrationManager = plugin.getIntegrationManager();
    }
    
    public void startDungeon(Player player, String skill, Location bellLocation) {
        UUID playerId = player.getUniqueId();
        
        // Remove bell
        bellLocation.getBlock().setType(Material.AIR);
        
        // Load dungeon schematic
        String dungeonType = playerDungeonType.get(playerId);
        if (dungeonType == null) {
            plugin.getLogger().warning("No dungeon type found for player " + player.getName());
            return;
        }
        
        String dungeonSchematic = configManager.getDungeonTypeDungeonSchematic(dungeonType);
        if (dungeonSchematic != null && integrationManager.isWorldEditEnabled()) {
            loadSchematic(dungeonSchematic, bellLocation);
        }
        
        // Mark dungeon as active
        playerActiveDungeon.put(playerId, skill);
        
        // Send start message
        String message = languageManager.getMessage("dungeon-started");
        player.sendMessage(languageManager.getPrefix() + message);
        
        // Start waves
        startWaves(player, skill, bellLocation);
    }
    
    private void startWaves(Player player, String skill, Location location) {
        UUID playerId = player.getUniqueId();
        String dungeonType = playerDungeonType.get(playerId);
        if (dungeonType == null) {
            plugin.getLogger().warning("No dungeon type found for player " + player.getName());
            return;
        }
        
        int waveCount = configManager.getDungeonTypeWaveCount(dungeonType);
        
        BukkitTask task = new BukkitRunnable() {
            int currentWave = 1;
            
            @Override
            public void run() {
                if (currentWave <= waveCount) {
                    spawnWave(player, dungeonType, currentWave, location);
                    currentWave++;
                } else {
                    // All waves completed, spawn boss
                    this.cancel();
                    spawnBoss(player, dungeonType, location);
                }
            }
        }.runTaskTimer(plugin, 0L, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()));
        
        activeTasks.put(playerId, task);
    }
    
    private void spawnWave(Player player, String dungeonType, int wave, Location location) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("wave", String.valueOf(wave));
        
        String message = languageManager.getMessage("wave-starting", placeholders);
        player.sendMessage(languageManager.getPrefix() + message);
        
        // Get mobs for this wave
        List<String> mobs = configManager.getDungeonTypeWaveMobs(dungeonType, wave);
        
        if (mobs != null && !mobs.isEmpty() && integrationManager.isMythicMobsEnabled()) {
            for (String mobId : mobs) {
                spawnMythicMob(mobId, location);
            }
        }
        
        // Schedule wave completion message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String completeMessage = languageManager.getMessage("wave-completed", placeholders);
            player.sendMessage(languageManager.getPrefix() + completeMessage);
        }, TimeUtils.parseTimeToTicks(configManager.getWaveDelay()) - 20L);
    }
    
    private void spawnBoss(Player player, String dungeonType, Location location) {
        String bossMessage = languageManager.getMessage("boss-incoming");
        player.sendMessage(languageManager.getPrefix() + bossMessage);
        
        // Delay before boss spawn
        String spawnDelay = configManager.getDungeonTypeBossSpawnDelay(dungeonType);
        long delayTicks = TimeUtils.parseTimeToTicks(spawnDelay);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String bossId = configManager.getDungeonTypeBoss(dungeonType);
            if (bossId != null && integrationManager.isMythicMobsEnabled()) {
                Entity boss = spawnMythicMob(bossId, location);
                if (boss != null) {
                    // Initialize damage tracking
                    bossDamageTracker.put(boss.getUniqueId(), new ConcurrentHashMap<>());
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("boss", bossId);
                    String spawnedMessage = languageManager.getMessage("boss-spawned", placeholders);
                    player.sendMessage(languageManager.getPrefix() + spawnedMessage);
                }
            }
        }, delayTicks);
    }
    
    private Entity spawnMythicMob(String mobId, Location location) {
        if (!integrationManager.isMythicMobsEnabled()) {
            return null;
        }
        
        try {
            MythicApi mythicApi = MythicBukkit.inst().getAPIHelper();
            return mythicApi.spawnMythicMob(mobId, location);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn MythicMob: " + mobId);
            return null;
        }
    }
    
    public void handleBossDefeat(LivingEntity boss, Player killer) {
        UUID bossId = boss.getUniqueId();
        Map<UUID, Double> damageMap = bossDamageTracker.get(bossId);
        
        if (damageMap == null) {
            return;
        }
        
        // Send boss defeated message
        String message = languageManager.getMessage("boss-defeated");
        killer.sendMessage(languageManager.getPrefix() + message);
        
        // Show damage ranking
        if (configManager.isDamageTrackingEnabled()) {
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
    }
    
    private void showDamageRanking(Player player, Map<UUID, Double> damageMap) {
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
            for (int i = 0; i < 10; i++) {
                player.sendMessage("");
            }
        }, showDuration);
    }
    
    private void spawnLootChest(Player player, Location location) {
        // Place chest
        location.getBlock().setType(Material.CHEST);
        
        String dungeonType = playerDungeonType.get(player.getUniqueId());
        if (dungeonType != null) {
            // Execute loot commands
            String lootType = configManager.getDungeonTypeLootType(dungeonType);
            if ("commands".equals(lootType)) {
                List<String> commands = configManager.getDungeonTypeLootCommands(dungeonType);
                if (commands != null) {
                    for (String command : commands) {
                        String processedCommand = command.replace("{player}", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
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
            if (location.getBlock().getType() == Material.CHEST) {
                location.getBlock().setType(Material.AIR);
                String timeoutMessage = languageManager.getMessage("chest-timeout");
                player.sendMessage(languageManager.getPrefix() + timeoutMessage);
            }
        }, timeout);
    }
    
    private void completeDungeon(Player player, String skill) {
        UUID playerId = player.getUniqueId();
        
        // Clear active dungeon
        playerActiveDungeon.remove(playerId);
        playerDungeonType.remove(playerId);
        dataManager.clearActiveDungeon(playerId, skill);
        
        // Cancel any active tasks
        BukkitTask task = activeTasks.get(playerId);
        if (task != null) {
            task.cancel();
            activeTasks.remove(playerId);
        }
        
        // Clear bell location
        Map<String, Location> bellMap = playerBellLocations.get(playerId);
        if (bellMap != null) {
            bellMap.remove(skill);
        }
        
        // Allow new map generation
        dataManager.setMapReceived(playerId, skill, false);
    }
    
    public void loadSchematic(String schematicName, Location location) {
        if (!integrationManager.isWorldEditEnabled()) {
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
            plugin.getLogger().warning("Failed to load schematic: " + schematicName);
        }
    }
    
    public void setBellLocation(UUID playerId, String skill, Location location) {
        playerBellLocations.computeIfAbsent(playerId, k -> new HashMap<>()).put(skill, location);
    }
    
    public String getBellSkill(UUID playerId, Location bellLocation) {
        Map<String, Location> bellMap = playerBellLocations.get(playerId);
        if (bellMap == null) {
            return null;
        }
        
        for (Map.Entry<String, Location> entry : bellMap.entrySet()) {
            if (entry.getValue().distance(bellLocation) < 5) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    public String selectRandomDungeonType() {
        Set<String> dungeonTypes = configManager.getDungeonTypes();
        if (dungeonTypes.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        int totalWeight = 0;
        Map<String, Integer> weights = new HashMap<>();
        
        for (String dungeonType : dungeonTypes) {
            int weight = configManager.getDungeonTypeWeight(dungeonType);
            weights.put(dungeonType, weight);
            totalWeight += weight;
        }
        
        if (totalWeight == 0) {
            // Fallback to equal probability
            List<String> typeList = new ArrayList<>(dungeonTypes);
            return typeList.get(ThreadLocalRandom.current().nextInt(typeList.size()));
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
        return dungeonTypes.iterator().next();
    }
    
    public void setPlayerDungeonType(UUID playerId, String dungeonType) {
        playerDungeonType.put(playerId, dungeonType);
    }
    
    public void addBossDamage(UUID bossId, UUID playerId, double damage) {
        Map<UUID, Double> damageMap = bossDamageTracker.get(bossId);
        if (damageMap != null) {
            damageMap.merge(playerId, damage, Double::sum);
        }
    }
    
    public void cleanup() {
        // Cancel all active tasks
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        // Clear all tracking data
        playerActiveDungeon.clear();
        playerDungeonType.clear();
        playerBellLocations.clear();
        bossDamageTracker.clear();
    }
}