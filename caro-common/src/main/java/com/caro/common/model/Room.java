package com.caro.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name; // "username + 's room"
    private String hostUsername;
    private String guestUsername; // Null if empty
    private boolean isBotMode;    // True if playing vs Server
    private GameSettings settings;
    private boolean isGameStarted;
    private GameState gameState;
    private int currentRound = 1;
    private int hostScore = 0;
    private int guestScore = 0;
    
    // Requirement 6: Chat history belongs to the room
    private List<ChatMessage> chatHistory;

    public Room(String id, String name, String hostUsername, GameSettings settings) {
        this.id = id;
        this.name = name;
        this.hostUsername = hostUsername;
        this.settings = settings;
        this.chatHistory = new ArrayList<>();
        this.isBotMode = false;
        this.isGameStarted = false;
        this.gameState = new GameState(settings.getBoardSize() , hostUsername);
    }

    public void addMessage(ChatMessage msg) {
        this.chatHistory.add(msg);
    }

    // Boilerplate Getters/Setters omitted for brevity, but you need them!
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
    public void setSettings(GameSettings settings) { this.settings = settings; }
    public void setChatHistory(List<ChatMessage> chatHistory) { this.chatHistory = chatHistory; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getHostUsername() { return hostUsername; }
    public String getGuestUsername() { return guestUsername; }
    public void setGuestUsername(String guestUsername) { this.guestUsername = guestUsername; }
    public GameSettings getSettings() { return settings; }
    public boolean isBotMode() { return isBotMode; }
    public void setBotMode(boolean botMode) { isBotMode = botMode; }
    public boolean isGameStarted() { return isGameStarted; }
    public void setGameStarted(boolean gameStarted) { isGameStarted = gameStarted; }
    public List<ChatMessage> getChatHistory() { return chatHistory; }
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int r) { this.currentRound = r; }
    public int getHostScore() { return hostScore; }
    public void setHostScore(int s) { this.hostScore = s; }
    public int getGuestScore() { return guestScore; }
    public void setGuestScore(int s) { this.guestScore = s; }

    public void resetMatch() {
        this.currentRound = 1;
        this.hostScore = 0;
        this.guestScore = 0;
        this.isGameStarted = false;
    }
}
