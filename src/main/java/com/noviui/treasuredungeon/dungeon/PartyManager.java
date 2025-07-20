package com.noviui.treasuredungeon.dungeon;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages party formation and dungeon scaling based on nearby players
 */
public class PartyManager {
    
    private final TreasureDungeonPlugin plugin;
    private final LanguageManager languageManager;
    
    // Party tracking
    private final Map<UUID, Set<UUID>> playerParties = new ConcurrentHashMap<>();
    private final Map<Location, Set<UUID>> bellProximityTracking = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int PARTY_DETECTION_RADIUS = 10; // Blocks
    private static final int MAX_PARTY_SIZE = 4;
    private static final int COUNTDOWN_SECONDS = 10;
    
    public PartyManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }
    
    /**
     * Detects nearby players when someone approaches a bell
     */
    public void detectNearbyPlayers(Player initiator, Location bellLocation) {
        if (initiator == null || bellLocation == null) {
            plugin.getLogger().warning("Invalid parameters for party detection");
            return;
        }
        
        try {
            Set<UUID> nearbyPlayers = new HashSet<>();
            nearbyPlayers.add(initiator.getUniqueId());
            
            // Find nearby players
            for (Player player : bellLocation.getWorld().getPlayers()) {
                if (player != null && !player.equals(initiator) && player.isOnline()) {
                    double distance = player.getLocation().distance(bellLocation);
                    if (distance <= PARTY_DETECTION_RADIUS && nearbyPlayers.size() < MAX_PARTY_SIZE) {
                        nearbyPlayers.add(player.getUniqueId());
                    }
                }
            }
            
            // Store proximity data
            bellProximityTracking.put(bellLocation, nearbyPlayers);
            
            // Notify players about party formation
            if (nearbyPlayers.size() > 1) {
                notifyPartyFormation(nearbyPlayers, bellLocation);
            } else {
                // Solo dungeon
                startDungeonCountdown(Collections.singletonList(initiator), bellLocation);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error detecting nearby players", e);
        }
    }
    
    /**
     * Notifies players about party formation and starts countdown
     */
    private void notifyPartyFormation(Set<UUID> playerIds, Location bellLocation) {
        try {
            List<Player> players = new ArrayList<>();
            
            // Convert UUIDs to Player objects
            for (UUID playerId : playerIds) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
            
            if (players.isEmpty()) {
                return;
            }
            
            // Notify about party formation
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("party_size", String.valueOf(players.size()));
            placeholders.put("countdown", String.valueOf(COUNTDOWN_SECONDS));
            
            String partyMessage = languageManager.getMessage("party-formed", placeholders);
            String countdownMessage = languageManager.getMessage("dungeon-countdown", placeholders);
            
            for (Player player : players) {
                player.sendMessage(languageManager.getPrefix() + partyMessage);
                player.sendMessage(languageManager.getPrefix() + countdownMessage);
            }
            
            // Start countdown
            startDungeonCountdown(players, bellLocation);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error notifying party formation", e);
        }
    }
    
    /**
     * Starts countdown before dungeon begins
     */
    private void startDungeonCountdown(List<Player> players, Location bellLocation) {
        new BukkitRunnable() {
            private int countdown = COUNTDOWN_SECONDS;
            
            @Override
            public void run() {
                try {
                    // Filter out offline players
                    List<Player> onlinePlayers = new ArrayList<>();
                    for (Player player : players) {
                        if (player != null && player.isOnline()) {
                            // Check if player is still nearby
                            if (player.getLocation().distance(bellLocation) <= PARTY_DETECTION_RADIUS * 1.5) {
                                onlinePlayers.add(player);
                            }
                        }
                    }
                    
                    if (onlinePlayers.isEmpty()) {
                        // No players left, cancel
                        this.cancel();
                        bellProximityTracking.remove(bellLocation);
                        return;
                    }
                    
                    if (countdown <= 0) {
                        // Start dungeon
                        this.cancel();
                        startDungeon(onlinePlayers, bellLocation);
                        return;
                    }
                    
                    // Countdown messages
                    if (countdown <= 5 || countdown % 5 == 0) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("seconds", String.valueOf(countdown));
                        
                        String message = languageManager.getMessage("countdown-tick", placeholders);
                        for (Player player : onlinePlayers) {
                            player.sendMessage(languageManager.getPrefix() + message);
                            player.sendTitle("§6" + countdown, "§eGet ready for battle!", 0, 20, 10);
                        }
                    }
                    
                    countdown--;
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in dungeon countdown", e);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }
    
    /**
     * Starts the dungeon with the formed party
     */
    private void startDungeon(List<Player> players, Location bellLocation) {
        try {
            if (players.isEmpty()) {
                plugin.getLogger().warning("Cannot start dungeon: no players");
                return;
            }
            
            // Remove from proximity tracking
            bellProximityTracking.remove(bellLocation);
            
            // Get dungeon type and scale difficulty
            Player initiator = players.get(0);
            UUID initiatorId = initiator.getUniqueId();
            
            // Get dungeon type from player data
            String dungeonType = plugin.getDungeonManager().getPlayerDungeonType(initiatorId);
            if (dungeonType == null) {
                dungeonType = plugin.getDungeonManager().selectRandomDungeonType();
            }
            
            if (dungeonType == null) {
                String errorMessage = languageManager.getMessage("no-dungeon-types");
                for (Player player : players) {
                    player.sendMessage(languageManager.getPrefix() + errorMessage);
                }
                return;
            }
            
            // Calculate scaled difficulty
            DungeonDifficulty difficulty = calculateDifficulty(players.size(), dungeonType);
            
            // Notify players about difficulty scaling
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("party_size", String.valueOf(players.size()));
            placeholders.put("difficulty", difficulty.getName());
            placeholders.put("mob_multiplier", String.valueOf(difficulty.getMobMultiplier()));
            placeholders.put("boss_multiplier", String.valueOf(difficulty.getBossMultiplier()));
            
            String difficultyMessage = languageManager.getMessage("difficulty-scaled", placeholders);
            for (Player player : players) {
                player.sendMessage(languageManager.getPrefix() + difficultyMessage);
            }
            
            // Start dungeon construction
            plugin.getDungeonBuilder().buildDungeon(bellLocation, dungeonType, initiatorId, players);
            
            // Store party information for loot distribution
            storePartyInfo(initiatorId, players, difficulty);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting party dungeon", e);
        }
    }
    
    /**
     * Calculates difficulty based on party size
     */
    private DungeonDifficulty calculateDifficulty(int partySize, String dungeonType) {
        String difficultyName;
        double mobMultiplier;
        double bossMultiplier;
        double lootMultiplier;
        
        switch (partySize) {
            case 1:
                difficultyName = "Solo";
                mobMultiplier = 1.0;
                bossMultiplier = 1.0;
                lootMultiplier = 1.0;
                break;
            case 2:
                difficultyName = "Duo";
                mobMultiplier = 1.5;
                bossMultiplier = 1.3;
                lootMultiplier = 1.2;
                break;
            case 3:
                difficultyName = "Trio";
                mobMultiplier = 2.0;
                bossMultiplier = 1.6;
                lootMultiplier = 1.5;
                break;
            case 4:
            default:
                difficultyName = "Squad";
                mobMultiplier = 2.5;
                bossMultiplier = 2.0;
                lootMultiplier = 2.0;
                break;
        }
        
        return new DungeonDifficulty(difficultyName, mobMultiplier, bossMultiplier, lootMultiplier);
    }
    
    /**
     * Stores party information for the dungeon
     */
    private void storePartyInfo(UUID initiatorId, List<Player> players, DungeonDifficulty difficulty) {
        try {
            Set<UUID> partyMembers = new HashSet<>();
            for (Player player : players) {
                if (player != null) {
                    partyMembers.add(player.getUniqueId());
                }
            }
            
            // Store party for each member
            for (UUID playerId : partyMembers) {
                playerParties.put(playerId, new HashSet<>(partyMembers));
            }
            
            // Store difficulty in dungeon manager
            plugin.getDungeonManager().setPartyDifficulty(initiatorId, difficulty);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error storing party information", e);
        }
    }
    
    /**
     * Gets party members for a player
     */
    public Set<UUID> getPartyMembers(UUID playerId) {
        return playerParties.getOrDefault(playerId, Collections.singleton(playerId));
    }
    
    /**
     * Checks if a player is in a party
     */
    public boolean isInParty(UUID playerId) {
        Set<UUID> party = playerParties.get(playerId);
        return party != null && party.size() > 1;
    }
    
    /**
     * Removes a player from their party
     */
    public void removeFromParty(UUID playerId) {
        Set<UUID> party = playerParties.remove(playerId);
        if (party != null) {
            // Remove this player from other party members' lists
            for (UUID memberId : party) {
                if (!memberId.equals(playerId)) {
                    Set<UUID> memberParty = playerParties.get(memberId);
                    if (memberParty != null) {
                        memberParty.remove(playerId);
                        if (memberParty.size() <= 1) {
                            playerParties.remove(memberId);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Cleans up party data
     */
    public void cleanup() {
        try {
            playerParties.clear();
            bellProximityTracking.clear();
            plugin.getLogger().info("Party manager cleanup completed");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during party manager cleanup", e);
        }
    }
    
    /**
     * Inner class representing dungeon difficulty
     */
    public static class DungeonDifficulty {
        private final String name;
        private final double mobMultiplier;
        private final double bossMultiplier;
        private final double lootMultiplier;
        
        public DungeonDifficulty(String name, double mobMultiplier, double bossMultiplier, double lootMultiplier) {
            this.name = name;
            this.mobMultiplier = mobMultiplier;
            this.bossMultiplier = bossMultiplier;
            this.lootMultiplier = lootMultiplier;
        }
        
        public String getName() { return name; }
        public double getMobMultiplier() { return mobMultiplier; }
        public double getBossMultiplier() { return bossMultiplier; }
        public double getLootMultiplier() { return lootMultiplier; }
    }
}