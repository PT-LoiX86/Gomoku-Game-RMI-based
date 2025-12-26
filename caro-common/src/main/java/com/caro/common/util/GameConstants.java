package com.caro.common.util;

public class GameConstants {
    public static final String RMI_ID = "CaroGameService";
    public static final int RMI_PORT = 1099;
    
    public static final int CELL_EMPTY = 0;
    public static final int CELL_X = 1; // Host usually
    public static final int CELL_O = 2; // Guest usually
    
    public static final int WIN_STREAK = 5; // The rule is 5 in a row
    public static final int TURN_TIMEOUT_SECONDS = 10;
}
