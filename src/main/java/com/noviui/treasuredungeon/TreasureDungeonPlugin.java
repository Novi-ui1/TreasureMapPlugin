package com.noviui.treasuredungeon;

import com.noviui.treasuredungeon.commands.TreasureCommand;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.dungeon.DungeonManager;
import com.noviui.treasuredungeon.integration.IntegrationManager;
import com.noviui.treasuredungeon.listeners.CommandBlockListener;
import com.noviui.treasuredungeon.listeners.McMMOListener;
import com.noviui.treasuredungeon.listeners.PlayerInteractListener;
import com.noviui.treasuredungeon.listeners.ProximityListener;
import com.noviui.treasuredungeon.map.MapManager;
import com.noviui.treasuredungeon.utils.LocationManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;

public final class TreasureDungeonPlugin extends JavaPlugin {
    
    private static TreasureDungeonPlugin instance;
    
    private ConfigManager configManager;
    private DataManager dataManager;
    private LanguageManager languageManager;
    private IntegrationManager integrationManager;
    private MapManager mapManager;
    private DungeonManager dungeonManager;
    private LocationManager locationManager;
    
    private BukkitTask autoSaveTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Initialize managers with error handling
            initializeManagers();
            
            // Load configurations asynchronously
            loadConfigurationsAsync().thenRun(() -> {
                // Register components after configs are loaded
                registerComponents();
                
                // Start auto-save task
                startAutoSaveTask();
                
                getLogger().info("TreasureDungeon plugin enabled successfully!");
            }).exceptionally(throwable -> {
                getLogger().log(Level.SEVERE, "Failed to initialize plugin", throwable);
                getServer().getPluginManager().disablePlugin(this);
                return null;
            });
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Critical error during plugin initialization", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel auto-save task
            if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
                autoSaveTask.cancel();
            }
            
            // Save data synchronously on shutdown
            if (dataManager != null) {
                dataManager.saveData();
            }
            
            // Cleanup active dungeons
            if (dungeonManager != null) {
                dungeonManager.cleanup();
            }
            
            // Clear caches
            if (locationManager != null) {
                locationManager.clearCache();
            }
            
            getLogger().info("TreasureDungeon plugin disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }
    
    private void initializeManagers() {
        try {
            this.configManager = new ConfigManager(this);
            this.dataManager = new DataManager(this);
            this.languageManager = new LanguageManager(this);
            this.integrationManager = new IntegrationManager(this);
            this.mapManager = new MapManager(this);
            this.dungeonManager = new DungeonManager(this);
            this.locationManager = new LocationManager(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize managers", e);
        }
    }
    
    private CompletableFuture<Void> loadConfigurationsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                configManager.loadConfig();
                dataManager.loadData();
                languageManager.loadLanguage();
                integrationManager.checkIntegrations();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load configurations", e);
            }
        });
    }
    
    private void registerComponents() {
        try {
            // Register listeners
            registerListeners();
            
            // Register commands
            registerCommands();
            
            // Create treasure world if needed
            createTreasureWorldAsync();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to register components", e);
        }
    }
    
    private void registerListeners() {
        try {
            Bukkit.getPluginManager().registerEvents(new McMMOListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
            Bukkit.getPluginManager().registerEvents(new ProximityListener(this), this);
            Bukkit.getPluginManager().registerEvents(new CommandBlockListener(this), this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register listeners", e);
        }
    }
    
    private void registerCommands() {
        try {
            getCommand("treasure").setExecutor(new TreasureCommand(this));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register commands", e);
        }
    }
    
    private void createTreasureWorldAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                String worldName = configManager.getTreasureWorldName();
                if (worldName == null || worldName.trim().isEmpty()) {
                    getLogger().warning("Treasure world name is not configured properly");
                    return;
                }
                
                if (Bukkit.getWorld(worldName) == null) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        try {
                            Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
                            getLogger().info("Created treasure world: " + worldName);
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Failed to create treasure world: " + worldName, e);
                        }
                    });
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error in treasure world creation task", e);
            }
        });
    }
    
    private void startAutoSaveTask() {
        // Auto-save every 5 minutes
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (dataManager != null) {
                    dataManager.saveDataAsync();
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error during auto-save", e);
            }
        }, 6000L, 6000L); // 5 minutes = 6000 ticks
    }
    
    public CompletableFuture<Void> reloadAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                configManager.loadConfig();
                dataManager.loadData();
                languageManager.loadLanguage();
                integrationManager.checkIntegrations();
                locationManager.clearCache();
            } catch (Exception e) {
                throw new RuntimeException("Failed to reload plugin", e);
            }
        });
    }
    
    public void reload() {
        try {
            reloadAsync().join();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin reload", e);
        }
    }
    
    public boolean isFullyInitialized() {
        return configManager != null && dataManager != null && 
               languageManager != null && integrationManager != null &&
               mapManager != null && dungeonManager != null && locationManager != null;
    }
    
    // Getters
    public static TreasureDungeonPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    public MapManager getMapManager() {
        return mapManager;
    }
    
    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }
    
    public LocationManager getLocationManager() {
        return locationManager;
    }
}