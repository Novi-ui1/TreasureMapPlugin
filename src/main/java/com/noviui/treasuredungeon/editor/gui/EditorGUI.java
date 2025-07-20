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
            Inventory gui = Bukkit.createInventory(null, MAIN_MENU_SIZE, "§6§lTreasure Dungeon Editor");
            
            // Fill background
            fillBackground(gui);
            
            // Main action items
            gui.setItem(10, createMenuItem(Material.EMERALD_BLOCK, "§a§lCreate New Dungeon", 
                "§7Start building a new dungeon", "§7from scratch", "", "§eClick to begin!"));
            
            gui.setItem(12, createMenuItem(Material.BOOK, "§b§lLoad Template", 
                "§7Load a pre-built dungeon", "§7template to modify", "", "§eClick to browse!"));
            
            gui.setItem(14, createMenuItem(Material.CHEST, "§e§lManage Dungeons", 
                "§7View and edit existing", "§7dungeon configurations", "", "§eClick to manage!"));
            
            gui.setItem(16, createMenuItem(Material.REDSTONE, "§c§lAdvanced Settings", 
                "§7Configure advanced dungeon", "§7properties and behaviors", "", "§eClick to configure!"));
            
            // Tool items
            gui.setItem(28, createMenuItem(Material.WOODEN_PICKAXE, "§6§lBlock Palette", 
                "§7Select blocks for building", "§7your dungeon structures", "", "§eClick to open!"));
            
            gui.setItem(30, createMenuItem(Material.COMPASS, "§d§lSpawn Editor", 
                "§7Configure mob spawn points", "§7and boss locations", "", "§eClick to edit!"));
            
            gui.setItem(32, createMenuItem(Material.DIAMOND_SWORD, "§4§lWave Designer", 
                "§7Design combat waves and", "§7configure mob behavior", "", "§eClick to design!"));
            
            gui.setItem(34, createMenuItem(Material.ENDER_CHEST, "§5§lLoot Editor", 
                "§7Configure treasure chests", "§7and reward systems", "", "§eClick to edit!"));
            
            // Utility items
            gui.setItem(46, createMenuItem(Material.PAPER, "§f§lHelp & Tutorials", 
                "§7Learn how to use the", "§7dungeon editor effectively", "", "§eClick for help!"));
            
            gui.setItem(48, createMenuItem(Material.WRITABLE_BOOK, "§3§lExport/Import", 
                "§7Share dungeons with other", "§7servers or players", "", "§eClick to manage!"));
            
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