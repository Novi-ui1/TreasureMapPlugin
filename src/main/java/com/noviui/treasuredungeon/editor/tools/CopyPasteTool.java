package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for copying and pasting structures in the editor
 */
public class CopyPasteTool {
    
    private final EditorSession session;
    private Map<String, Material> clipboard = new HashMap<>();
    private Location clipboardOrigin;
    private int clipboardSizeX, clipboardSizeY, clipboardSizeZ;
    
    public CopyPasteTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Copies the selected area to clipboard
     */
    public boolean copySelection(Location pos1, Location pos2) {
        if (pos1 == null || pos2 == null || !pos1.getWorld().equals(pos2.getWorld())) {
            return false;
        }
        
        try {
            clipboard.clear();
            clipboardOrigin = pos1.clone();
            
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            
            clipboardSizeX = maxX - minX + 1;
            clipboardSizeY = maxY - minY + 1;
            clipboardSizeZ = maxZ - minZ + 1;
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location loc = new Location(pos1.getWorld(), x, y, z);
                        Block block = loc.getBlock();
                        String key = (x - minX) + "," + (y - minY) + "," + (z - minZ);
                        clipboard.put(key, block.getType());
                    }
                }
            }
            
            session.setUnsavedChanges(true);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Pastes the clipboard contents at the specified location
     */
    public boolean paste(Location location) {
        if (location == null || clipboard.isEmpty()) {
            return false;
        }
        
        try {
            World world = location.getWorld();
            if (world == null) return false;
            
            int baseX = location.getBlockX();
            int baseY = location.getBlockY();
            int baseZ = location.getBlockZ();
            
            for (Map.Entry<String, Material> entry : clipboard.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int offsetX = Integer.parseInt(coords[0]);
                int offsetY = Integer.parseInt(coords[1]);
                int offsetZ = Integer.parseInt(coords[2]);
                
                Location pasteLocation = new Location(world, 
                    baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
                pasteLocation.getBlock().setType(entry.getValue());
            }
            
            session.setUnsavedChanges(true);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if clipboard has content
     */
    public boolean hasClipboard() {
        return !clipboard.isEmpty();
    }
    
    /**
     * Gets clipboard size info
     */
    public String getClipboardInfo() {
        if (clipboard.isEmpty()) {
            return "Empty";
        }
        return clipboardSizeX + "x" + clipboardSizeY + "x" + clipboardSizeZ + " (" + clipboard.size() + " blocks)";
    }
    
    /**
     * Clears the clipboard
     */
    public void clearClipboard() {
        clipboard.clear();
        clipboardOrigin = null;
        clipboardSizeX = clipboardSizeY = clipboardSizeZ = 0;
    }
}