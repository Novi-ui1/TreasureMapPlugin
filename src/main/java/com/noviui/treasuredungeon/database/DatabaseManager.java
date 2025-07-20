package com.noviui.treasuredungeon.database;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages database connections and operations for MySQL/PostgreSQL support
 */
public class DatabaseManager {
    
    private final TreasureDungeonPlugin plugin;
    private final ConfigManager configManager;
    
    private HikariDataSource dataSource;
    private boolean enabled = false;
    private DatabaseType databaseType;
    
    public DatabaseManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * Initializes database connection
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!configManager.isDatabaseEnabled()) {
                    plugin.getLogger().info("Database support is disabled, using file storage");
                    return false;
                }
                
                String type = configManager.getDatabaseType().toLowerCase();
                switch (type) {
                    case "mysql":
                        databaseType = DatabaseType.MYSQL;
                        break;
                    case "postgresql":
                        databaseType = DatabaseType.POSTGRESQL;
                        break;
                    default:
                        plugin.getLogger().warning("Unsupported database type: " + type);
                        return false;
                }
                
                // Setup connection pool
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(buildJdbcUrl());
                config.setUsername(configManager.getDatabaseUsername());
                config.setPassword(configManager.getDatabasePassword());
                config.setMaximumPoolSize(configManager.getDatabaseMaxConnections());
                config.setMinimumIdle(configManager.getDatabaseMinConnections());
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                
                // Database-specific settings
                if (databaseType == DatabaseType.MYSQL) {
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("useServerPrepStmts", "true");
                    config.addDataSourceProperty("useLocalSessionState", "true");
                    config.addDataSourceProperty("rewriteBatchedStatements", "true");
                    config.addDataSourceProperty("cacheResultSetMetadata", "true");
                    config.addDataSourceProperty("cacheServerConfiguration", "true");
                    config.addDataSourceProperty("elideSetAutoCommits", "true");
                    config.addDataSourceProperty("maintainTimeStats", "false");
                }
                
                dataSource = new HikariDataSource(config);
                
                // Test connection
                try (Connection connection = dataSource.getConnection()) {
                    if (connection.isValid(5)) {
                        plugin.getLogger().info("Database connection established successfully");
                        enabled = true;
                        
                        // Create tables
                        createTables();
                        return true;
                    }
                }
                
                plugin.getLogger().severe("Failed to establish database connection");
                return false;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error initializing database", e);
                return false;
            }
        });
    }
    
    /**
     * Builds JDBC URL based on configuration
     */
    private String buildJdbcUrl() {
        String host = configManager.getDatabaseHost();
        int port = configManager.getDatabasePort();
        String database = configManager.getDatabaseName();
        
        switch (databaseType) {
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, database, configManager.isDatabaseSSL());
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s?ssl=%s",
                    host, port, database, configManager.isDatabaseSSL() ? "require" : "disable");
            default:
                throw new IllegalStateException("Unsupported database type: " + databaseType);
        }
    }
    
    /**
     * Creates necessary database tables
     */
    private void createTables() {
        try (Connection connection = getConnection()) {
            // Players table
            String playersTable = """
                CREATE TABLE IF NOT EXISTS td_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_dungeons_completed INT DEFAULT 0,
                    total_damage_dealt BIGINT DEFAULT 0,
                    total_bosses_killed INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
            
            // Cooldowns table
            String cooldownsTable = """
                CREATE TABLE IF NOT EXISTS td_cooldowns (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    skill VARCHAR(50) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_player_skill (player_uuid, skill),
                    FOREIGN KEY (player_uuid) REFERENCES td_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Active dungeons table
            String dungeonsTable = """
                CREATE TABLE IF NOT EXISTS td_active_dungeons (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    skill VARCHAR(50) NOT NULL,
                    dungeon_type VARCHAR(50) NOT NULL,
                    world_name VARCHAR(50) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    status ENUM('in-progress', 'completed', 'failed') DEFAULT 'in-progress',
                    party_size INT DEFAULT 1,
                    difficulty_multiplier DECIMAL(3,2) DEFAULT 1.00,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL,
                    UNIQUE KEY unique_player_skill (player_uuid, skill),
                    FOREIGN KEY (player_uuid) REFERENCES td_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Dungeon completions table
            String completionsTable = """
                CREATE TABLE IF NOT EXISTS td_dungeon_completions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    dungeon_type VARCHAR(50) NOT NULL,
                    skill VARCHAR(50) NOT NULL,
                    party_size INT NOT NULL,
                    completion_time INT NOT NULL,
                    damage_dealt BIGINT NOT NULL,
                    boss_killed BOOLEAN DEFAULT FALSE,
                    loot_received TEXT,
                    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_uuid) REFERENCES td_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Statistics table
            String statisticsTable = """
                CREATE TABLE IF NOT EXISTS td_statistics (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    stat_type VARCHAR(50) NOT NULL,
                    stat_value BIGINT NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_player_stat (player_uuid, stat_type),
                    FOREIGN KEY (player_uuid) REFERENCES td_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Adjust for PostgreSQL
            if (databaseType == DatabaseType.POSTGRESQL) {
                playersTable = playersTable.replace("AUTO_INCREMENT", "SERIAL")
                    .replace("ON UPDATE CURRENT_TIMESTAMP", "");
                cooldownsTable = cooldownsTable.replace("AUTO_INCREMENT", "SERIAL");
                dungeonsTable = dungeonsTable.replace("AUTO_INCREMENT", "SERIAL")
                    .replace("ENUM('in-progress', 'completed', 'failed')", "VARCHAR(20) CHECK (status IN ('in-progress', 'completed', 'failed'))");
                completionsTable = completionsTable.replace("AUTO_INCREMENT", "SERIAL");
                statisticsTable = statisticsTable.replace("AUTO_INCREMENT", "SERIAL")
                    .replace("ON UPDATE CURRENT_TIMESTAMP", "");
            }
            
            // Execute table creation
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(playersTable);
                stmt.execute(cooldownsTable);
                stmt.execute(dungeonsTable);
                stmt.execute(completionsTable);
                stmt.execute(statisticsTable);
            }
            
            plugin.getLogger().info("Database tables created/verified successfully");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating database tables", e);
        }
    }
    
    /**
     * Gets a database connection
     */
    public Connection getConnection() throws SQLException {
        if (!enabled || dataSource == null) {
            throw new SQLException("Database is not enabled or not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Saves player cooldown to database
     */
    public CompletableFuture<Void> saveCooldown(UUID playerId, String skill, Instant expiresAt) {
        return CompletableFuture.runAsync(() -> {
            if (!enabled) return;
            
            try (Connection connection = getConnection()) {
                // First ensure player exists
                ensurePlayerExists(connection, playerId);
                
                String sql = """
                    INSERT INTO td_cooldowns (player_uuid, skill, expires_at) 
                    VALUES (?, ?, ?) 
                    ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at)
                    """;
                
                if (databaseType == DatabaseType.POSTGRESQL) {
                    sql = """
                        INSERT INTO td_cooldowns (player_uuid, skill, expires_at) 
                        VALUES (?, ?, ?) 
                        ON CONFLICT (player_uuid, skill) 
                        DO UPDATE SET expires_at = EXCLUDED.expires_at
                        """;
                }
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, skill);
                    stmt.setTimestamp(3, Timestamp.from(expiresAt));
                    stmt.executeUpdate();
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving cooldown to database", e);
            }
        });
    }
    
    /**
     * Checks if player is on cooldown
     */
    public CompletableFuture<Boolean> isOnCooldown(UUID playerId, String skill) {
        return CompletableFuture.supplyAsync(() -> {
            if (!enabled) return false;
            
            try (Connection connection = getConnection()) {
                String sql = """
                    SELECT expires_at FROM td_cooldowns 
                    WHERE player_uuid = ? AND skill = ? AND expires_at > CURRENT_TIMESTAMP
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, skill);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error checking cooldown in database", e);
                return false;
            }
        });
    }
    
    /**
     * Saves active dungeon to database
     */
    public CompletableFuture<Void> saveActiveDungeon(UUID playerId, String skill, String dungeonType, 
                                                    String worldName, int x, int y, int z, int partySize, double difficultyMultiplier) {
        return CompletableFuture.runAsync(() -> {
            if (!enabled) return;
            
            try (Connection connection = getConnection()) {
                ensurePlayerExists(connection, playerId);
                
                String sql = """
                    INSERT INTO td_active_dungeons (player_uuid, skill, dungeon_type, world_name, x, y, z, party_size, difficulty_multiplier) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) 
                    ON DUPLICATE KEY UPDATE 
                        dungeon_type = VALUES(dungeon_type),
                        world_name = VALUES(world_name),
                        x = VALUES(x),
                        y = VALUES(y),
                        z = VALUES(z),
                        party_size = VALUES(party_size),
                        difficulty_multiplier = VALUES(difficulty_multiplier),
                        status = 'in-progress',
                        created_at = CURRENT_TIMESTAMP
                    """;
                
                if (databaseType == DatabaseType.POSTGRESQL) {
                    sql = """
                        INSERT INTO td_active_dungeons (player_uuid, skill, dungeon_type, world_name, x, y, z, party_size, difficulty_multiplier) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (player_uuid, skill) 
                        DO UPDATE SET 
                            dungeon_type = EXCLUDED.dungeon_type,
                            world_name = EXCLUDED.world_name,
                            x = EXCLUDED.x,
                            y = EXCLUDED.y,
                            z = EXCLUDED.z,
                            party_size = EXCLUDED.party_size,
                            difficulty_multiplier = EXCLUDED.difficulty_multiplier,
                            status = 'in-progress',
                            created_at = CURRENT_TIMESTAMP
                        """;
                }
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, skill);
                    stmt.setString(3, dungeonType);
                    stmt.setString(4, worldName);
                    stmt.setInt(5, x);
                    stmt.setInt(6, y);
                    stmt.setInt(7, z);
                    stmt.setInt(8, partySize);
                    stmt.setDouble(9, difficultyMultiplier);
                    stmt.executeUpdate();
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving active dungeon to database", e);
            }
        });
    }
    
    /**
     * Records dungeon completion
     */
    public CompletableFuture<Void> recordCompletion(UUID playerId, String dungeonType, String skill, 
                                                   int partySize, int completionTime, long damageDealt, 
                                                   boolean bossKilled, String lootReceived) {
        return CompletableFuture.runAsync(() -> {
            if (!enabled) return;
            
            try (Connection connection = getConnection()) {
                ensurePlayerExists(connection, playerId);
                
                // Record completion
                String sql = """
                    INSERT INTO td_dungeon_completions 
                    (player_uuid, dungeon_type, skill, party_size, completion_time, damage_dealt, boss_killed, loot_received) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, dungeonType);
                    stmt.setString(3, skill);
                    stmt.setInt(4, partySize);
                    stmt.setInt(5, completionTime);
                    stmt.setLong(6, damageDealt);
                    stmt.setBoolean(7, bossKilled);
                    stmt.setString(8, lootReceived);
                    stmt.executeUpdate();
                }
                
                // Update player statistics
                updatePlayerStats(connection, playerId, damageDealt, bossKilled);
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error recording dungeon completion", e);
            }
        });
    }
    
    /**
     * Gets leaderboard data
     */
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String statType, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            
            if (!enabled) return leaderboard;
            
            try (Connection connection = getConnection()) {
                String sql = """
                    SELECT p.username, s.stat_value 
                    FROM td_statistics s 
                    JOIN td_players p ON s.player_uuid = p.uuid 
                    WHERE s.stat_type = ? 
                    ORDER BY s.stat_value DESC 
                    LIMIT ?
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, statType);
                    stmt.setInt(2, limit);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        int position = 1;
                        while (rs.next()) {
                            leaderboard.add(new LeaderboardEntry(
                                position++,
                                rs.getString("username"),
                                rs.getLong("stat_value")
                            ));
                        }
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting leaderboard data", e);
            }
            
            return leaderboard;
        });
    }
    
    /**
     * Ensures a player exists in the database
     */
    private void ensurePlayerExists(Connection connection, UUID playerId) throws SQLException {
        String sql = """
            INSERT IGNORE INTO td_players (uuid, username) 
            VALUES (?, ?)
            """;
        
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = """
                INSERT INTO td_players (uuid, username) 
                VALUES (?, ?) 
                ON CONFLICT (uuid) DO NOTHING
                """;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, "Unknown"); // Will be updated when player joins
            stmt.executeUpdate();
        }
    }
    
    /**
     * Updates player statistics
     */
    private void updatePlayerStats(Connection connection, UUID playerId, long damageDealt, boolean bossKilled) throws SQLException {
        // Update total damage
        updateStatistic(connection, playerId, "total_damage", damageDealt);
        
        // Update dungeons completed
        updateStatistic(connection, playerId, "dungeons_completed", 1);
        
        // Update bosses killed
        if (bossKilled) {
            updateStatistic(connection, playerId, "bosses_killed", 1);
        }
    }
    
    /**
     * Updates a specific statistic
     */
    private void updateStatistic(Connection connection, UUID playerId, String statType, long value) throws SQLException {
        String sql = """
            INSERT INTO td_statistics (player_uuid, stat_type, stat_value) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE stat_value = stat_value + VALUES(stat_value)
            """;
        
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = """
                INSERT INTO td_statistics (player_uuid, stat_type, stat_value) 
                VALUES (?, ?, ?) 
                ON CONFLICT (player_uuid, stat_type) 
                DO UPDATE SET stat_value = td_statistics.stat_value + EXCLUDED.stat_value
                """;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, statType);
            stmt.setLong(3, value);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Closes database connection
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed");
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Database types enum
     */
    public enum DatabaseType {
        MYSQL, POSTGRESQL
    }
    
    /**
     * Leaderboard entry class
     */
    public static class LeaderboardEntry {
        private final int position;
        private final String playerName;
        private final long value;
        
        public LeaderboardEntry(int position, String playerName, long value) {
            this.position = position;
            this.playerName = playerName;
            this.value = value;
        }
        
        public int getPosition() { return position; }
        public String getPlayerName() { return playerName; }
        public long getValue() { return value; }
    }
}