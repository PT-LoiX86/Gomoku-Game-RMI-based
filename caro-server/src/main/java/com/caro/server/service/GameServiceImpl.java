package com.caro.server.service;

import com.caro.common.model.*;
import com.caro.common.service.ClientCallback;
import com.caro.common.service.GameService;
import com.caro.common.util.GameConstants;
import com.caro.common.util.GameRules;
import com.caro.server.bot.BotEngine;
import com.caro.server.manager.RoomManager;
import com.caro.server.manager.SessionManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class GameServiceImpl extends UnicastRemoteObject implements GameService {

    private final SessionManager sessionManager;
    private final RoomManager roomManager;

    private final ScheduledExecutorService gameScheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();

    public GameServiceImpl() throws RemoteException {
        super();
        this.sessionManager = SessionManager.getInstance();
        this.roomManager = RoomManager.getInstance();
    }

    @Override
    public boolean login(String username, ClientCallback callback) throws RemoteException {
        if (username == null || username.trim().isEmpty()) return false;
        
        // TODO: Add JSON check against users.json here
        // For now, we allow login if name not taken
        if (sessionManager.getCallback(username) != null) {
            throw new RemoteException("User already logged in.");
        }

        sessionManager.registerUser(username, callback);
        List<Room> rooms = roomManager.getAllRooms();
        System.out.println("User " + username + " logged in. Sending " + rooms.size() + " rooms.");
        // Push initial lobby state to the new user
        callback.onLobbyUpdate(roomManager.getAllRooms());
        broadcastLobbyUpdate();
        broadcastUserList();
        return true;
    }

    @Override
    public void createRoom(String username, GameSettings settings) throws RemoteException {
        String roomId = UUID.randomUUID().toString();
        String roomName = username + "'s room";
        
        Room room = new Room(roomId, roomName, username, settings);
        roomManager.addRoom(room);

        ClientCallback hostCallback = sessionManager.getCallback(username);
        if (hostCallback != null) {
            hostCallback.onRoomInfoUpdate(room); 
            // The client needs to handle this by switching scene if they are currently in Lobby
        }
        
        broadcastLobbyUpdate();
    }

    @Override
    public void joinRoom(String username, String roomId) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room == null) throw new RemoteException("Room not found.");
        
        if (room.getGuestUsername() != null) throw new RemoteException("Room is full.");
        
        room.setGuestUsername(username);
        
        // Notify the Host that someone joined
        ClientCallback hostCallback = sessionManager.getCallback(room.getHostUsername());
        if (hostCallback != null) {
            hostCallback.onRoomInfoUpdate(room);
        }
        
        ClientCallback guestCallback = sessionManager.getCallback(username);
        if (guestCallback != null) {
            // This will trigger 'onRoomInfoUpdate' on the guest client,
            // which sets the room data and refreshes the UI
            guestCallback.onRoomInfoUpdate(room);
        }
        
        broadcastLobbyUpdate(); // Update lobby for everyone else (room is now 2/2)
    }

    @Override
    public void updateRoomSettings(String username, String roomId, GameSettings settings) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room != null && room.getHostUsername().equals(username)) {
            // Update the server-side room object
            // Note: create a setter in Room.java if missing: room.setSettings(settings);
            // Or modify the existing object directly if mutable.
            room.getSettings().setBoardSize(settings.getBoardSize());
            room.getSettings().setTotalRounds(settings.getTotalRounds());
            room.getSettings().setTimePerTurnSeconds(settings.getTimePerTurnSeconds());
            
            // Notify everyone in the room
            ClientCallback host = sessionManager.getCallback(room.getHostUsername());
            ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
            
            if (host != null) host.onRoomInfoUpdate(room);
            if (guest != null) guest.onRoomInfoUpdate(room);
        }
    }


    @Override
    public void sendChat(String username, String roomId, String message) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room == null) return;
        
        ChatMessage msg = new ChatMessage(username, message);
        room.addMessage(msg);
        
        notifyRoom(room, msg);
    }

    // --- Helper Methods ---
    
    public void broadcastLobbyUpdate() {
        List<Room> rooms = roomManager.getAllRooms();
        System.out.println("Broadcasting update: " + rooms.size() + " rooms active.");

        // Use SessionManager to send to EVERYONE
        SessionManager.getInstance().broadcastToAll(rooms);
    }
    
    private void notifyRoom(Room room, ChatMessage msg) {
         try {
             ClientCallback host = sessionManager.getCallback(room.getHostUsername());
             ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
             
             if (host != null) host.onChatMessageReceived(msg);
             if (guest != null) guest.onChatMessageReceived(msg);
         } catch (RemoteException e) {
             e.printStackTrace();
         }
    }

    private void closeRoom(Room room) {
        // 1. Remove from Manager
        roomManager.removeRoom(room.getId());
        
        // 2. Kick Guest if present
        if (room.getGuestUsername() != null) {
            ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
            if (guest != null) {
                try {
                    // Send a specific message or just an empty room update?
                    // Better: Send a "Kicked" signal or force them to lobby.
                    guest.onKicked("Host closed the room.");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // 3. Broadcast to lobby (Room is gone)
        broadcastLobbyUpdate();
    }
    
    // Implement other methods (leaveRoom, placeMove, etc.) similarly...
    @Override
    public void logout(String username) throws RemoteException {
        sessionManager.removeUser(username);
        // Also handle if they were in a room?
        Room r = roomManager.getRoomByUsername(username);
        if (r != null) {
            leaveRoom(username, r.getId());
        }
        broadcastLobbyUpdate();
        broadcastUserList();
    }

    @Override
    public void sendHeartbeat(String username) throws RemoteException {
        sessionManager.updateHeartbeat(username);
    }

    @Override
        public void leaveRoom(String username, String roomId) throws RemoteException {
            Room room = roomManager.getRoom(roomId);
            if (room != null) {
                // Logic to remove player
                if (username.equals(room.getGuestUsername())) {
                    room.setGuestUsername(null);
                    room.setGameStarted(false); // Stop game if guest leaves
                    
                    // Notify Host that guest left
                    ClientCallback hostCallback = sessionManager.getCallback(room.getHostUsername());
                    if (hostCallback != null) hostCallback.onRoomInfoUpdate(room);
                } 
                else if (username.equals(room.getHostUsername())) {
                    // Logic for Host leaving (See Bug 2 below)
                    closeRoom(room); // We need a helper for this
                    return; // closeRoom handles broadcast
                }
            }
            
            // IMPORTANT: Broadcast the change to everyone (including the person who left)
            broadcastLobbyUpdate();
        }

    @Override
    public void kickPlayer(String hostUsername, String roomId, String playerToKick) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room == null) return;
        
        // Validation: Only Host can kick
        if (!room.getHostUsername().equals(hostUsername)) return;
        
        // Validation: Can only kick the current guest
        if (!playerToKick.equals(room.getGuestUsername())) return;
        
        // Case A: Kicking a Bot
        if (playerToKick.equals("BOT") && room.isBotMode()) {
            room.setGuestUsername(null);
            room.setBotMode(false);
            
            // Notify Host
            ClientCallback host = sessionManager.getCallback(hostUsername);
            if (host != null) host.onRoomInfoUpdate(room);
            
            broadcastLobbyUpdate();
            return;
        }
        
        // Case B: Kicking a Human
        ClientCallback guest = sessionManager.getCallback(playerToKick);
        
        // 1. Remove from room
        room.setGuestUsername(null);
        room.setGameStarted(false);
        room.resetMatch(); // Reset scores if kicked
        
        // 2. Notify Guest (You are kicked!)
        if (guest != null) {
            try {
                guest.onKicked("You have been kicked by the host.");
            } catch (RemoteException e) {
                // ignore
            }
        }
        
        // 3. Notify Host (Room is empty now)
        ClientCallback host = sessionManager.getCallback(hostUsername);
        if (host != null) host.onRoomInfoUpdate(room);
        
        // 4. Update Lobby
        broadcastLobbyUpdate();
    }
    
    @Override
    public void addBot(String hostUsername, String roomId) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room != null && room.getHostUsername().equals(hostUsername)) {
            if (room.getGuestUsername() == null) {
                room.setGuestUsername("BOT");
                room.setBotMode(true);
                
                // Notify Host that "BOT" joined
                ClientCallback hostCallback = sessionManager.getCallback(hostUsername);
                if (hostCallback != null) hostCallback.onRoomInfoUpdate(room);
                
                broadcastLobbyUpdate();
            }
        }
    }
    
    @Override
    public void startGame(String username, String roomId) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room == null || !room.getHostUsername().equals(username)) return;
        
        // Initialize Game
        room.setGameStarted(true);
        GameState state = new GameState(room.getSettings().getBoardSize(), room.getHostUsername());
        room.setGameState(state);

        startTurnTimer(room);
        
        // Notify change (this switches UI to Game Panel)
        notifyRoomUpdate(room); 
        
        // Broadcast initial empty board
        broadcastGameState(room);
    }

    @Override
    public void placeMove(String username, String roomId, int row, int col) throws RemoteException {
        Room room = roomManager.getRoom(roomId);
        if (room == null || !room.isGameStarted()) return;
        
        GameState state = room.getGameState();
        
        // Validation
        if (!state.getCurrentTurnUsername().equals(username)) return;
        if (state.getBoard()[row][col] != GameConstants.CELL_EMPTY) return;

        ScheduledFuture<?> timer = roomTimers.get(roomId);
        if (timer != null) timer.cancel(false);
        
        // Execute Move
        int playerVal = username.equals(room.getHostUsername()) ? GameConstants.CELL_X : GameConstants.CELL_O;
        state.getBoard()[row][col] = playerVal;
        broadcastGameState(room);
        
        // Check Win
        if (GameRules.checkWin(state.getBoard(), row, col, playerVal)) {
            if (username.equals(room.getHostUsername())) {
                room.setHostScore(room.getHostScore() + 1);
            } else {
                room.setGuestScore(room.getGuestScore() + 1);
            }
            roomTimers.remove(roomId);
            
            handleRoundEnd(room, username);
        } else if (GameRules.isFull(state.getBoard())) {
            roomTimers.remove(roomId);
            handleRoundEnd(room, "DRAW");
        } else {
            // Next Turn
            String nextPlayer = username.equals(room.getHostUsername()) ? room.getGuestUsername() : room.getHostUsername();
            state.setCurrentTurnUsername(nextPlayer);
            startTurnTimer(room);
            //broadcastGameState(room);

            if (room.isBotMode() && nextPlayer.equals("BOT")) {
                triggerBotMove(room);
            }
        }
    }
    
    // Helper to send "onRoomInfoUpdate" to both players
    private void notifyRoomUpdate(Room room) {
        try {
            ClientCallback host = sessionManager.getCallback(room.getHostUsername());
            ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
            if (host != null) host.onRoomInfoUpdate(room);
            if (guest != null) guest.onRoomInfoUpdate(room);
        } catch (RemoteException e) { e.printStackTrace(); }
    }

    private void broadcastGameState(Room room) {
        try {
            ClientCallback host = sessionManager.getCallback(room.getHostUsername());
            ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
            if (host != null) host.onGameStateUpdate(room.getGameState());
            if (guest != null) guest.onGameStateUpdate(room.getGameState());
        } catch (RemoteException e) { e.printStackTrace(); }
    }
    
    private void notifyGameEnded(Room room, String winner) {
        try {
            ClientCallback host = sessionManager.getCallback(room.getHostUsername());
            ClientCallback guest = sessionManager.getCallback(room.getGuestUsername());
            if (host != null) host.onGameEnded(winner);
            if (guest != null) guest.onGameEnded(winner);
        } catch (RemoteException e) { e.printStackTrace(); }
    }

    private void handleRoundEnd(Room room, String roundWinner) {
        int totalRounds = room.getSettings().getTotalRounds();
        String msg;
        
        if (room.getCurrentRound() < totalRounds) {
            msg = "Round " + room.getCurrentRound() + " Over! Winner: " + roundWinner + "\nNext round starts in 5 seconds...";
        } else {
            int hostScore = room.getHostScore();
            int guestScore = room.getGuestScore();
            
            String matchWinner;
            if (hostScore > guestScore) matchWinner = room.getHostUsername();
            else if (guestScore > hostScore) matchWinner = room.getGuestUsername();
            else matchWinner = "DRAW";
            
            msg = "MATCH OVER! Final Winner: " + matchWinner + "\nReturning to lobby in 5 seconds...";
        }
        notifyGameEnded(room, msg); 

        gameScheduler.schedule(() -> {
            try {
                // Re-fetch room to ensure it still exists and hasn't been closed
                Room currentRoom = roomManager.getRoom(room.getId());
                if (currentRoom == null) return;

                if (currentRoom.getCurrentRound() < totalRounds) {
                    
                    // 1. Increment Round
                    currentRoom.setCurrentRound(currentRoom.getCurrentRound() + 1);
                    
                    // 2. Reset Board
                    // Logic: Round 1=Host starts, Round 2=Guest starts, etc.
                    String startPlayer = (currentRoom.getCurrentRound() % 2 == 1) 
                                         ? currentRoom.getHostUsername() 
                                         : currentRoom.getGuestUsername();
                                         
                    GameState newState = new GameState(currentRoom.getSettings().getBoardSize(), startPlayer);
                    currentRoom.setGameState(newState);

                    // 3. Update Room Info (Updates "Round 2/5" label)
                    notifyRoomUpdate(currentRoom); 
                    
                    // 4. Update Board (Clears the pieces)
                    broadcastGameState(currentRoom); 
                    
                    // 5. Start Timer
                    startTurnTimer(currentRoom);
                    
                    // 6. If Bot is starting next round, trigger it
                    if (currentRoom.isBotMode() && startPlayer.equals("BOT")) {
                        triggerBotMove(currentRoom);
                    }
                    
                } else {
                    // --- END MATCH ---
                    
                    // 1. Clear Timers
                    roomTimers.remove(currentRoom.getId());
                    
                    // 2. Reset Room Data
                    currentRoom.resetMatch(); // Sets isGameStarted = false, scores = 0
                    
                    // 3. Update UI (This switches the View back to Room Setup/Lobby)
                    notifyRoomUpdate(currentRoom); 
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void handleTimeout(String roomId, String expectedPlayer) {
        try {
            Room room = roomManager.getRoom(roomId);
            if (room == null || !room.isGameStarted()) return;
            
            // Check race condition
            if (!room.getGameState().getCurrentTurnUsername().equals(expectedPlayer)) return;
            
            System.out.println("Timeout! Skipping " + expectedPlayer);
            
            // Switch Turn
            String nextPlayer = expectedPlayer.equals(room.getHostUsername()) ? room.getGuestUsername() : room.getHostUsername();
            room.getGameState().setCurrentTurnUsername(nextPlayer);
            
            // Notify clients
            broadcastGameState(room);
            
            // Loop: Start timer for next guy
            startTurnTimer(room);

            if (room.isBotMode() && nextPlayer.equals("BOT")) {
                triggerBotMove(room);
            }
            
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startTurnTimer(Room room) {
        // Cancel old
        ScheduledFuture<?> existing = roomTimers.get(room.getId());
        if (existing != null && !existing.isDone()) existing.cancel(false);
        
        int limit = room.getSettings().getTimePerTurnSeconds();
        if (limit <= 0) limit = 10;
        
        // Schedule Task
        Runnable task = () -> handleTimeout(room.getId(), room.getGameState().getCurrentTurnUsername());
        ScheduledFuture<?> future = gameScheduler.schedule(task, limit, TimeUnit.SECONDS);
        
        roomTimers.put(room.getId(), future);
    }

    private void triggerBotMove(Room room) {
        // Run in separate thread to simulate "thinking" and avoid blocking RMI
        gameScheduler.schedule(() -> {
            try {
                int[][] board = room.getGameState().getBoard();
                
                // Bot is Guest (O), Human is Host (X)
                int botVal = GameConstants.CELL_O;
                int humanVal = GameConstants.CELL_X;
                
                int[] move = BotEngine.getBestMove(board, botVal, humanVal);
                
                System.out.println("Bot moving to: " + move[0] + ", " + move[1]);
                
                // Recursively call placeMove for the Bot
                // Note: username must match what the state expects ("BOT")
                placeMove("BOT", room.getId(), move[0], move[1]);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1000, TimeUnit.MILLISECONDS); // 1s delay for realism
    }

    @Override
    public List<String> getOnlineUsers() throws RemoteException {
        return sessionManager.getActiveUsers();
    }

    @Override
    public List<Room> getAllRooms() throws RemoteException {
        // Return all rooms (RoomManager should already have a method for this)
        return roomManager.getAllRooms();
    }


    public void broadcastUserList() {
        // Get all usernames from SessionManager
        // You might need to add a public method to SessionManager to get this Set/List
        List<String> users = sessionManager.getActiveUsers(); 
        
        SessionManager.getInstance().broadcastToAllUsers(users);
    }
}
