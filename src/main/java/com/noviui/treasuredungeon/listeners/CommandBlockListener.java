package com.noviui.treasuredungeon.listeners;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlockListener implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    
    public CommandBlockListener(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Check if player is in treasure world
        String treasureWorldName = configManager.getTreasureWorldName();
        if (!player.getWorld().getName().equals(treasureWorldName)) {
            return;
        }
        
        // Check blocked commands
        List<String> blockedCommands = configManager.getBlockedCommands();
        for (String blockedCommand : blockedCommands) {
            if (command.startsWith("/" + blockedCommand.toLowerCase())) {
                event.setCancelled(true);
                
                String message = "§cThat command is blocked in the treasure world!";
                player.sendMessage(languageManager.getPrefix() + message);
                return;
            }
        }
    }
}