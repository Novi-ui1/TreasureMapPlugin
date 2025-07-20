package com.noviui.treasuredungeon.editor;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.editor.gui.EditorGUI;
import com.noviui.treasuredungeon.editor.templates.TemplateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main manager for the visual dungeon editor system
 */
public class EditorManager implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final LanguageManager languageManager;
    private final TemplateManager templateManager;
    
    // Active editor sessions
    private final Map<UUID, EditorSession> activeSessions = new ConcurrentHashMap<>();
    
    public EditorManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.templateManager = new TemplateManager(plugin);
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the main editor interface for a player
     */
    public void openEditor(Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Cannot open editor for null or offline player");
            return;
        }
        
        try {
            // Check permission
            if (!player.hasPermission("treasure.editor")) {
                String message = languageManager.getMessage("no-permission");
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
            
            // Close existing session if any
            closeEditor(player);
            
            // Create new editor session
            EditorSession session = new EditorSession(plugin, player);
            activeSessions.put(player.getUniqueId(), session);
            
            // Open main editor GUI
            EditorGUI gui = new EditorGUI(plugin, session);
            gui.openMainMenu();
            
            // Send welcome message
            String welcomeMessage = languageManager.getMessage("editor-welcome");
            player.sendMessage(languageManager.getPrefix() + welcomeMessage);
            
            plugin.getLogger().info("Opened dungeon editor for player: " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening editor for player " + player.getName(), e);
            
            String errorMessage = languageManager.getMessage("editor-error");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
    
    /**
     * Closes the editor for a player
     */
    public void closeEditor(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        EditorSession session = activeSessions.remove(playerId);
        
        if (session != null) {
            try {
                session.cleanup();
                player.closeInventory();
                
                String message = languageManager.getMessage("editor-closed");
                player.sendMessage(languageManager.getPrefix() + message);
                
                plugin.getLogger().info("Closed dungeon editor for player: " + player.getName());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing editor for player " + player.getName(), e);
            }
        }
    }
    
    /**
     * Gets the active editor session for a player
     */
    public EditorSession getSession(Player player) {
        return player != null ? activeSessions.get(player.getUniqueId()) : null;
    }
    
    /**
     * Checks if a player has an active editor session
     */
    public boolean hasActiveSession(Player player) {
        return player != null && activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the template manager
     */
    public TemplateManager getTemplateManager() {
        return templateManager;
    }
    
    /**
     * Gets the number of active editor sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    // Event handlers
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        EditorSession session = getSession(player);
        
        if (session != null) {
            try {
                // Let the session handle the click
                session.handleInventoryClick(event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error handling inventory click in editor", e);
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        EditorSession session = getSession(player);
        
        if (session != null) {
            try {
                // Let the session handle the close
                session.handleInventoryClose(event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error handling inventory close in editor", e);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up editor session when player leaves
        closeEditor(event.getPlayer());
    }
    
    /**
     * Cleanup method for plugin shutdown
     */
    public void cleanup() {
        try {
            // Close all active sessions
            for (EditorSession session : activeSessions.values()) {
                if (session != null) {
                    session.cleanup();
                }
            }
            activeSessions.clear();
            
            // Cleanup template manager
            if (templateManager != null) {
                templateManager.cleanup();
            }
            
            plugin.getLogger().info("Editor manager cleanup completed");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during editor manager cleanup", e);
        }
    }
}