package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Tool for building and placing blocks in the editor
 */
public class BuildTool {
    
    private final EditorSession session;
    
    public BuildTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Places a block at the specified location
     */
    public boolean placeBlock(Location location, Material material) {
        if (location == null || material == null) {
            return false;
        }
        
        try {
            Block block = location.getBlock();
            block.setType(material);
            
            session.setUnsavedChanges(true);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Removes a block at the specified location
     */
    public boolean removeBlock(Location location) {
        return placeBlock(location, Material.AIR);
    }
    
    /**
     * Fills an area with the specified material
     */
    public int fillArea(Location pos1, Location pos2, Material material) {
        if (pos1 == null || pos2 == null || material == null) {
            return 0;
        }
        
        int blocksChanged = 0;
        
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(pos1.getWorld(), x, y, z);
                    if (placeBlock(loc, material)) {
                        blocksChanged++;
                    }
                }
            }
        }
        
        return blocksChanged;
    }
}