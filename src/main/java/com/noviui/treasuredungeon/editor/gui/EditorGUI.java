package com.noviui.treasuredungeon.editor.gui;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.LanguageManager;
import com.noviui.treasuredungeon.editor.EditorSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Professional GUI system for the dungeon editor
 */
public class EditorGUI {
    
    private final TreasureDungeonPlugin plugin;
    private final LanguageManager languageManager;
    private final EditorSession session;
    
    // GUI Constants
    private static final int MAIN_MENU_SIZE = 54; // 6 rows
    private static final int PALETTE_SIZE = 54;
    private static final int PROPERTIES_SIZE = 45; // 5 rows
    
    public EditorGUI(TreasureDungeonPlugin plugin, EditorSession session) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.session = session;
    }
    
    /**
     * Opens the main editor menu
     */
    public void openMainMenu() {
        try {
            Inventory gui = Bukkit.createInventory(null, MAIN_MENU_SIZE, "§6§lTreasure Dungeon Editor - Main Menu");
            
            // Fill background
            fillBackground(gui);
            
            // Main action items
            gui.setItem(10, createMenuItem(Material.EMERALD_BLOCK, "§a§lCreate New Dungeon", 
                "§7Start building a new dungeon", "§7from scratch", "", "§eClick to begin!"));
            
            gui.setItem(12, createMenuItem(Material.BOOK, "§b§lLoad Template", 
                "§7Load a pre-built dungeon", "§7template to modify", "", "§eClick to browse!"));
            
            gui.setItem(14, createMenuItem(Material.MAP, "§e§lMap Editor", 
                "§7Edit map names, lore,", "§7and visual properties", "", "§eClick to edit!"));
            
            gui.setItem(16, createMenuItem(Material.REDSTONE, "§c§lAdvanced Tools", 
                "§7Access copy/paste, undo/redo", "§7and other advanced features", "", "§eClick to access!"));
            
            // Tool items
            gui.setItem(28, createMenuItem(Material.WOODEN_PICKAXE, "§6§lBlock Palette", 
                "§7Select blocks for building", "§7your dungeon structures", "", "§eClick to open!"));
            
            gui.setItem(30, createMenuItem(Material.COMPASS, "§d§lSpawn Editor", 
                "§7Configure mob spawn points", "§7and boss locations", "", "§eClick to edit!"));
            
            gui.setItem(32, createMenuItem(Material.DIAMOND_SWORD, "§4§lWave Designer", 
                "§7Design combat waves and", "§7configure mob behavior", "", "§eClick to design!"));
            
            gui.setItem(34, createMenuItem(Material.ENDER_CHEST, "§5§lLoot Editor", 
                "§7Configure treasure chests", "§7and reward systems", "", "§eClick to edit!"));
            
            // Preview and collaboration
            gui.setItem(37, createMenuItem(Material.SPYGLASS, "§3§lPreview Mode", 
                "§7Preview your dungeon", "§7with visual effects", "", "§eClick to toggle!"));
            
            gui.setItem(39, createMenuItem(Material.WRITABLE_BOOK, "§3§lExport/Import", 
                "§7Share dungeons with other", "§7servers or players", "", "§eClick to manage!"));
            
            // Utility items
            gui.setItem(46, createMenuItem(Material.PAPER, "§f§lHelp & Tutorials", 
                "§7Learn how to use the", "§7dungeon editor effectively", "", "§eClick for help!"));
            
            gui.setItem(50, createMenuItem(Material.BARRIER, "§c§lExit Editor", 
                "§7Close the dungeon editor", "§7and return to game", "", "§eClick to exit!"));
            
            // Status item
            gui.setItem(53, createStatusItem());
            
            session.setMode("MAIN_MENU");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening main editor menu", e);
        }
    }
    
    /**
     * Opens the block palette for material selection
     */
    public void openBlockPalette() {
        try {
            Inventory gui = Bukkit.createInventory(null, PALETTE_SIZE, "§6§lBlock Palette - Select Materials");
            
            // Fill background
            fillBackground(gui);
            
            // Common building blocks
            Material[] buildingBlocks = {
                Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
                Material.CRACKED_STONE_BRICKS, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.DEEPSLATE_BRICKS,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
                Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.SANDSTONE,
                Material.RED_SAND, Material.RED_SANDSTONE, Material.GRAVEL, Material.CLAY,
                Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.BLACKSTONE,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_STONE, Material.END_STONE_BRICKS
            };
            
            int slot = 10;
            for (Material material : buildingBlocks) {
                if (slot >= 44) break; // Don't fill bottom row
                if (slot % 9 == 8 || slot % 9 == 0) {
                    slot++; // Skip border slots
                    continue;
                }
                
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e" + formatMaterialName(material.name()));
                    meta.setLore(Arrays.asList("§7Click to select this material", "§7for building your dungeon"));
                    item.setItemMeta(meta);
                }
                
                gui.setItem(slot, item);
                slot++;
            }
            
            // Navigation items
            gui.setItem(45, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            gui.setItem(53, createMenuItem(Material.CHEST, "§b§lMore Blocks", 
                "§7Browse additional block categories"));
            
            session.setMode("BLOCK_PALETTE");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening block palette", e);
        }
    }
    
    /**
     * Opens the properties configuration panel
     */
    public void openPropertiesPanel() {
        try {
            Inventory gui = Bukkit.createInventory(null, PROPERTIES_SIZE, "§6§lDungeon Properties");
            
            // Fill background
            fillBackground(gui);
            
            // Property configuration items
            gui.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lDungeon Name", 
                "§7Set the display name for", "§7this dungeon type", "", "§eClick to edit!"));
            
            gui.setItem(12, createMenuItem(Material.EXPERIENCE_BOTTLE, "§b§lDifficulty Level", 
                "§7Configure the difficulty", "§7and challenge rating", "", "§eClick to adjust!"));
            
            gui.setItem(14, createMenuItem(Material.CLOCK, "§d§lWave Timing", 
                "§7Set delays between waves", "§7and boss spawn timing", "", "§eClick to configure!"));
            
            gui.setItem(16, createMenuItem(Material.GOLD_INGOT, "§6§lReward Multiplier", 
                "§7Adjust loot and experience", "§7rewards for this dungeon", "", "§eClick to modify!"));
            
            gui.setItem(19, createMenuItem(Material.ZOMBIE_HEAD, "§c§lMob Configuration", 
                "§7Configure mob types, health,", "§7and special abilities", "", "§eClick to edit!"));
            
            gui.setItem(21, createMenuItem(Material.WITHER_SKELETON_SKULL, "§4§lBoss Settings", 
                "§7Configure boss health, abilities,", "§7and special mechanics", "", "§eClick to configure!"));
            
            gui.setItem(23, createMenuItem(Material.BELL, "§e§lActivation Requirements", 
                "§7Set skill levels and other", "§7requirements to access", "", "§eClick to set!"));
            
            gui.setItem(25, createMenuItem(Material.COMPARATOR, "§3§lAdvanced Options", 
                "§7Configure advanced dungeon", "§7mechanics and features", "", "§eClick for more!"));
            
            // Navigation
            gui.setItem(36, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            gui.setItem(44, createMenuItem(Material.EMERALD, "§a§lSave Changes", 
                "§7Save all property changes", "§7to the dungeon configuration"));
            
            session.setMode("PROPERTIES");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening properties panel", e);
        }
    }
    
    /**
     * Opens the map editor for editing map properties
     */
    public void openMapEditor() {
        try {
            Inventory gui = Bukkit.createInventory(null, PROPERTIES_SIZE, "§6§lMap Editor - Customize Maps");
            
            // Fill background
            fillBackground(gui);
            
            // Map customization items
            gui.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lMap Display Name", 
                "§7Set the display name that", "§7appears on treasure maps", "", "§eClick to edit!"));
            
            gui.setItem(12, createMenuItem(Material.WRITABLE_BOOK, "§b§lMap Lore", 
                "§7Configure the lore text", "§7shown on treasure maps", "", "§eClick to edit!"));
            
            gui.setItem(14, createMenuItem(Material.FILLED_MAP, "§d§lMap Material", 
                "§7Change the item type", "§7used for treasure maps", "", "§eClick to change!"));
            
            gui.setItem(16, createMenuItem(Material.GLOWSTONE_DUST, "§6§lGlowing Effect", 
                "§7Toggle enchantment glow", "§7on treasure maps", "", "§eClick to toggle!"));
            
            gui.setItem(19, createMenuItem(Material.PAINTING, "§c§lCustom Model Data", 
                "§7Set custom model data for", "§7resource pack integration", "", "§eClick to set!"));
            
            gui.setItem(21, createMenuItem(Material.EXPERIENCE_BOTTLE, "§a§lDrop Chance", 
                "§7Configure the percentage", "§7chance to receive maps", "", "§eClick to adjust!"));
            
            gui.setItem(23, createMenuItem(Material.CLOCK, "§3§lCooldown Settings", 
                "§7Set cooldown time between", "§7receiving treasure maps", "", "§eClick to configure!"));
            
            gui.setItem(25, createMenuItem(Material.DIAMOND, "§5§lLevel Requirements", 
                "§7Configure skill level", "§7requirements for maps", "", "§eClick to set!"));
            
            // Preview section
            gui.setItem(31, createMenuItem(Material.ITEM_FRAME, "§6§lPreview Map", 
                "§7See how your map will", "§7look with current settings", "", "§eClick to preview!"));
            
            // Navigation
            gui.setItem(36, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            gui.setItem(44, createMenuItem(Material.EMERALD, "§a§lSave Map Settings", 
                "§7Save all map configuration", "§7changes to the config file"));
            
            session.setMode("MAP_EDITOR");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening map editor", e);
        }
    }
    
    /**
     * Opens the advanced tools panel
     */
    public void openAdvancedTools() {
        try {
            Inventory gui = Bukkit.createInventory(null, PROPERTIES_SIZE, "§6§lAdvanced Tools - Pro Features");
            
            // Fill background
            fillBackground(gui);
            
            // History tools
            gui.setItem(10, createMenuItem(Material.ARROW, "§c§lUndo", 
                "§7Undo the last action", "§7History: " + session.getUndoRedoTool().getUndoCount() + " actions", 
                "", session.getUndoRedoTool().canUndo() ? "§eClick to undo!" : "§7Nothing to undo"));
            
            gui.setItem(12, createMenuItem(Material.ARROW, "§a§lRedo", 
                "§7Redo the last undone action", "§7Available: " + session.getUndoRedoTool().getRedoCount() + " actions", 
                "", session.getUndoRedoTool().canRedo() ? "§eClick to redo!" : "§7Nothing to redo"));
            
            // Copy/Paste tools
            gui.setItem(14, createMenuItem(Material.SHEARS, "§b§lCopy Selection", 
                "§7Copy the selected area", "§7to clipboard for pasting", "", "§eClick to copy!"));
            
            gui.setItem(16, createMenuItem(Material.SLIME_BALL, "§d§lPaste", 
                "§7Paste clipboard contents", "§7at current location", 
                "§7Clipboard: " + session.getCopyPasteTool().getClipboardInfo(), 
                session.getCopyPasteTool().hasClipboard() ? "§eClick to paste!" : "§7Clipboard empty"));
            
            // Advanced selection tools
            gui.setItem(19, createMenuItem(Material.GOLDEN_PICKAXE, "§6§lFill Tool", 
                "§7Fill selected area with", "§7chosen material", "", "§eClick to use!"));
            
            gui.setItem(21, createMenuItem(Material.TNT, "§c§lClear Area", 
                "§7Clear selected area", "§7(set all blocks to air)", "", "§eClick to clear!"));
            
            gui.setItem(23, createMenuItem(Material.STRUCTURE_VOID, "§8§lReplace Tool", 
                "§7Replace specific blocks", "§7with another material", "", "§eClick to configure!"));
            
            gui.setItem(25, createMenuItem(Material.END_ROD, "§f§lLine Tool", 
                "§7Draw lines between", "§7two points", "", "§eClick to use!"));
            
            // Measurement tools
            gui.setItem(28, createMenuItem(Material.STICK, "§e§lMeasure Tool", 
                "§7Measure distances and", "§7calculate area volumes", "", "§eClick to measure!"));
            
            gui.setItem(30, createMenuItem(Material.COMPASS, "§3§lCenter Finder", 
                "§7Find the center point", "§7of selected area", "", "§eClick to find!"));
            
            // Transformation tools
            gui.setItem(32, createMenuItem(Material.PISTON, "§9§lMove Selection", 
                "§7Move selected area in", "§7any direction", "", "§eClick to move!"));
            
            gui.setItem(34, createMenuItem(Material.OBSERVER, "§5§lRotate Selection", 
                "§7Rotate selected area", "§790, 180, or 270 degrees", "", "§eClick to rotate!"));
            
            // Navigation
            gui.setItem(36, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            gui.setItem(44, createMenuItem(Material.BARRIER, "§c§lClear History", 
                "§7Clear all undo/redo history", "§7This cannot be undone!"));
            
            session.setMode("ADVANCED_TOOLS");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening advanced tools", e);
        }
    }
    
    /**
     * Opens the export/import panel
     */
    public void openExportImport() {
        try {
            Inventory gui = Bukkit.createInventory(null, PROPERTIES_SIZE, "§6§lExport/Import - Share Dungeons");
            
            // Fill background
            fillBackground(gui);
            
            // Export options
            gui.setItem(10, createMenuItem(Material.CHEST, "§a§lExport Dungeon", 
                "§7Export current dungeon", "§7to a shareable file", "", "§eClick to export!"));
            
            gui.setItem(12, createMenuItem(Material.BOOK, "§b§lExport Template", 
                "§7Save current dungeon", "§7as a reusable template", "", "§eClick to save!"));
            
            gui.setItem(14, createMenuItem(Material.MAP, "§e§lExport Schematic", 
                "§7Export as WorldEdit", "§7schematic file", "", "§eClick to export!"));
            
            gui.setItem(16, createMenuItem(Material.PAPER, "§f§lExport Config", 
                "§7Export configuration", "§7settings only", "", "§eClick to export!"));
            
            // Import options
            gui.setItem(19, createMenuItem(Material.ENDER_CHEST, "§d§lImport Dungeon", 
                "§7Import a dungeon from", "§7another server", "", "§eClick to import!"));
            
            gui.setItem(21, createMenuItem(Material.ENCHANTED_BOOK, "§5§lImport Template", 
                "§7Load a template from", "§7the templates folder", "", "§eClick to load!"));
            
            gui.setItem(23, createMenuItem(Material.FILLED_MAP, "§3§lImport Schematic", 
                "§7Import WorldEdit schematic", "§7as dungeon structure", "", "§eClick to import!"));
            
            gui.setItem(25, createMenuItem(Material.WRITABLE_BOOK, "§6§lImport Config", 
                "§7Import configuration", "§7from another dungeon", "", "§eClick to import!"));
            
            // Sharing options
            gui.setItem(28, createMenuItem(Material.BEACON, "§c§lShare Online", 
                "§7Upload to community", "§7dungeon repository", "", "§eClick to share!"));
            
            gui.setItem(30, createMenuItem(Material.GLOBE_BANNER_PATTERN, "§9§lBrowse Community", 
                "§7Download dungeons from", "§7the community repository", "", "§eClick to browse!"));
            
            gui.setItem(32, createMenuItem(Material.PLAYER_HEAD, "§a§lMy Uploads", 
                "§7Manage your uploaded", "§7dungeons and templates", "", "§eClick to manage!"));
            
            gui.setItem(34, createMenuItem(Material.HEART_OF_THE_SEA, "§b§lFeatured Dungeons", 
                "§7Browse featured and", "§7popular community dungeons", "", "§eClick to browse!"));
            
            // Navigation
            gui.setItem(36, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            gui.setItem(44, createMenuItem(Material.EMERALD, "§a§lRefresh List", 
                "§7Refresh available dungeons", "§7and templates list"));
            
            session.setMode("EXPORT_IMPORT");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening export/import panel", e);
        }
    }
    
    /**
     * Opens the collaborative editing panel
     */
    public void openCollaborativeEditor() {
        try {
            Inventory gui = Bukkit.createInventory(null, PROPERTIES_SIZE, "§6§lCollaborative Editing");
            
            // Fill background
            fillBackground(gui);
            
            // Collaboration options
            gui.setItem(10, createMenuItem(Material.PLAYER_HEAD, "§a§lInvite Players", 
                "§7Invite other players to", "§7collaborate on this dungeon", "", "§eClick to invite!"));
            
            gui.setItem(12, createMenuItem(Material.IRON_DOOR, "§b§lManage Permissions", 
                "§7Set what collaborators", "§7can and cannot do", "", "§eClick to manage!"));
            
            gui.setItem(14, createMenuItem(Material.BELL, "§e§lActive Sessions", 
                "§7View all active editing", "§7sessions for this dungeon", "", "§eClick to view!"));
            
            gui.setItem(16, createMenuItem(Material.BOOK, "§d§lEdit History", 
                "§7View complete edit history", "§7with player attribution", "", "§eClick to view!"));
            
            // Real-time features
            gui.setItem(19, createMenuItem(Material.REDSTONE_TORCH, "§c§lLive Cursors", 
                "§7Show other players'", "§7cursor positions", "", "§eClick to toggle!"));
            
            gui.setItem(21, createMenuItem(Material.CHAT, "§f§lEditor Chat", 
                "§7Open collaborative", "§7chat for this session", "", "§eClick to open!"));
            
            gui.setItem(23, createMenuItem(Material.CLOCK, "§6§lSync Changes", 
                "§7Synchronize all changes", "§7with other editors", "", "§eClick to sync!"));
            
            gui.setItem(25, createMenuItem(Material.BARRIER, "§c§lLock Editing", 
                "§7Temporarily lock editing", "§7to prevent conflicts", "", "§eClick to lock!"));
            
            // Navigation
            gui.setItem(36, createMenuItem(Material.ARROW, "§a§lBack to Main Menu", 
                "§7Return to the main editor menu"));
            
            session.setMode("COLLABORATIVE");
            session.getPlayer().openInventory(gui);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening collaborative editor", e);
        }
    }
    
    /**
     * Creates a menu item with the specified properties
     */
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            
            // Add enchant glow for important items
            if (name.contains("Create") || name.contains("Save")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a status item showing current session info
     */
    private ItemStack createStatusItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§lSession Status");
            
            List<String> lore = Arrays.asList(
                "§7Player: §e" + session.getPlayer().getName(),
                "§7Session ID: §e" + session.getSessionId().toString().substring(0, 8),
                "§7Current Mode: §e" + session.getCurrentMode(),
                "§7Selected Material: §e" + formatMaterialName(session.getSelectedMaterial().name()),
                "",
                session.hasUnsavedChanges() ? "§c⚠ Unsaved Changes" : "§a✓ All Changes Saved"
            );
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Fills the GUI background with decorative items
     */
    private void fillBackground(Inventory gui) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            background.setItemMeta(meta);
        }
        
        // Fill border slots
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, background);
            }
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