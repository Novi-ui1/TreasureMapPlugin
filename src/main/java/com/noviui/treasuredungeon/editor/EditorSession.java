package com.noviui.treasuredungeon.editor;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.editor.tools.BuildTool;
import com.noviui.treasuredungeon.editor.tools.SelectionTool;
import com.noviui.treasuredungeon.editor.tools.SpawnTool;
import com.noviui.treasuredungeon.editor.tools.CopyPasteTool;
import com.noviui.treasuredungeon.editor.tools.UndoRedoTool;
import com.noviui.treasuredungeon.editor.tools.PreviewTool;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents an individual player's editor session
 */
public class EditorSession {
    
    private final TreasureDungeonPlugin plugin;
    private final LanguageManager languageManager;
    private final Player player;
    private final UUID sessionId;
    
    // Session state
    private String currentDungeonType;
    private String currentMode = "MAIN_MENU"; // MAIN_MENU, BUILDING, CONFIGURING, etc.
    private Material selectedMaterial = Material.STONE;
    private Location editLocation;
    
    // Tools
    private final BuildTool buildTool;
    private final SelectionTool selectionTool;
    private final SpawnTool spawnTool;
    private final CopyPasteTool copyPasteTool;
    private final UndoRedoTool undoRedoTool;
    private final PreviewTool previewTool;
    
    // Session data
    private final Map<String, Object> sessionData = new HashMap<>();
    private boolean hasUnsavedChanges = false;
    
    public EditorSession(TreasureDungeonPlugin plugin, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.player = player;
        this.sessionId = UUID.randomUUID();
        
        // Initialize tools
        this.buildTool = new BuildTool(this);
        this.selectionTool = new SelectionTool(this);
        this.spawnTool = new SpawnTool(this);
        this.copyPasteTool = new CopyPasteTool(this);
        this.undoRedoTool = new UndoRedoTool(this);
        this.previewTool = new PreviewTool(this);
        
        plugin.getLogger().info("Created editor session " + sessionId + " for player " + player.getName());
    }
    
    /**
     * Handles inventory click events for this session
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        try {
            String inventoryTitle = event.getView().getTitle();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }
            
            // Cancel the event by default for editor GUIs
            event.setCancelled(true);
            
            // Route to appropriate handler based on current mode
            switch (currentMode) {
                case "MAIN_MENU":
                    handleMainMenuClick(event);
                    break;
                case "DUNGEON_SELECTOR":
                    handleDungeonSelectorClick(event);
                    break;
                case "BLOCK_PALETTE":
                    handleBlockPaletteClick(event);
                    break;
                case "PROPERTIES":
                    handlePropertiesClick(event);
                    break;
                case "TEMPLATES":
                    handleTemplatesClick(event);
                    break;
                case "MAP_EDITOR":
                    handleMapEditorClick(event);
                    break;
                case "ADVANCED_TOOLS":
                    handleAdvancedToolsClick(event);
                    break;
                case "EXPORT_IMPORT":
                    handleExportImportClick(event);
                    break;
                default:
                    plugin.getLogger().warning("Unknown editor mode: " + currentMode);
                    break;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling inventory click in editor session", e);
        }
    }
    
    /**
     * Handles inventory close events for this session
     */
    public void handleInventoryClose(InventoryCloseEvent event) {
        try {
            // Check if we should auto-save or warn about unsaved changes
            if (hasUnsavedChanges) {
                String message = languageManager.getMessage("editor-unsaved-changes");
                player.sendMessage(languageManager.getPrefix() + message);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling inventory close in editor session", e);
        }
    }
    
    private void handleMainMenuClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        if (displayName.contains("Create New Dungeon")) {
            setMode("DUNGEON_SELECTOR");
            // Open dungeon type selector
        } else if (displayName.contains("Load Template")) {
            setMode("TEMPLATES");
            // Open template selector
        } else if (displayName.contains("Block Palette")) {
            setMode("BLOCK_PALETTE");
            // Open block palette
        } else if (displayName.contains("Properties")) {
            setMode("PROPERTIES");
            // Open properties panel
        } else if (displayName.contains("Map Editor")) {
            setMode("MAP_EDITOR");
            // Open map editor
        } else if (displayName.contains("Advanced Tools")) {
            setMode("ADVANCED_TOOLS");
            // Open advanced tools
        } else if (displayName.contains("Export/Import")) {
            setMode("EXPORT_IMPORT");
            // Open export/import
        } else if (displayName.contains("Preview")) {
            // Toggle preview mode
            togglePreview();
        }
    }
    
    private void handleDungeonSelectorClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        // Extract dungeon type from item
        String displayName = item.getItemMeta().getDisplayName();
        // Implementation for dungeon type selection
    }
    
    private void handleBlockPaletteClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() != Material.AIR) {
            selectedMaterial = item.getType();
            
            String message = languageManager.getMessage("editor-material-selected");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("material", selectedMaterial.name());
            
            String formattedMessage = languageManager.getMessage("editor-material-selected", placeholders);
            player.sendMessage(languageManager.getPrefix() + formattedMessage);
        }
    }
    
    private void handlePropertiesClick(InventoryClickEvent event) {
        // Handle properties panel interactions
    }
    
    private void handleTemplatesClick(InventoryClickEvent event) {
        // Handle template selection
    }
    
    private void handleMapEditorClick(InventoryClickEvent event) {
        // Handle map editor interactions
    }
    
    private void handleAdvancedToolsClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        if (displayName.contains("Undo")) {
            undoRedoTool.undo();
        } else if (displayName.contains("Redo")) {
            undoRedoTool.redo();
        } else if (displayName.contains("Copy")) {
            // Handle copy operation
        } else if (displayName.contains("Paste")) {
            // Handle paste operation
        }
    }
    
    private void handleExportImportClick(InventoryClickEvent event) {
        // Handle export/import operations
    }
    
    // Getters and setters
    
    public Player getPlayer() {
        return player;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public String getCurrentDungeonType() {
        return currentDungeonType;
    }
    
    public void setCurrentDungeonType(String dungeonType) {
        this.currentDungeonType = dungeonType;
        this.hasUnsavedChanges = true;
    }
    
    public String getCurrentMode() {
        return currentMode;
    }
    
    public void setMode(String mode) {
        this.currentMode = mode;
    }
    
    public Material getSelectedMaterial() {
        return selectedMaterial;
    }
    
    public void setSelectedMaterial(Material material) {
        this.selectedMaterial = material;
    }
    
    public Location getEditLocation() {
        return editLocation;
    }
    
    public void setEditLocation(Location location) {
        this.editLocation = location;
    }
    
    public BuildTool getBuildTool() {
        return buildTool;
    }
    
    public SelectionTool getSelectionTool() {
        return selectionTool;
    }
    
    public SpawnTool getSpawnTool() {
        return spawnTool;
    }
    
    public CopyPasteTool getCopyPasteTool() {
        return copyPasteTool;
    }
    
    public UndoRedoTool getUndoRedoTool() {
        return undoRedoTool;
    }
    
    public PreviewTool getPreviewTool() {
        return previewTool;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    public void setUnsavedChanges(boolean hasChanges) {
        this.hasUnsavedChanges = hasChanges;
    }
    
    public TreasureDungeonPlugin getPlugin() {
        return plugin;
    }
    
    // Session data management
    
    public void setSessionData(String key, Object value) {
        sessionData.put(key, value);
    }
    
    public Object getSessionData(String key) {
        return sessionData.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getSessionData(String key, Class<T> type) {
        Object value = sessionData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Toggles preview mode
     */
    private void togglePreview() {
        if (previewTool.isPreviewActive()) {
            previewTool.stopPreview();
        } else {
            // Start preview with current selection
        }
    }
    
    /**
     * Saves the current dungeon configuration
     */
    public void saveDungeon() {
        try {
            // Implementation for saving dungeon
            hasUnsavedChanges = false;
            
            String message = languageManager.getMessage("editor-dungeon-saved");
            player.sendMessage(languageManager.getPrefix() + message);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving dungeon in editor session", e);
            
            String errorMessage = languageManager.getMessage("editor-save-failed");
            player.sendMessage(languageManager.getPrefix() + errorMessage);
        }
    }
    
    /**
     * Cleanup method for session termination
     */
    public void cleanup() {
        try {
            // Save any unsaved changes if needed
            
            // Cleanup tools
            if (previewTool != null) {
                previewTool.cleanup();
            }
            
            if (hasUnsavedChanges) {
                // Could implement auto-save here
            }
            
            // Clear session data
            sessionData.clear();
            
            plugin.getLogger().info("Cleaned up editor session " + sessionId + " for player " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during editor session cleanup", e);
        }
    }
}