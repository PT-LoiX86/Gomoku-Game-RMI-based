package com.caro.common.model;

import java.io.Serializable;

public class GameSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private int boardSize; // Default 5, Max 20
    private int totalRounds;
    private int timePerTurnSeconds; // For the 10s timeout rule

    public GameSettings(int boardSize, int totalRounds, int timePerTurnSeconds) {
        this.boardSize = boardSize;
        this.totalRounds = totalRounds;
        this.timePerTurnSeconds = timePerTurnSeconds;
    }
    
    // Getters and Setters
    public void setBoardSize(int boardSize) { this.boardSize = boardSize; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public void setTimePerTurnSeconds(int timePerTurnSeconds) { this.timePerTurnSeconds = timePerTurnSeconds; } 
    public int getBoardSize() { return boardSize; }
    public int getTotalRounds() { return totalRounds; }
    public int getTimePerTurnSeconds() { return timePerTurnSeconds; }
}
