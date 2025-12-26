package com.caro.client.controller;

import com.caro.client.app.ViewManager;
import com.caro.client.rmi.RmiClientManager;
import com.caro.common.model.GameSettings;
import com.caro.common.model.Room;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.rmi.RemoteException;
import java.util.List;

public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<Room> roomListView;
    @FXML private ListView<String> onlineUserList;

    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final ObservableList<Room> rooms = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        String username = RmiClientManager.getInstance().getUsername();
        welcomeLabel.setText("Welcome to Caro, " + username + "!");
        
        roomListView.setItems(rooms);
        onlineUserList.setItems(onlineUsers);
        
        // Custom Cell Factory to display room details nicely
        roomListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String status = (item.getGuestUsername() == null) ? "Open (1/2)" : "Full (2/2)";
                    setText(item.getName() + " | " + item.getHostUsername() + "'s Room [" + status + "]");
                    
                    setStyle("-fx-text-fill: black;"); 
                    // Double click to join
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && item.getGuestUsername() == null) {
                            handleJoinRoom(item);
                        }
                    });
                }
            }
        });
        refreshRoomList();
        refreshOnlineUsers();
    }

    // Called by ClientCallbackImpl via ViewManager
    public void updateRoomList(List<Room> newRooms) {
        Platform.runLater(() -> {
            System.out.println("LobbyController received rooms: " + newRooms.size());
            rooms.clear();
            rooms.addAll(newRooms);
            roomListView.refresh();
        });
    }

    @FXML
    private void handleCreateRoom() {
        try {
            String user = RmiClientManager.getInstance().getUsername();
            // Default settings for now
            GameSettings settings = new GameSettings(10, 5, 10); 
            
            RmiClientManager.getInstance().getService().createRoom(user, settings);
            
            // Upon success, the server will call our 'onRoomInfoUpdate' or we assume success.
            // Ideally, we wait for a callback confirmation or specific event to switch view.
            // For MVP simplicity, let's switch to RoomView immediately? 
            // Better: Wait for the 'onRoomInfoUpdate' callback to switch everyone.
             ViewManager.getInstance().showRoom();
             
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    private void handleJoinRoom(Room room) {
         try {
            String user = RmiClientManager.getInstance().getUsername();
            RmiClientManager.getInstance().getService().joinRoom(user, room.getId());
        } catch (RemoteException e) {
            e.printStackTrace();
            // Show alert "Room full"
        }
    }

    @FXML
    private void handleLogout() {
        try {
            String user = RmiClientManager.getInstance().getUsername();
            RmiClientManager.getInstance().getService().logout(user);
            
            // Stop heartbeat!
            RmiClientManager.getInstance().stop(); // You need to implement stop() to kill the thread
            
            ViewManager.getInstance().showLogin();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateUserList(List<String> users) {
        Platform.runLater(() -> {
            String myName = RmiClientManager.getInstance().getUsername();
            
            onlineUsers.clear();
            for (String user : users) {
                // EXCLUDE SELF
                if (!user.equals(myName)) {
                    onlineUsers.add(user);
                }
            }
        });
    }

    private void refreshOnlineUsers() {
        new Thread(() -> {
            try {
                List<String> users = RmiClientManager.getInstance().getService().getOnlineUsers();
                updateUserList(users);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void refreshRoomList() {
        new Thread(() -> {
            try {
                // Manually ask server for the list
                List<Room> rooms = RmiClientManager.getInstance().getService().getAllRooms();
                
                // Update UI on JavaFX Thread
                Platform.runLater(() -> updateRoomList(rooms));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
