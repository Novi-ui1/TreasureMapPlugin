package com.noviui.treasuredungeon.editor.gui;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Specialized GUI for editing map properties
 */
public class MapEditorGUI {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    private final EditorSession session;
    
    public MapEditorGUI(TreasureDungeonPlugin plugin, EditorSession session) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
        this.session = session;
    }
    
    /**
     * Opens the map name editor
     */
    public void openMapNameEditor(String skill) {
        Player player = session.getPlayer();
        
        // Create conversation for text input
        ConversationFactory factory = new ConversationFactory(plugin)
            .withModality(true)
            .withPrefix(new NullConversationPrefix())
            .withFirstPrompt(new MapNamePrompt(skill))
            .withEscapeSequence("cancel")
            .withTimeout(60)
            .addConversationAbandonedListener(event -> {
                if (!event.gracefulExit()) {
                    player.sendMessage(languageManager.getPrefix() + "§cMap name editing cancelled.");
                }
            });
        
        Conversation conversation = factory.buildConversation(player);
        conversation.begin();
    }
    
    /**
     * Opens the map lore editor
     */
    public void openMapLoreEditor(String skill) {
        Player player = session.getPlayer();
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withModality(true)
            .withPrefix(new NullConversationPrefix())
            .withFirstPrompt(new MapLorePrompt(skill))
            .withEscapeSequence("cancel")
            .withTimeout(120)
            .addConversationAbandonedListener(event -> {
                if (!event.gracefulExit()) {
                    player.sendMessage(languageManager.getPrefix() + "§cMap lore editing cancelled.");
                }
            });
        
        Conversation conversation = factory.buildConversation(player);
        conversation.begin();
    }
    
    /**
     * Opens material selector for maps
     */
    public void openMaterialSelector(String skill) {
        try {
            Inventory gui = Bukkit.createInventory(null, 54, "§6§lSelect Map Material");
            
            // Common map materials
            Material[] mapMaterials = {
                Material.FILLED_MAP, Material.MAP, Material.PAPER, Material.BOOK,
                Material.WRITABLE_BOOK, Material.WRITTEN_BOOK, Material.ENCHANTED_BOOK,
                Material.COMPASS, Material.CLOCK, Material.RECOVERY_COMPASS,
                Material.SPYGLASS, Material.TELESCOPE, Material.BUNDLE
            };
            
            int slot = 10;
            for (Material material : mapMaterials) {
                if (slot >= 44) break;
                if (slot % 9 == 8 || slot % 9 == 0) {
                    slot++;
                    continue;
                }
                
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e" + formatMaterialName(material.name()));
                    meta.setLore(Arrays.asList(
                        "§7Click to select this material",
                        "§7for " + skill + " treasure maps"
                    ));
                    item.setItemMeta(meta);
                }
                
                gui.setItem(slot, item);
                slot++;
            }
            
            session.setSessionData("material_selector_skill", skill);
            session.setMode("MATERIAL_SELECTOR");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening material selector", e);
        }
    }
    
    /**
     * Conversation prompt for map name editing
     */
    private class MapNamePrompt extends StringPrompt {
        private final String skill;
        
        public MapNamePrompt(String skill) {
            this.skill = skill;
        }
        
        @Override
        public String getPromptText(ConversationContext context) {
            String currentName = configManager.getSkillMapDisplayName(skill);
            return languageManager.getPrefix() + "§eEnter the new display name for " + skill + " maps:\n" +
                   "§7Current: " + (currentName != null ? currentName : "Not set") + "\n" +
                   "§7Type 'cancel' to cancel.";
        }
        
        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input == null || input.trim().isEmpty()) {
                return new MapNamePrompt(skill);
            }
            
            // Save the new name (this would update the config)
            session.setSessionData("map_name_" + skill, input.trim());
            session.setUnsavedChanges(true);
            
            context.getForWhom().sendRawMessage(languageManager.getPrefix() + 
                "§aMap name updated to: " + input.trim());
            
            return Prompt.END_OF_CONVERSATION;
        }
    }
    
    /**
     * Conversation prompt for map lore editing
     */
    private class MapLorePrompt extends StringPrompt {
        private final String skill;
        
        public MapLorePrompt(String skill) {
            this.skill = skill;
        }
        
        @Override
        public String getPromptText(ConversationContext context) {
            List<String> currentLore = configManager.getSkillMapLore(skill);
            return languageManager.getPrefix() + "§eEnter the new lore for " + skill + " maps:\n" +
                   "§7Use | to separate lines\n" +
                   "§7Current: " + (currentLore != null ? String.join(" | ", currentLore) : "Not set") + "\n" +
                   "§7Type 'cancel' to cancel.";
        }
        
        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input == null || input.trim().isEmpty()) {
                return new MapLorePrompt(skill);
            }
            
            // Parse lore lines
            String[] loreLines = input.split("\\|");
            for (int i = 0; i < loreLines.length; i++) {
                loreLines[i] = loreLines[i].trim();
            }
            
            // Save the new lore
            session.setSessionData("map_lore_" + skill, Arrays.asList(loreLines));
            session.setUnsavedChanges(true);
            
            context.getForWhom().sendRawMessage(languageManager.getPrefix() + 
                "§aMap lore updated with " + loreLines.length + " lines.");
            
            return Prompt.END_OF_CONVERSATION;
        }
    }
    
    /**
     * Formats material names for display
     */
    private String formatMaterialName(String materialName) {
        return Arrays.stream(materialName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(materialName);
    }
}