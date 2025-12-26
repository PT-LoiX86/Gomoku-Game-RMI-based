package com.caro.client.rmi;

import com.caro.common.model.ChatMessage;
import com.caro.common.model.GameState;
import com.caro.common.model.Room;
import com.caro.common.service.ClientCallback;
import com.caro.client.app.ViewManager;
import com.caro.client.controller.LobbyController; // We need a way to pass data to controllers
import com.caro.client.controller.RoomController;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.rmi.RemoteException;
import java.util.List;

public class ClientCallbackImpl implements ClientCallback {

    // We need a way to route these events to the active Controller.
    // A simple Observer pattern or static reference helper is needed here.
    // For this MVP structure, let's assume we have a 'ViewManager' or similar.
    
    // inside onLobbyUpdate
    @Override
    public void onLobbyUpdate(List<Room> rooms) throws RemoteException {
        Platform.runLater(() -> {
            LobbyController controller = (LobbyController) ViewManager.getInstance().getController("LOBBY_CONTROLLER");
            if (controller != null) {
                controller.updateRoomList(rooms);
            }
        });
    }

    @Override
    public void onRoomInfoUpdate(Room room) throws RemoteException {
        Platform.runLater(() -> {
            // Check if we are already in the room view?
            // If not, force switch to Room View            
            // If controller is null, or we aren't looking at it, switch scene
            // But ViewManager manages scenes.
            
            // SIMPLIFICATION: Always set the data.
            // ViewManager needs a method to "Ensure Room View is Visible"
            
            ViewManager.getInstance().showRoom(); // Switch scene
            RoomController rc = (RoomController) ViewManager.getInstance().getController("ROOM_CONTROLLER");     
            
            if (rc != null) {
                // 3. Pass the data
                rc.setRoom(room);
            } else {
                System.err.println("Error: RoomController is null!");
            }
        });
    }

    @Override
    public void onGameStateUpdate(GameState state) throws RemoteException {
        Platform.runLater(() -> {
            // DEBUG 1: Does this print?
            System.out.println("Game Move Received. Current Turn: " + state.getCurrentTurnUsername());
            
            RoomController controller = (RoomController) ViewManager.getInstance().getController("ROOM_CONTROLLER");
            
            // DEBUG 2: Is controller null?
            if (controller != null) {
                System.out.println("Controller found! calling updateGameState...");
                controller.updateGameState(state);
            } else {
                System.err.println("CRITICAL: RoomController is NULL in ViewManager!");
            }
        });
    }


    @Override
    public void onChatMessageReceived(ChatMessage message) throws RemoteException {
        Platform.runLater(() -> {
            RoomController controller = (RoomController) ViewManager.getInstance().getController("ROOM_CONTROLLER");
            if (controller != null) {
                // Ensure this method actually appends text
                controller.addChatMessage(message);
            }
            System.out.println("Chat: " + message.getSender() + ": " + message.getContent());
        });
    }

    @Override
    public void onGameEnded(String winner) throws RemoteException {
        Platform.runLater(() -> {
            RoomController controller = (RoomController) ViewManager.getInstance().getController("ROOM_CONTROLLER");

            if (controller != null) {
                controller.onGameEnded(winner);
            } else {
                showDelayedAlert(winner);
            }
        });
    }

    private void showDelayedAlert(String message) {
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
    }

    @Override
    public void onKicked(String reason) throws RemoteException {
        Platform.runLater(() -> {
            // Show Alert
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Room Closed");
            alert.setHeaderText(null);
            alert.setContentText(reason);
            alert.showAndWait();
            
            // Go back to Lobby
            ViewManager.getInstance().showLobby();
        });
    }

    @Override
    public void ping() throws RemoteException {
        // Just return, server checks connectivity
    }

    @Override
    public void onUserListUpdate(List<String> users) throws RemoteException {
        Platform.runLater(() -> {
            LobbyController controller = (LobbyController) ViewManager.getInstance().getController("LOBBY_CONTROLLER");
            if (controller != null) {
                controller.updateUserList(users);
            }
        });
    }

}
