package com.noviui.treasuredungeon.editor.tools;

import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for real-time preview of dungeon structures
 */
public class PreviewTool {
    
    private final EditorSession session;
    private final Map<Location, Material> previewBlocks = new HashMap<>();
    private BukkitTask previewTask;
    private boolean previewActive = false;
    
    public PreviewTool(EditorSession session) {
        this.session = session;
    }
    
    /**
     * Starts preview mode for the selected area
     */
    public boolean startPreview(Location pos1, Location pos2, Material previewMaterial) {
        if (pos1 == null || pos2 == null || previewMaterial == null) {
            return false;
        }
        
        stopPreview();
        
        try {
            previewBlocks.clear();
            
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            
            // Store original blocks and set preview
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location loc = new Location(pos1.getWorld(), x, y, z);
                        Material original = loc.getBlock().getType();
                        previewBlocks.put(loc.clone(), original);
                        
                        // Only preview non-air blocks or outline
                        if (isOutlineBlock(x, y, z, minX, maxX, minY, maxY, minZ, maxZ)) {
                            loc.getBlock().setType(previewMaterial);
                        }
                    }
                }
            }
            
            previewActive = true;
            startPreviewEffects();
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Stops preview mode and restores original blocks
     */
    public void stopPreview() {
        if (!previewActive) return;
        
        try {
            // Stop effects
            if (previewTask != null && !previewTask.isCancelled()) {
                previewTask.cancel();
            }
            
            // Restore original blocks
            for (Map.Entry<Location, Material> entry : previewBlocks.entrySet()) {
                Location loc = entry.getKey();
                Material original = entry.getValue();
                if (loc.getWorld() != null) {
                    loc.getBlock().setType(original);
                }
            }
            
            previewBlocks.clear();
            previewActive = false;
            
        } catch (Exception e) {
            // Log error but continue cleanup
        }
    }
    
    /**
     * Starts visual effects for preview
     */
    private void startPreviewEffects() {
        Player player = session.getPlayer();
        if (player == null || !player.isOnline()) return;
        
        previewTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!previewActive || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                try {
                    // Add particle effects around preview blocks
                    for (Location loc : previewBlocks.keySet()) {
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.REDSTONE, 
                                loc.clone().add(0.5, 0.5, 0.5), 
                                1, 0, 0, 0, 0);
                        }
                    }
                } catch (Exception e) {
                    this.cancel();
                }
            }
        }.runTaskTimer(session.getPlugin(), 0L, 10L); // Every 0.5 seconds
    }
    
    /**
     * Checks if a block is part of the outline
     */
    private boolean isOutlineBlock(int x, int y, int z, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return x == minX || x == maxX || 
               y == minY || y == maxY || 
               z == minZ || z == maxZ;
    }
    
    /**
     * Checks if preview is active
     */
    public boolean isPreviewActive() {
        return previewActive;
    }
    
    /**
     * Gets preview block count
     */
    public int getPreviewBlockCount() {
        return previewBlocks.size();
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        stopPreview();
    }
}