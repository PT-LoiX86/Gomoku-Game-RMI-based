package com.caro.common.model;

import java.io.Serializable;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    // 0 = Empty, 1 = Host (X), 2 = Guest (O)
    private int[][] board; 
    private String currentTurnUsername;
    private String winnerUsername; // Null if game ongoing
    private boolean isDraw;
    private int currentRound;

    public GameState(int boardSize, String firstTurnUsername) {
        this.board = new int[boardSize][boardSize];
        this.currentTurnUsername = firstTurnUsername;
        this.currentRound = 1;
    }

    // Getters and Setters
    public int[][] getBoard() { return board; }
    public String getCurrentTurnUsername() { return currentTurnUsername; }
    public void setCurrentTurnUsername(String username) { this.currentTurnUsername = username; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winner) { this.winnerUsername = winner; }
    public boolean isDraw() { return isDraw; }
    public void setDraw(boolean draw) { isDraw = draw; }
}
