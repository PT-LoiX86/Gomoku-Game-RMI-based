package com.caro.server.bot;

import com.caro.common.util.GameConstants;
import java.util.HashMap;
import java.util.Map;

public class BotEngine {

    private static final Map<Integer, Integer> ATTACK_SCORES = new HashMap<>();
    private static final Map<Integer, Integer> DEFEND_SCORES = new HashMap<>();

    static {
        ATTACK_SCORES.put(5, 100000);
        ATTACK_SCORES.put(4, 10000);
        ATTACK_SCORES.put(3, 1000);
        ATTACK_SCORES.put(2, 100);
        ATTACK_SCORES.put(1, 10);

        DEFEND_SCORES.put(5, 90000);
        DEFEND_SCORES.put(4, 80000);
        DEFEND_SCORES.put(3, 5000);
        DEFEND_SCORES.put(2, 500);
        DEFEND_SCORES.put(1, 50);
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {0, 1}, {1, 1}, {1, -1}
    };

    public static int[] getBestMove(int[][] board, int botVal, int humanVal) {
        int size = board.length;
        long bestScore = -1;
        int[] bestMove = null;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == GameConstants.CELL_EMPTY) {
                    
                    long attackScore = calculateDirectionScore(board, r, c, botVal, true);
                    long defendScore = calculateDirectionScore(board, r, c, humanVal, false);
                    
                    // Weight defense slightly higher to block human threats
                    long totalScore = attackScore + (long)(defendScore * 1.2);

                    if (totalScore > bestScore) {
                        bestScore = totalScore;
                        bestMove = new int[]{r, c};
                    }
                }
            }
        }

        // If board is empty, play center
        if (bestMove == null) {
            return new int[]{size / 2, size / 2};
        }
        return bestMove;
    }

    private static long calculateDirectionScore(int[][] board, int r, int c, int symbol, boolean isAttack) {
        int size = board.length;
        long totalScore = 0;

        for (int[] dir : DIRECTIONS) {
            int dx = dir[0];
            int dy = dir[1];
            
            int count = 1; 
            int blocked = 0;

            // Forward scan
            for (int i = 1; i <= 4; i++) {
                int nr = r + dx * i;
                int nc = c + dy * i;
                
                if (isValid(nr, nc, size)) {
                    if (board[nr][nc] == symbol) {
                        count++;
                    } else if (board[nr][nc] != GameConstants.CELL_EMPTY) {
                        blocked++;
                        break;
                    } else {
                        break;
                    }
                } else {
                    blocked++;
                    break;
                }
            }

            // Backward scan
            for (int i = 1; i <= 4; i++) {
                int nr = r - dx * i;
                int nc = c - dy * i;
                
                if (isValid(nr, nc, size)) {
                    if (board[nr][nc] == symbol) {
                        count++;
                    } else if (board[nr][nc] != GameConstants.CELL_EMPTY) {
                        blocked++;
                        break;
                    } else {
                        break;
                    }
                } else {
                    blocked++;
                    break;
                }
            }

            // Only score if not blocked on both ends
            if (blocked < 2) {
                int k = Math.min(count, 5);
                if (isAttack) {
                    totalScore += ATTACK_SCORES.getOrDefault(k, 0);
                } else {
                    totalScore += DEFEND_SCORES.getOrDefault(k, 0);
                }
            }
        }
        return totalScore;
    }

    private static boolean isValid(int r, int c, int size) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }
}
