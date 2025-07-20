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
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TreasureDungeonPlugin extends JavaPlugin {
    
    private static TreasureDungeonPlugin instance;
    
    private ConfigManager configManager;
    private DataManager dataManager;
    private LanguageManager languageManager;
    private IntegrationManager integrationManager;
    private MapManager mapManager;
    private DungeonManager dungeonManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.languageManager = new LanguageManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.mapManager = new MapManager(this);
        this.dungeonManager = new DungeonManager(this);
        
        // Load configurations
        configManager.loadConfig();
        dataManager.loadData();
        languageManager.loadLanguage();
        
        // Check integrations
        integrationManager.checkIntegrations();
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Create treasure world if needed
        createTreasureWorld();
        
        getLogger().info("TreasureDungeon plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save data
        if (dataManager != null) {
            dataManager.saveData();
        }
        
        // Cleanup active dungeons
        if (dungeonManager != null) {
            dungeonManager.cleanup();
        }
        
        getLogger().info("TreasureDungeon plugin disabled!");
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new McMMOListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProximityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockListener(this), this);
    }
    
    private void registerCommands() {
        getCommand("treasure").setExecutor(new TreasureCommand(this));
    }
    
    private void createTreasureWorld() {
        String worldName = configManager.getTreasureWorldName();
        if (Bukkit.getWorld(worldName) == null) {
            try {
                Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
                getLogger().info("Created treasure world: " + worldName);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to create treasure world: " + worldName, e);
            }
        }
    }
    
    public void reload() {
        configManager.loadConfig();
        dataManager.loadData();
        languageManager.loadLanguage();
        integrationManager.checkIntegrations();
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
}