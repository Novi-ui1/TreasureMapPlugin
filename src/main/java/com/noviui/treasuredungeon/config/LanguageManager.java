package com.noviui.treasuredungeon.config;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    
    private final TreasureDungeonPlugin plugin;
    private FileConfiguration langConfig;
    private final Map<String, String> messageCache = new HashMap<>();
    
    public LanguageManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadLanguage() {
        String language = plugin.getConfigManager().getLanguage();
        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        
        // Create lang folder if it doesn't exist
        if (!langFile.getParentFile().exists()) {
            langFile.getParentFile().mkdirs();
        }
        
        // Save default language files
        if (!new File(plugin.getDataFolder(), "lang/en.yml").exists()) {
            plugin.saveResource("lang/en.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "lang/pt.yml").exists()) {
            plugin.saveResource("lang/pt.yml", false);
        }
        
        // Load the specified language file
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        } else {
            // Fallback to English
            File enFile = new File(plugin.getDataFolder(), "lang/en.yml");
            langConfig = YamlConfiguration.loadConfiguration(enFile);
        }
        
        loadMessagesToCache();
    }
    
    private void loadMessagesToCache() {
        messageCache.clear();
        loadSectionToCache("messages", "");
    }
    
    private void loadSectionToCache(String section, String prefix) {
        if (langConfig.getConfigurationSection(section) == null) {
            return;
        }
        
        for (String key : langConfig.getConfigurationSection(section).getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            String path = section + "." + key;
            
            if (langConfig.isConfigurationSection(path)) {
                loadSectionToCache(path, fullKey);
            } else {
                String value = langConfig.getString(path, "");
                messageCache.put(fullKey, colorize(value));
            }
        }
    }
    
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "Missing message: " + key);
    }
    
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}