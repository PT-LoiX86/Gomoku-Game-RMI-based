package com.caro.common.service;

import com.caro.common.model.GameSettings;
import com.caro.common.model.Room;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameService extends Remote {
    
    // Auth
    // Returns true if login success, throws exception or false if fail
    boolean login(String username, ClientCallback callback) throws RemoteException;
    void logout(String username) throws RemoteException;
    void sendHeartbeat(String username) throws RemoteException;

    List<String> getOnlineUsers() throws RemoteException;

    // Room Management
    List<Room> getAllRooms() throws RemoteException;
    void updateRoomSettings(String username, String roomId, GameSettings settings) throws RemoteException;
    void createRoom(String username, GameSettings settings) throws RemoteException;
    void joinRoom(String username, String roomId) throws RemoteException;
    void leaveRoom(String username, String roomId) throws RemoteException;
    void kickPlayer(String hostUsername, String roomId, String playerToKick) throws RemoteException;
    
    // PvE
    void addBot(String hostUsername, String roomId) throws RemoteException;

    // Game Logic
    void startGame(String hostUsername, String roomId) throws RemoteException;
    void placeMove(String username, String roomId, int row, int col) throws RemoteException;
    
    // Chat
    void sendChat(String username, String roomId, String message) throws RemoteException;
}
