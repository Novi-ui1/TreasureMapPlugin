package com.noviui.treasuredungeon.utils;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles checking for plugin updates
 */
public class UpdateChecker {
    
    private final TreasureDungeonPlugin plugin;
    private final String currentVersion;
    private final String updateUrl;
    
    public UpdateChecker(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.updateUrl = "https://api.github.com/repos/novi-ui/treasuredungeon/releases/latest";
    }
    
    /**
     * Checks for updates asynchronously
     */
    public CompletableFuture<UpdateResult> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(updateUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "TreasureDungeon-Plugin");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    return parseUpdateResponse(response.toString());
                } else {
                    plugin.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
                    return new UpdateResult(false, currentVersion, null, "Failed to connect to update server");
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error checking for updates", e);
                return new UpdateResult(false, currentVersion, null, "Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parses the GitHub API response to extract version information
     */
    private UpdateResult parseUpdateResponse(String jsonResponse) {
        try {
            // Simple JSON parsing for version extraction
            // In a real implementation, you might want to use a JSON library
            String tagNamePattern = "\"tag_name\":\"";
            int tagStart = jsonResponse.indexOf(tagNamePattern);
            if (tagStart == -1) {
                return new UpdateResult(false, currentVersion, null, "Could not parse version information");
            }
            
            tagStart += tagNamePattern.length();
            int tagEnd = jsonResponse.indexOf("\"", tagStart);
            if (tagEnd == -1) {
                return new UpdateResult(false, currentVersion, null, "Could not parse version information");
            }
            
            String latestVersion = jsonResponse.substring(tagStart, tagEnd);
            
            // Remove 'v' prefix if present
            if (latestVersion.startsWith("v")) {
                latestVersion = latestVersion.substring(1);
            }
            
            boolean hasUpdate = isNewerVersion(latestVersion, currentVersion);
            String downloadUrl = extractDownloadUrl(jsonResponse);
            
            return new UpdateResult(hasUpdate, currentVersion, latestVersion, 
                hasUpdate ? "Update available!" : "Plugin is up to date");
                
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing update response", e);
            return new UpdateResult(false, currentVersion, null, "Error parsing response");
        }
    }
    
    /**
     * Extracts download URL from GitHub API response
     */
    private String extractDownloadUrl(String jsonResponse) {
        try {
            String downloadPattern = "\"browser_download_url\":\"";
            int downloadStart = jsonResponse.indexOf(downloadPattern);
            if (downloadStart == -1) {
                return null;
            }
            
            downloadStart += downloadPattern.length();
            int downloadEnd = jsonResponse.indexOf("\"", downloadStart);
            if (downloadEnd == -1) {
                return null;
            }
            
            return jsonResponse.substring(downloadStart, downloadEnd);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error extracting download URL", e);
            return null;
        }
    }
    
    /**
     * Compares version strings to determine if an update is available
     */
    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
            
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Error comparing versions: " + latest + " vs " + current);
            return false;
        }
    }
    
    /**
     * Notifies administrators about available updates
     */
    public void notifyAdmins(UpdateResult result) {
        if (result.hasUpdate()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = String.format(
                    "§6[TreasureDungeon] §eUpdate available! Current: %s, Latest: %s",
                    result.getCurrentVersion(),
                    result.getLatestVersion()
                );
                
                Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("treasure.admin"))
                    .forEach(player -> player.sendMessage(message));
                    
                plugin.getLogger().info("Update available: " + result.getLatestVersion());
            });
        }
    }
    
    /**
     * Result of an update check
     */
    public static class UpdateResult {
        private final boolean hasUpdate;
        private final String currentVersion;
        private final String latestVersion;
        private final String message;
        
        public UpdateResult(boolean hasUpdate, String currentVersion, String latestVersion, String message) {
            this.hasUpdate = hasUpdate;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.message = message;
        }
        
        public boolean hasUpdate() {
            return hasUpdate;
        }
        
        public String getCurrentVersion() {
            return currentVersion;
        }
        
        public String getLatestVersion() {
            return latestVersion;
        }
        
        public String getMessage() {
            return message;
        }
    }
}