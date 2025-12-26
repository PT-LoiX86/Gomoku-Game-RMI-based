package com.caro.client.controller;

import com.caro.client.app.ViewManager;
import com.caro.client.rmi.RmiClientManager;
import com.caro.common.model.ChatMessage;
import com.caro.common.model.GameSettings;
import com.caro.common.model.GameState;
import com.caro.common.model.Room;
import com.caro.common.util.GameConstants;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.rmi.RemoteException;

public class RoomController {

    @FXML private Label roomNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label hostLabel;
    @FXML private Label guestLabel;
    @FXML private Button addBotButton;
    @FXML private Button startGameButton;
    
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    
    @FXML private VBox settingsPanel;
    @FXML private VBox gamePanel;
    @FXML private GridPane gameBoardGrid;
    
    @FXML private Spinner<Integer> boardSizeSpinner;
    @FXML private Spinner<Integer> roundsSpinner;
    @FXML private TextField timePerTurnField;
    @FXML private Label waitingLabel;
    @FXML private GridPane settingsGrid;
    @FXML private Label settingsLabel;
    
    @FXML private Label roundLabel;
    @FXML private Label player1Label;
    @FXML private Label player2Label;
    @FXML private Label score1Label;
    @FXML private Label score2Label;
    @FXML private Label timerLabel;
    @FXML private Button kickButton;

    private Room currentRoom;
    private Button[][] boardButtons;
    private String myUsername;
    private int currentBoardSize = -1;
    private Timeline turnTimer;
    private int secondsLeft;

    @FXML
    public void initialize() {
        timePerTurnField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { // Only digits allowed
                return change;
            }
            return null; // Reject change
        }));
    }

    public void setRoom(Room room) {
        this.currentRoom = room;
        this.myUsername = RmiClientManager.getInstance().getUsername();
        updateRoomInfo(room);
    }

    public void updateRoomInfo(Room room) {
        Platform.runLater(() -> {
            this.currentRoom = room;
            roomNameLabel.setText(room.getName() + " (" + room.getHostUsername() + ")");
            
            roundLabel.setText("Round: " + room.getCurrentRound() + " / " + room.getSettings().getTotalRounds());

            player1Label.setText("Host: " + room.getHostUsername());
            hostLabel.setText("Host: " + room.getHostUsername());
            score1Label.setText("Score: " + room.getHostScore());

            if (room.getGuestUsername() != null) {
                statusLabel.setText("Lobby Full");
                player2Label.setText(room.getGuestUsername());
                guestLabel.setText(room.getGuestUsername());
                score2Label.setText("Score: " + room.getGuestScore());
            } else {
                player2Label.setText("Waiting...");
                guestLabel.setText("Waiting...");
                score2Label.setText("-");
            }

            boolean isHost = myUsername.equals(room.getHostUsername());
            boolean isGameStarted = room.isGameStarted();
            boolean isGuestPlayer = (room.getGuestUsername() != null && room.getGuestUsername() != "BOT");

            settingsPanel.setVisible(!isGameStarted);
            gamePanel.setVisible(isGameStarted);

            if (!isGameStarted) {

                settingsLabel.setVisible(isHost);
                settingsLabel.setManaged(isHost); 

                settingsGrid.setVisible(isHost);
                settingsGrid.setManaged(isHost); 
                
                startGameButton.setVisible(isHost);
                startGameButton.setManaged(isHost);
        
                addBotButton.setVisible(isHost && !isGuestPlayer);
                addBotButton.setManaged(isHost);

                waitingLabel.setVisible(!isHost);

                kickButton.setVisible(isHost && room.getGuestUsername() != null && !room.isGameStarted());

                roundLabel.setText("Match Setup");

                score1Label.setVisible(false);
                score2Label.setVisible(false);
            } else {
                roundLabel.setText("Round: " + room.getCurrentRound() + " / " + room.getSettings().getTotalRounds());
                score1Label.setVisible(true);
                score2Label.setVisible(true);
            }

            // 3. Game Started Logic
            if (isGameStarted) {
                 if (this.currentBoardSize != room.getSettings().getBoardSize() || gameBoardGrid.getChildren().isEmpty()) {
                      initBoard(room.getSettings().getBoardSize());
                      this.currentBoardSize = room.getSettings().getBoardSize();
                 }
            }

            if (!room.isGameStarted() && turnTimer != null) {
                 turnTimer.stop();
            }
        });
    }

    @FXML
    private void handleStartGame() {
        try {
            // 1. Read the final settings from the UI
            int size = boardSizeSpinner.getValue();
            int rounds = roundsSpinner.getValue();
            int time = 10;
            try {
                time = Integer.parseInt(timePerTurnField.getText());
            } catch (NumberFormatException e) {
                time = 10; // Default
            }

            GameSettings settings = new GameSettings(size, rounds, time);

            // 2. Send settings to server FIRST
            RmiClientManager.getInstance().getService().updateRoomSettings(myUsername, currentRoom.getId(), settings);
            
            // 3. THEN Start the game
            RmiClientManager.getInstance().getService().startGame(myUsername, currentRoom.getId());
            
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // --- Standard Board & Chat Logic ---

    private void initBoard(int size) {
        gameBoardGrid.getChildren().clear();
        boardButtons = new Button[size][size];
        gameBoardGrid.setAlignment(javafx.geometry.Pos.CENTER);
        
        double cellSize = 30.0; 

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Button btn = new Button();
                btn.setPrefSize(cellSize, cellSize);
                btn.setMinSize(cellSize, cellSize);
                btn.setStyle("-fx-font-size: 10px; -fx-padding: 0;"); 
                final int row = r;
                final int col = c;
                btn.setOnAction(e -> handleBoardClick(row, col));
                boardButtons[r][c] = btn;
                gameBoardGrid.add(btn, c, r);
            }
        }
    }

    private void handleBoardClick(int r, int c) {
        try {
            RmiClientManager.getInstance().getService().placeMove(myUsername, currentRoom.getId(), r, c);
        } catch (RemoteException e) { e.printStackTrace(); }
    }

    public void updateGameState(GameState state) {
        Platform.runLater(() -> {
            int[][] board = state.getBoard();

            System.out.println("Redrawing board. Size: " + board.length + ". Button Array Size: " + (boardButtons == null ? "null" : boardButtons.length));
            
            // 1. Update Turn Label
            boolean isMyTurn = myUsername.equals(state.getCurrentTurnUsername());
            if (state.getWinnerUsername() == null) { // Only update if game running
                if (isMyTurn) {
                    statusLabel.setText("YOUR TURN (" + (myUsername.equals(currentRoom.getHostUsername()) ? "X" : "O") + ")");
                    statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("Waiting for " + state.getCurrentTurnUsername());
                    statusLabel.setStyle("-fx-text-fill: black;");
                }
            }

            // 2. Render Board
            for (int r = 0; r < board.length; r++) {
                for (int c = 0; c < board[r].length; c++) {
                    int val = board[r][c];
                    // Safety check if board size changed unexpectedly
                    if (r < boardButtons.length && c < boardButtons[r].length) {
                        Button btn = boardButtons[r][c];
                        if (val == GameConstants.CELL_X) {
                            btn.setText("X");
                            btn.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
                        } else if (val == GameConstants.CELL_O) {
                            btn.setText("O");
                            btn.setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-font-size: 14px;");
                        } else {
                            btn.setText("");
                        }
                    }
                }
            }

            if (currentRoom != null) {
                startClientTimer(currentRoom.getSettings().getTimePerTurnSeconds());
            }
        });
    }

    public void addChatMessage(ChatMessage msg) {
        Platform.runLater(() -> {
            // Append and scroll
            chatArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    @FXML 
    private void handleSendChat() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        try {
            RmiClientManager.getInstance().getService().sendChat(myUsername, currentRoom.getId(), text);
            chatInput.clear();
        } catch (RemoteException e) { e.printStackTrace(); }
    }
    
    @FXML 
    private void handleAddBot() {
        try { RmiClientManager.getInstance().getService().addBot(myUsername, currentRoom.getId()); } 
        catch (RemoteException e) { e.printStackTrace(); }
    }
    
    @FXML 
    private void handleLeaveRoom() {
        try { 
            RmiClientManager.getInstance().getService().leaveRoom(myUsername, currentRoom.getId());
            ViewManager.getInstance().showLobby();
        } catch (RemoteException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleKickGuest() {
        if (currentRoom.getGuestUsername() == null) return;
        try {
            RmiClientManager.getInstance().getService().kickPlayer(
                myUsername, 
                currentRoom.getId(), 
                currentRoom.getGuestUsername() // Target
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startClientTimer(int durationSeconds) {
        // Stop existing
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        secondsLeft = durationSeconds;
        timerLabel.setText(secondsLeft + "s");
        
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            timerLabel.setText(secondsLeft + "s");
            if (secondsLeft <= 0) {
                turnTimer.stop();
            }
        }));
        
        turnTimer.setCycleCount(Timeline.INDEFINITE);
        turnTimer.play();
    }

    public void onGameEnded(String message) {
        Platform.runLater(() -> {
            if (turnTimer != null) {
                turnTimer.stop();
            }
            timerLabel.setText("Ended");
            
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Game Over");
                        alert.setHeaderText(null);
                        alert.setContentText(message);
                        alert.show();
                    });
                }
            }, 300);
        });
    }

}
