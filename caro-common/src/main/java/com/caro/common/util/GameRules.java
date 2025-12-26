package com.caro.common.util;

public class GameRules {
    // Directions: Horizontal, Vertical, Diagonal \, Diagonal /
    private static final int[] dx = {1, 0, 1, 1};
    private static final int[] dy = {0, 1, 1, -1};

    public static boolean checkWin(int[][] board, int r, int c, int playerValue) {
        int size = board.length;
        for (int i = 0; i < 4; i++) {
            int count = 1; 
            // Look forward
            for (int step = 1; step < 5; step++) {
                int nr = r + step * dy[i];
                int nc = c + step * dx[i];
                if (isValid(nr, nc, size) && board[nr][nc] == playerValue) count++;
                else break;
            }
            // Look backward
            for (int step = 1; step < 5; step++) {
                int nr = r - step * dy[i];
                int nc = c - step * dx[i];
                if (isValid(nr, nc, size) && board[nr][nc] == playerValue) count++;
                else break;
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private static boolean isValid(int r, int c, int size) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }
    
    public static boolean isFull(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == GameConstants.CELL_EMPTY) return false;
            }
        }
        return true;
    }
}
