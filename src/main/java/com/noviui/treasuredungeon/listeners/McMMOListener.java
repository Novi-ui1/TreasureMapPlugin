package com.noviui.treasuredungeon.listeners;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.DataManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.map.MapManager;
import com.noviui.treasuredungeon.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class McMMOListener implements Listener {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final LanguageManager languageManager;
    private final MapManager mapManager;
    
    public McMMOListener(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.languageManager = plugin.getLanguageManager();
        this.mapManager = plugin.getMapManager();
    }
    
    @EventHandler
    public void onMcMMOLevelUp(McMMOPlayerLevelUpEvent event) {
        Player player = event.getPlayer();
        String skillName = event.getSkill().getName().toLowerCase();
        int newLevel = event.getSkillLevel();
        
        // Check if skill is enabled
        if (!configManager.isSkillEnabled(skillName)) {
            return;
        }
        
        // Check if player reached required level
        int requiredLevel = configManager.getSkillLevelRequired(skillName);
        if (newLevel < requiredLevel) {
            return;
        }
        
        // Check if player already has a map for this skill
        if (dataManager.hasReceivedMap(player.getUniqueId(), skillName)) {
            return;
        }
        
        // Check cooldown
        if (dataManager.isOnCooldown(player.getUniqueId(), skillName)) {
            long remaining = dataManager.getCooldownRemaining(player.getUniqueId(), skillName);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cooldown", languageManager.formatTime(remaining));
            
            String message = languageManager.getMessage("cooldown-active", placeholders);
            player.sendMessage(languageManager.getPrefix() + message);
            return;
        }
        
        // Check drop chance
        double chance = configManager.getSkillDropChance(skillName);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        
        // Give treasure map
        giveTreasureMap(player, skillName);
    }
    
    private void giveTreasureMap(Player player, String skillName) {
        // Create and give map item
        mapManager.giveTreasureMap(player, skillName);
        
        // Mark as received
        dataManager.setMapReceived(player.getUniqueId(), skillName, true);
        
        // Set cooldown
        String cooldownStr = configManager.getSkillCooldown(skillName);
        long cooldownMs = TimeUtils.parseTimeToMillis(cooldownStr);
        dataManager.setCooldown(player.getUniqueId(), skillName, System.currentTimeMillis() + cooldownMs);
        
        // Send messages
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("skill", skillName);
        
        // Private message
        String privateMessage = languageManager.getMessage("map-given-private", placeholders);
        player.sendMessage(languageManager.getPrefix() + privateMessage);
        
        // Global announcement
        if (configManager.isGlobalAnnounce()) {
            String globalMessage = languageManager.getMessage("map-given-global", placeholders);
            Bukkit.broadcastMessage(languageManager.getPrefix() + globalMessage);
        }
    }
}