package com.noviui.treasuredungeon.map;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MapManager {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final NamespacedKey treasureMapKey;
    
    public MapManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.treasureMapKey = new NamespacedKey(plugin, "treasure_map_skill");
    }
    
    public void giveTreasureMap(Player player, String skill) {
        ItemStack mapItem = createTreasureMap(skill);
        player.getInventory().addItem(mapItem);
    }
    
    public ItemStack createTreasureMap(String skill) {
        // Get material
        String materialStr = configManager.getSkillMapMaterial(skill);
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.FILLED_MAP;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        String displayName = configManager.getSkillMapDisplayName(skill);
        if (displayName != null) {
            meta.setDisplayName(colorize(displayName));
        }
        
        // Set lore
        List<String> lore = configManager.getSkillMapLore(skill);
        if (lore != null && !lore.isEmpty()) {
            List<String> colorizedLore = new ArrayList<>();
            for (String line : lore) {
                colorizedLore.add(colorize(line));
            }
            meta.setLore(colorizedLore);
        }
        
        // Set custom model data
        int customModelData = configManager.getSkillMapCustomModelData(skill);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        // Set glowing effect
        if (configManager.isSkillMapGlowing(skill)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Add treasure map identifier
        meta.getPersistentDataContainer().set(treasureMapKey, PersistentDataType.STRING, skill);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public String getTreasureMapSkill(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(treasureMapKey, PersistentDataType.STRING);
    }
    
    public boolean isTreasureMap(ItemStack item) {
        return getTreasureMapSkill(item) != null;
    }
    
    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}