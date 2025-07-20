package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for managing mob spawn points in the editor
 */
public class SpawnTool {
    
    private final EditorSession session;
    private final List<Location> mobSpawns = new ArrayList<>();
    private Location bossSpawn;
    
    public SpawnTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Adds a mob spawn point
     */
    public void addMobSpawn(Location location) {
        if (location != null) {
            mobSpawns.add(location.clone());
            session.setUnsavedChanges(true);
        }
    }
    
    /**
     * Removes a mob spawn point
     */
    public boolean removeMobSpawn(Location location) {
        if (location != null) {
            boolean removed = mobSpawns.removeIf(spawn -> 
                spawn.distance(location) < 2.0);
            if (removed) {
                session.setUnsavedChanges(true);
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Sets the boss spawn location
     */
    public void setBossSpawn(Location location) {
        this.bossSpawn = location != null ? location.clone() : null;
        session.setUnsavedChanges(true);
    }
    
    /**
     * Gets all mob spawn points
     */
    public List<Location> getMobSpawns() {
        return new ArrayList<>(mobSpawns);
    }
    
    /**
     * Gets the boss spawn location
     */
    public Location getBossSpawn() {
        return bossSpawn != null ? bossSpawn.clone() : null;
    }
    
    /**
     * Clears all spawn points
     */
    public void clearAllSpawns() {
        mobSpawns.clear();
        bossSpawn = null;
        session.setUnsavedChanges(true);
    }
    
    /**
     * Gets the total number of spawn points
     */
    public int getTotalSpawnCount() {
        return mobSpawns.size() + (bossSpawn != null ? 1 : 0);
    }
}