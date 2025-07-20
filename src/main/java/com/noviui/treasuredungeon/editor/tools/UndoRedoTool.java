package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Tool for undo/redo functionality in the editor
 */
public class UndoRedoTool {
    
    private final EditorSession session;
    private final Stack<EditorAction> undoStack = new Stack<>();
    private final Stack<EditorAction> redoStack = new Stack<>();
    private static final int MAX_HISTORY = 50;
    
    public UndoRedoTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Records an action for undo/redo
     */
    public void recordAction(Location location, Material oldMaterial, Material newMaterial) {
        if (location == null) return;
        
        EditorAction action = new EditorAction(location.clone(), oldMaterial, newMaterial);
        undoStack.push(action);
        
        // Clear redo stack when new action is performed
        redoStack.clear();
        
        // Limit history size
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
        
        session.setUnsavedChanges(true);
    }
    
    /**
     * Records a bulk action for undo/redo
     */
    public void recordBulkAction(List<Location> locations, List<Material> oldMaterials, List<Material> newMaterials) {
        if (locations == null || oldMaterials == null || newMaterials == null) return;
        if (locations.size() != oldMaterials.size() || locations.size() != newMaterials.size()) return;
        
        BulkEditorAction bulkAction = new BulkEditorAction();
        for (int i = 0; i < locations.size(); i++) {
            bulkAction.addAction(locations.get(i).clone(), oldMaterials.get(i), newMaterials.get(i));
        }
        
        undoStack.push(bulkAction);
        redoStack.clear();
        
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
        
        session.setUnsavedChanges(true);
    }
    
    /**
     * Undoes the last action
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        try {
            EditorAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
            
            session.setUnsavedChanges(true);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Redoes the last undone action
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        try {
            EditorAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);
            
            session.setUnsavedChanges(true);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if undo is available
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Checks if redo is available
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Gets undo stack size
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * Gets redo stack size
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    /**
     * Clears all history
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
    
    /**
     * Single editor action
     */
    private static class EditorAction {
        protected final Location location;
        protected final Material oldMaterial;
        protected final Material newMaterial;
        
        public EditorAction(Location location, Material oldMaterial, Material newMaterial) {
            this.location = location;
            this.oldMaterial = oldMaterial;
            this.newMaterial = newMaterial;
        }
        
        public void undo() {
            if (location != null && location.getWorld() != null) {
                location.getBlock().setType(oldMaterial);
            }
        }
        
        public void redo() {
            if (location != null && location.getWorld() != null) {
                location.getBlock().setType(newMaterial);
            }
        }
    }
    
    /**
     * Bulk editor action for multiple blocks
     */
    private static class BulkEditorAction extends EditorAction {
        private final List<EditorAction> actions = new ArrayList<>();
        
        public BulkEditorAction() {
            super(null, null, null);
        }
        
        public void addAction(Location location, Material oldMaterial, Material newMaterial) {
            actions.add(new EditorAction(location, oldMaterial, newMaterial));
        }
        
        @Override
        public void undo() {
            for (int i = actions.size() - 1; i >= 0; i--) {
                actions.get(i).undo();
            }
        }
        
        @Override
        public void redo() {
            for (EditorAction action : actions) {
                action.redo();
            }
        }
    }
}