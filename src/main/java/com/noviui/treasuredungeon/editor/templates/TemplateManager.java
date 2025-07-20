package com.noviui.treasuredungeon.editor.templates;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages dungeon templates for the editor
 */
public class TemplateManager {
    
    private final TreasureDungeonPlugin plugin;
    private final File templatesFolder;
    private final List<DungeonTemplate> templates = new ArrayList<>();
    
    public TemplateManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        
        // Create templates folder if it doesn't exist
        if (!templatesFolder.exists()) {
            templatesFolder.mkdirs();
        }
        
        loadTemplates();
    }
    
    /**
     * Loads all templates from the templates folder
     */
    public void loadTemplates() {
        templates.clear();
        
        try {
            File[] templateFiles = templatesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            
            if (templateFiles != null) {
                for (File file : templateFiles) {
                    try {
                        DungeonTemplate template = loadTemplate(file);
                        if (template != null) {
                            templates.add(template);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load template: " + file.getName(), e);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + templates.size() + " dungeon templates");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading templates", e);
        }
    }
    
    /**
     * Loads a single template from a file
     */
    private DungeonTemplate loadTemplate(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            String name = config.getString("name", file.getName().replace(".yml", ""));
            String description = config.getString("description", "No description");
            String author = config.getString("author", "Unknown");
            String difficulty = config.getString("difficulty", "medium");
            
            return new DungeonTemplate(name, description, author, difficulty, file);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading template from file: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * Gets all available templates
     */
    public List<DungeonTemplate> getTemplates() {
        return new ArrayList<>(templates);
    }
    
    /**
     * Gets templates by difficulty
     */
    public List<DungeonTemplate> getTemplatesByDifficulty(String difficulty) {
        return templates.stream()
                .filter(template -> template.getDifficulty().equalsIgnoreCase(difficulty))
                .toList();
    }
    
    /**
     * Gets a template by name
     */
    public DungeonTemplate getTemplate(String name) {
        return templates.stream()
                .filter(template -> template.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Saves a new template
     */
    public boolean saveTemplate(DungeonTemplate template) {
        try {
            File file = new File(templatesFolder, template.getName() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("name", template.getName());
            config.set("description", template.getDescription());
            config.set("author", template.getAuthor());
            config.set("difficulty", template.getDifficulty());
            
            config.save(file);
            
            // Add to loaded templates if not already present
            if (!templates.contains(template)) {
                templates.add(template);
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving template: " + template.getName(), e);
            return false;
        }
    }
    
    /**
     * Deletes a template
     */
    public boolean deleteTemplate(String name) {
        try {
            DungeonTemplate template = getTemplate(name);
            if (template != null) {
                File file = template.getFile();
                if (file.exists() && file.delete()) {
                    templates.remove(template);
                    return true;
                }
            }
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting template: " + name, e);
            return false;
        }
    }
    
    /**
     * Creates default templates
     */
    public void createDefaultTemplates() {
        // Create basic templates if none exist
        if (templates.isEmpty()) {
            createBasicTemplate();
            createAdvancedTemplate();
        }
    }
    
    private void createBasicTemplate() {
        DungeonTemplate basic = new DungeonTemplate(
            "Basic Arena",
            "A simple arena-style dungeon perfect for beginners",
            "TreasureDungeon",
            "easy",
            new File(templatesFolder, "basic_arena.yml")
        );
        
        saveTemplate(basic);
    }
    
    private void createAdvancedTemplate() {
        DungeonTemplate advanced = new DungeonTemplate(
            "Complex Fortress",
            "A multi-level fortress with advanced mechanics",
            "TreasureDungeon",
            "hard",
            new File(templatesFolder, "complex_fortress.yml")
        );
        
        saveTemplate(advanced);
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        templates.clear();
        plugin.getLogger().info("Template manager cleanup completed");
    }
}