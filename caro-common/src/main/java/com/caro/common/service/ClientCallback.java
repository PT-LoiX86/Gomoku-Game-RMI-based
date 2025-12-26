package com.caro.common.service;

import com.caro.common.model.ChatMessage;
import com.caro.common.model.GameState;
import com.caro.common.model.Room;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallback extends Remote {
    
    // Updates the Lobby list (called when any room changes)
    void onLobbyUpdate(List<Room> rooms) throws RemoteException;

    // Update the user list (called when players login or logout)
    void onUserListUpdate(List<String> users) throws RemoteException;
    
    // Updates the Room View (called when player joins/leaves)
    void onRoomInfoUpdate(Room room) throws RemoteException;
    
    // Updates the Game Board (called after a move)
    void onGameStateUpdate(GameState state) throws RemoteException;
    
    // Receive a chat message
    void onChatMessageReceived(ChatMessage message) throws RemoteException;
    
    // Called when the game ends (Win/Loss/Draw)
    void onGameEnded(String winnerUsername) throws RemoteException;
    
    // Called if server kicks you (timeout or host kicked)
    void onKicked(String reason) throws RemoteException;
    
    // Simple ping to check if client is alive
    void ping() throws RemoteException;
}
