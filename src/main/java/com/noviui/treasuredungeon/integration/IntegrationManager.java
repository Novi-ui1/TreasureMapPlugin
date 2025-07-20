package com.noviui.treasuredungeon.integration;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.Bukkit;

public class IntegrationManager {
    
    private final TreasureDungeonPlugin plugin;
    private boolean mcMMOEnabled = false;
    private boolean worldEditEnabled = false;
    private boolean mythicMobsEnabled = false;
    private boolean multiverseEnabled = false;
    
    public IntegrationManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void checkIntegrations() {
        // Check mcMMO (required)
        mcMMOEnabled = Bukkit.getPluginManager().isPluginEnabled("mcMMO");
        if (!mcMMOEnabled) {
            plugin.getLogger().severe("mcMMO is required but not found! Plugin will not work properly.");
        } else {
            plugin.getLogger().info("mcMMO integration enabled!");
        }
        
        // Check WorldEdit (optional but recommended)
        worldEditEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
        if (worldEditEnabled) {
            plugin.getLogger().info("WorldEdit integration enabled!");
        } else {
            plugin.getLogger().warning("WorldEdit not found. Schematic loading will be disabled.");
        }
        
        // Check MythicMobs (optional but recommended)
        mythicMobsEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        if (mythicMobsEnabled) {
            plugin.getLogger().info("MythicMobs integration enabled!");
        } else {
            plugin.getLogger().warning("MythicMobs not found. Custom mob spawning will be disabled.");
        }
        
        // Check Multiverse (optional)
        multiverseEnabled = Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");
        if (multiverseEnabled) {
            plugin.getLogger().info("Multiverse-Core integration enabled!");
        }
    }
    
    public boolean isMcMMOEnabled() {
        return mcMMOEnabled;
    }
    
    public boolean isWorldEditEnabled() {
        return worldEditEnabled;
    }
    
    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }
    
    public boolean isMultiverseEnabled() {
        return multiverseEnabled;
    }
}