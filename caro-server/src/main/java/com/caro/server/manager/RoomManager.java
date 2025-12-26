package com.caro.server.manager;

import com.caro.common.model.Room;
import com.caro.common.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoomManager {
    private static RoomManager instance;
    
    // Maps RoomID -> Room Object
    private final Map<String, Room> activeRooms = new ConcurrentHashMap<>();

    private RoomManager() {}

    public static synchronized RoomManager getInstance() {
        if (instance == null) instance = new RoomManager();
        return instance;
    }

    public void addRoom(Room room) {
        activeRooms.put(room.getId(), room);
    }

    public void removeRoom(String roomId) {
        activeRooms.remove(roomId);
    }

    public Room getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(activeRooms.values());
    }
    
    // Helper to find which room a user is currently in (useful for disconnect logic)
    public Room getRoomByUsername(String username) {
        for (Room r : activeRooms.values()) {
            if (username.equals(r.getHostUsername()) || username.equals(r.getGuestUsername())) {
                return r;
            }
        }
        return null;
    }
}
