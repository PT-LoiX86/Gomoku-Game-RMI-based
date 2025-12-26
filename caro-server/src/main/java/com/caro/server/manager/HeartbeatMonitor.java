package com.caro.server.manager;

import com.caro.common.model.Room;
import com.caro.common.service.ClientCallback;
import com.caro.server.service.GameServiceImpl; // Needed to access broadcast logic? Or callback logic.
// Ideally, we decouple this, but for simplicity, we will use Managers directly.

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatMonitor {

    private final SessionManager sessionManager;
    private final RoomManager roomManager;
    private final ScheduledExecutorService scheduler;
    private final GameServiceImpl gameService;
    
    // Config: How often to check
    private static final int CHECK_INTERVAL_SECONDS = 3;
    // Config: How long before declaring dead
    private static final long TIMEOUT_THRESHOLD_MS = 10000; // 10 seconds

    public HeartbeatMonitor(GameServiceImpl service) {
        this.gameService = service;
        this.sessionManager = SessionManager.getInstance();
        this.roomManager = RoomManager.getInstance();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        System.out.println("Heartbeat Monitor started...");
        scheduler.scheduleAtFixedRate(this::checkClients, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    public void stop() {
        scheduler.shutdown();
    }

    private void checkClients() {
        long now = System.currentTimeMillis();
        Map<String, Long> heartbeats = sessionManager.getHeartbeats();

        // Iterate over a copy of the keySet to avoid ConcurrentModificationException
        // (Though ConcurrentHashMap handles this well, it's good practice)
        for (String username : heartbeats.keySet()) {
            long lastSeen = heartbeats.get(username);
            
            if ((now - lastSeen) > TIMEOUT_THRESHOLD_MS) {
                System.out.println("Heartbeat timeout for user: " + username + ". Disconnecting...");
                handleDisconnect(username);
            }
        }
    }

    private void handleDisconnect(String username) {
        // 1. Check if they are in a room
        Room room = roomManager.getRoomByUsername(username);
        
        if (room != null) {
            handleRoomDisconnect(room, username);
        }

        // 2. Remove from session manager (This effectively "logs them out")
        sessionManager.removeUser(username);

        gameService.broadcastLobbyUpdate();
        gameService.broadcastUserList(); 
    }
    
    private void handleRoomDisconnect(Room room, String disconnectedUser) {
        boolean isHost = disconnectedUser.equals(room.getHostUsername());
        
        if (isHost) {
            // Case A: Host Disconnected -> Destroy Room & Kick Guest
            System.out.println("Host " + disconnectedUser + " disconnected. Closing room " + room.getId());
            
            String guest = room.getGuestUsername();
            if (guest != null) {
                notifyUser(guest, "The host disconnected. The room has been closed.");
            }
            
            roomManager.removeRoom(room.getId());
            
            sessionManager.broadcastToAll(roomManager.getAllRooms());
        } else {
            // Case B: Guest Disconnected -> Kick Guest, Notify Host
            System.out.println("Guest " + disconnectedUser + " disconnected from room " + room.getId());
            
            room.setGuestUsername(null); // Open the slot again
            room.setGameStarted(false);  // Stop game if running
            
            notifyUser(room.getHostUsername(), "The opponent disconnected.");
            
            // Notify host to update their UI (Room View needs to show empty slot)
            try {
                ClientCallback hostCallback = sessionManager.getCallback(room.getHostUsername());
                if (hostCallback != null) {
                    hostCallback.onRoomInfoUpdate(room);
                }
            } catch (RemoteException e) {
                // Host might be gone too? Next heartbeat will catch them.
            }
        }
    }
    
    private void notifyUser(String username, String message) {
        ClientCallback callback = sessionManager.getCallback(username);
        if (callback != null) {
            try {
                callback.onKicked(message);
            } catch (RemoteException e) {
                // If we can't notify them, they are probably dead too.
                // Do nothing, let the next heartbeat loop clean them up.
            }
        }
    }
}
