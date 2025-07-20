package com.noviui.treasuredungeon.commands;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.editor.EditorManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TreasureCommand implements CommandExecutor {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    private final EditorManager editorManager;
    
    public TreasureCommand(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
        this.editorManager = plugin.getEditorManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "tp":
            case "teleport":
                return handleTeleport(sender);
                
            case "reload":
                return handleReload(sender);
                
            case "editor":
                return handleEditor(sender);
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleTeleport(CommandSender sender) {
        if (!sender.hasPermission("treasure.tp")) {
            String message = languageManager.getMessage("no-permission");
            sender.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        if (!(sender instanceof Player)) {
            String message = languageManager.getMessage("player-only");
            sender.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        Player player = (Player) sender;
        
        // Get treasure world
        String worldName = configManager.getTreasureWorldName();
        World treasureWorld = plugin.getServer().getWorld(worldName);
        
        if (treasureWorld == null) {
            String message = languageManager.getMessage("world-not-found");
            player.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        // Teleport to spawn location
        Location spawnLocation = new Location(treasureWorld,
            configManager.getTreasureSpawnX(),
            configManager.getTreasureSpawnY(),
            configManager.getTreasureSpawnZ());
        
        String teleportingMessage = languageManager.getMessage("teleporting");
        player.sendMessage(languageManager.getPrefix() + teleportingMessage);
        
        player.teleport(spawnLocation);
        
        String successMessage = languageManager.getMessage("teleport-success");
        player.sendMessage(languageManager.getPrefix() + successMessage);
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("treasure.reload")) {
            String message = languageManager.getMessage("no-permission");
            sender.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        plugin.reload();
        
        String message = languageManager.getMessage("reload-success");
        sender.sendMessage(languageManager.getPrefix() + message);
        
        return true;
    }
    
    private boolean handleEditor(CommandSender sender) {
        if (!sender.hasPermission("treasure.editor")) {
            String message = languageManager.getMessage("no-permission");
            sender.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        if (!(sender instanceof Player)) {
            String message = languageManager.getMessage("player-only");
            sender.sendMessage(languageManager.getPrefix() + message);
            return true;
        }
        
        Player player = (Player) sender;
        editorManager.openEditor(player);
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = languageManager.getConfig().getStringList("messages.command-help");
        
        if (helpMessages.isEmpty()) {
            sender.sendMessage(languageManager.getPrefix() + "§6Treasure Dungeon Commands:");
            sender.sendMessage("§e/treasure tp §7- Teleport to treasure spawn");
            sender.sendMessage("§e/treasure reload §7- Reload configuration");
            sender.sendMessage("§e/treasure editor §7- Open dungeon editor");
        } else {
            for (String line : helpMessages) {
                sender.sendMessage(languageManager.getPrefix() + line);
            }
        }
    }
}