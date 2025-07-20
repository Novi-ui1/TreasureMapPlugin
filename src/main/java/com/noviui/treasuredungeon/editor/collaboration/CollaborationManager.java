package com.noviui.treasuredungeon.editor.collaboration;

import com.noviui.treasuredungeon.TreasureDungeonPlugin;
import com.noviui.treasuredungeon.config.LanguageManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages collaborative editing sessions
 */
public class CollaborationManager {
    
    private final TreasureDungeonPlugin plugin;
    private final LanguageManager languageManager;
    
    // Active collaborative sessions
    private final Map<String, CollaborativeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSessions = new ConcurrentHashMap<>();
    
    public CollaborationManager(TreasureDungeonPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }
    
    /**
     * Creates a new collaborative session
     */
    public CollaborativeSession createSession(String dungeonId, Player owner) {
        if (dungeonId == null || owner == null) {
            return null;
        }
        
        CollaborativeSession session = new CollaborativeSession(dungeonId, owner);
        activeSessions.put(dungeonId, session);
        playerSessions.put(owner.getUniqueId(), dungeonId);
        
        return session;
    }
    
    /**
     * Invites a player to a collaborative session
     */
    public boolean invitePlayer(String dungeonId, Player invitee, Player inviter) {
        CollaborativeSession session = activeSessions.get(dungeonId);
        if (session == null) {
            return false;
        }
        
        return session.invitePlayer(invitee, inviter);
    }
    
    /**
     * Accepts an invitation to a collaborative session
     */
    public boolean acceptInvitation(Player player, String dungeonId) {
        CollaborativeSession session = activeSessions.get(dungeonId);
        if (session == null) {
            return false;
        }
        
        if (session.acceptInvitation(player)) {
            playerSessions.put(player.getUniqueId(), dungeonId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes a player from a collaborative session
     */
    public void removePlayer(Player player) {
        String dungeonId = playerSessions.remove(player.getUniqueId());
        if (dungeonId != null) {
            CollaborativeSession session = activeSessions.get(dungeonId);
            if (session != null) {
                session.removePlayer(player);
                
                // Remove session if empty
                if (session.getParticipants().isEmpty()) {
                    activeSessions.remove(dungeonId);
                }
            }
        }
    }
    
    /**
     * Gets the collaborative session for a player
     */
    public CollaborativeSession getPlayerSession(Player player) {
        String dungeonId = playerSessions.get(player.getUniqueId());
        return dungeonId != null ? activeSessions.get(dungeonId) : null;
    }
    
    /**
     * Broadcasts an action to all participants in a session
     */
    public void broadcastAction(String dungeonId, Player actor, String action, Object data) {
        CollaborativeSession session = activeSessions.get(dungeonId);
        if (session != null) {
            session.broadcastAction(actor, action, data);
        }
    }
    
    /**
     * Gets all active sessions
     */
    public Collection<CollaborativeSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        activeSessions.clear();
        playerSessions.clear();
    }
}