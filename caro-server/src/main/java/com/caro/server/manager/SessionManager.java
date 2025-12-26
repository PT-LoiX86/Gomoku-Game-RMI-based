package com.caro.server.manager;

import com.caro.common.service.ClientCallback;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static SessionManager instance;
    
    // Maps Username -> Callback Interface
    private final Map<String, ClientCallback> activeClients = new ConcurrentHashMap<>();
    // Maps Username -> Last Heartbeat Timestamp
    private final Map<String, Long> lastHeartbeats = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void registerUser(String username, ClientCallback callback) {
        activeClients.put(username, callback);
        lastHeartbeats.put(username, System.currentTimeMillis());
        System.out.println("User registered: " + username);
    }

    public void removeUser(String username) {
        activeClients.remove(username);
        lastHeartbeats.remove(username);
        System.out.println("User removed: " + username);
    }

    public ClientCallback getCallback(String username) {
        return activeClients.get(username);
    }

    public void updateHeartbeat(String username) {
        if (activeClients.containsKey(username)) {
            lastHeartbeats.put(username, System.currentTimeMillis());
        }
    }
    
    public Map<String, Long> getHeartbeats() {
        return lastHeartbeats;
    }

    public void broadcastToAll(List<com.caro.common.model.Room> rooms) {
        activeClients.forEach((username, callback) -> {
            try {
                callback.onLobbyUpdate(rooms);
                System.out.println("Sent lobby update to: " + username);
            } catch (RemoteException e) {
                System.err.println("Failed to update user " + username + ": " + e.getMessage());
                // No need to remove them here aggressively
            }
        });
    }

        public List<String> getActiveUsers() {
        return new ArrayList<>(activeClients.keySet());
    }
    
    public void broadcastToAllUsers(List<String> users) {
        activeClients.forEach((name, cb) -> {
            try { cb.onUserListUpdate(users); } 
            catch (Exception e) {}
        });
    }

}
