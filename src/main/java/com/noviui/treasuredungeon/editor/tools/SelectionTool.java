package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;

/**
 * Tool for selecting areas in the editor
 */
public class SelectionTool {
    
    private final EditorSession session;
    private Location pos1;
    private Location pos2;
    
    public SelectionTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Sets the first position of the selection
     */
    public void setPos1(Location location) {
        this.pos1 = location;
        session.setSessionData("selection_pos1", location);
    }
    
    /**
     * Sets the second position of the selection
     */
    public void setPos2(Location location) {
        this.pos2 = location;
        session.setSessionData("selection_pos2", location);
    }
    
    /**
     * Gets the first position
     */
    public Location getPos1() {
        return pos1;
    }
    
    /**
     * Gets the second position
     */
    public Location getPos2() {
        return pos2;
    }
    
    /**
     * Checks if a complete selection exists
     */
    public boolean hasSelection() {
        return pos1 != null && pos2 != null;
    }
    
    /**
     * Calculates the volume of the selection
     */
    public int getSelectionVolume() {
        if (!hasSelection()) {
            return 0;
        }
        
        int dx = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int dy = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int dz = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        
        return dx * dy * dz;
    }
    
    /**
     * Clears the current selection
     */
    public void clearSelection() {
        this.pos1 = null;
        this.pos2 = null;
        session.setSessionData("selection_pos1", null);
        session.setSessionData("selection_pos2", null);
    }
}