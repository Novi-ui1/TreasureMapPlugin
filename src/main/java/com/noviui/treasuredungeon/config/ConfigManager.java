package com.noviui.treasuredungeon.config;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Set;

public class ConfigManager {
    
    private final TreasureDungeonPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // General settings
    public boolean isDebugEnabled() {
        return config.getBoolean("general.debug", false);
    }
    
    public String getLanguage() {
        return config.getString("general.language", "en");
    }
    
    public String getTreasureWorldName() {
        return config.getString("general.treasure-world.name", "treasure_world");
    }
    
    public int getTreasureSpawnX() {
        return config.getInt("general.treasure-world.spawn.x", 0);
    }
    
    public int getTreasureSpawnY() {
        return config.getInt("general.treasure-world.spawn.y", -60);
    }
    
    public int getTreasureSpawnZ() {
        return config.getInt("general.treasure-world.spawn.z", 0);
    }
    
    public int getMinX() {
        return config.getInt("general.treasure-world.limits.min-x", -1000);
    }
    
    public int getMaxX() {
        return config.getInt("general.treasure-world.limits.max-x", 1000);
    }
    
    public int getMinZ() {
        return config.getInt("general.treasure-world.limits.min-z", -1000);
    }
    
    public int getMaxZ() {
        return config.getInt("general.treasure-world.limits.max-z", 1000);
    }
    
    public int getMinDistance() {
        return config.getInt("general.treasure-world.min-distance", 50);
    }
    
    public String getWaveDelay() {
        return config.getString("general.waves.delay-between-waves", "10s");
    }
    
    public String getBossDelay() {
        return config.getString("general.waves.delay-before-boss", "15s");
    }
    
    public String getChestTimeout() {
        return config.getString("general.chest.timeout", "5m");
    }
    
    public boolean isChestAutoDestroy() {
        return config.getBoolean("general.chest.auto-destroy", true);
    }
    
    public boolean isDamageTrackingEnabled() {
        return config.getBoolean("general.damage-tracking.enabled", true);
    }
    
    public String getDamageShowDuration() {
        return config.getString("general.damage-tracking.show-duration", "30s");
    }
    
    public boolean showPersonalDamage() {
        return config.getBoolean("general.damage-tracking.show-personal-damage", true);
    }
    
    public boolean isGlobalAnnounce() {
        return config.getBoolean("general.announce-globally", true);
    }
    
    public List<String> getBlockedCommands() {
        return config.getStringList("general.blocked-commands");
    }
    
    // Skill settings
    public Set<String> getEnabledSkills() {
        return config.getConfigurationSection("skills").getKeys(false);
    }
    
    public boolean isSkillEnabled(String skill) {
        return config.getBoolean("skills." + skill + ".enabled", false);
    }
    
    public int getSkillLevelRequired(String skill) {
        return config.getInt("skills." + skill + ".level-required", 1000);
    }
    
    public double getSkillDropChance(String skill) {
        return config.getDouble("skills." + skill + ".chance-to-drop", 0.3);
    }
    
    public String getSkillCooldown(String skill) {
        return config.getString("skills." + skill + ".cooldown", "6h");
    }
    
    public String getSkillBellSchematic(String skill) {
        return config.getString("skills." + skill + ".dungeon.schematic-bell");
    }
    
    public String getSkillDungeonSchematic(String skill) {
        return config.getString("skills." + skill + ".dungeon.schematic-dungeon");
    }
    
    public int getSkillWaveCount(String skill) {
        return config.getInt("skills." + skill + ".dungeon.waves.count", 2);
    }
    
    public List<String> getSkillWaveMobs(String skill, int wave) {
        return config.getStringList("skills." + skill + ".dungeon.waves.mobs.wave" + wave);
    }
    
    public String getSkillBoss(String skill) {
        return config.getString("skills." + skill + ".dungeon.boss.id");
    }
    
    public String getSkillBossSpawnDelay(String skill) {
        return config.getString("skills." + skill + ".dungeon.boss.spawn-delay", "8s");
    }
    
    public String getSkillMapMaterial(String skill) {
        return config.getString("skills." + skill + ".map-item.material", "FILLED_MAP");
    }
    
    public String getSkillMapDisplayName(String skill) {
        return config.getString("skills." + skill + ".map-item.display-name");
    }
    
    public List<String> getSkillMapLore(String skill) {
        return config.getStringList("skills." + skill + ".map-item.lore");
    }
    
    public int getSkillMapCustomModelData(String skill) {
        return config.getInt("skills." + skill + ".map-item.custom-model-data", 0);
    }
    
    public boolean isSkillMapGlowing(String skill) {
        return config.getBoolean("skills." + skill + ".map-item.glowing", false);
    }
    
    public String getSkillLootType(String skill) {
        return config.getString("skills." + skill + ".loot.type", "commands");
    }
    
    public List<String> getSkillLootCommands(String skill) {
        return config.getStringList("skills." + skill + ".loot.commands");
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}