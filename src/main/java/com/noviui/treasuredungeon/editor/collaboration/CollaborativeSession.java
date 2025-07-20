package com.noviui.treasuredungeon.editor.collaboration;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a collaborative editing session
 */
public class CollaborativeSession {
    
    private final String dungeonId;
    private final Player owner;
    private final Map<UUID, Player> participants = new ConcurrentHashMap<>();
    private final Map<UUID, CollaboratorPermission> permissions = new ConcurrentHashMap<>();
    private final Set<UUID> pendingInvitations = ConcurrentHashMap.newKeySet();
    private final List<CollaborativeAction> actionHistory = new ArrayList<>();
    
    private boolean locked = false;
    private long createdAt;
    
    public CollaborativeSession(String dungeonId, Player owner) {
        this.dungeonId = dungeonId;
        this.owner = owner;
        this.createdAt = System.currentTimeMillis();
        
        // Add owner as participant with full permissions
        participants.put(owner.getUniqueId(), owner);
        permissions.put(owner.getUniqueId(), CollaboratorPermission.OWNER);
    }
    
    /**
     * Invites a player to the session
     */
    public boolean invitePlayer(Player invitee, Player inviter) {
        if (invitee == null || inviter == null) {
            return false;
        }
        
        // Check if inviter has permission to invite
        CollaboratorPermission inviterPerm = permissions.get(inviter.getUniqueId());
        if (inviterPerm == null || !inviterPerm.canInvite()) {
            return false;
        }
        
        // Check if already invited or participating
        if (participants.containsKey(invitee.getUniqueId()) || 
            pendingInvitations.contains(invitee.getUniqueId())) {
            return false;
        }
        
        pendingInvitations.add(invitee.getUniqueId());
        
        // Send invitation message
        invitee.sendMessage("§6[Collaboration] §e" + inviter.getName() + 
            " invited you to collaborate on dungeon: " + dungeonId);
        invitee.sendMessage("§7Use /treasure collab accept " + dungeonId + " to join!");
        
        return true;
    }
    
    /**
     * Accepts an invitation
     */
    public boolean acceptInvitation(Player player) {
        if (player == null || !pendingInvitations.contains(player.getUniqueId())) {
            return false;
        }
        
        pendingInvitations.remove(player.getUniqueId());
        participants.put(player.getUniqueId(), player);
        permissions.put(player.getUniqueId(), CollaboratorPermission.COLLABORATOR);
        
        // Notify all participants
        broadcastMessage("§a" + player.getName() + " joined the collaborative session!");
        
        return true;
    }
    
    /**
     * Removes a player from the session
     */
    public void removePlayer(Player player) {
        if (player == null) return;
        
        participants.remove(player.getUniqueId());
        permissions.remove(player.getUniqueId());
        pendingInvitations.remove(player.getUniqueId());
        
        // Notify remaining participants
        if (!participants.isEmpty()) {
            broadcastMessage("§c" + player.getName() + " left the collaborative session.");
        }
    }
    
    /**
     * Broadcasts an action to all participants
     */
    public void broadcastAction(Player actor, String action, Object data) {
        CollaborativeAction collabAction = new CollaborativeAction(
            actor.getUniqueId(), action, data, System.currentTimeMillis()
        );
        
        actionHistory.add(collabAction);
        
        // Notify all participants except the actor
        for (Player participant : participants.values()) {
            if (participant != null && !participant.equals(actor) && participant.isOnline()) {
                participant.sendMessage("§7[Collab] §e" + actor.getName() + " " + action);
            }
        }
    }
    
    /**
     * Broadcasts a message to all participants
     */
    public void broadcastMessage(String message) {
        for (Player participant : participants.values()) {
            if (participant != null && participant.isOnline()) {
                participant.sendMessage(message);
            }
        }
    }
    
    /**
     * Checks if a player has permission for an action
     */
    public boolean hasPermission(Player player, String action) {
        CollaboratorPermission permission = permissions.get(player.getUniqueId());
        if (permission == null) {
            return false;
        }
        
        return permission.hasPermission(action);
    }
    
    /**
     * Sets the session lock status
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
        broadcastMessage(locked ? "§cSession locked for editing." : "§aSession unlocked for editing.");
    }
    
    // Getters
    public String getDungeonId() { return dungeonId; }
    public Player getOwner() { return owner; }
    public Collection<Player> getParticipants() { return participants.values(); }
    public boolean isLocked() { return locked; }
    public long getCreatedAt() { return createdAt; }
    public List<CollaborativeAction> getActionHistory() { return new ArrayList<>(actionHistory); }
    
    /**
     * Collaborative action record
     */
    public static class CollaborativeAction {
        private final UUID playerId;
        private final String action;
        private final Object data;
        private final long timestamp;
        
        public CollaborativeAction(UUID playerId, String action, Object data, long timestamp) {
            this.playerId = playerId;
            this.action = action;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getAction() { return action; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Collaborator permission levels
     */
    public enum CollaboratorPermission {
        OWNER(true, true, true, true, true),
        ADMIN(true, true, true, true, false),
        COLLABORATOR(true, true, false, false, false),
        VIEWER(false, false, false, false, false);
        
        private final boolean canEdit;
        private final boolean canInvite;
        private final boolean canKick;
        private final boolean canChangePermissions;
        private final boolean canDelete;
        
        CollaboratorPermission(boolean canEdit, boolean canInvite, boolean canKick, 
                             boolean canChangePermissions, boolean canDelete) {
            this.canEdit = canEdit;
            this.canInvite = canInvite;
            this.canKick = canKick;
            this.canChangePermissions = canChangePermissions;
            this.canDelete = canDelete;
        }
        
        public boolean hasPermission(String action) {
            switch (action.toLowerCase()) {
                case "edit": return canEdit;
                case "invite": return canInvite;
                case "kick": return canKick;
                case "permissions": return canChangePermissions;
                case "delete": return canDelete;
                default: return false;
            }
        }
        
        public boolean canEdit() { return canEdit; }
        public boolean canInvite() { return canInvite; }
        public boolean canKick() { return canKick; }
        public boolean canChangePermissions() { return canChangePermissions; }
        public boolean canDelete() { return canDelete; }
    }
}