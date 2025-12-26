package com.caro.client.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    private static ViewManager instance;
    private Stage primaryStage;
    
    // Store controllers so CallbackImpl can access them
    private final Map<String, Object> controllers = new HashMap<>();

    private ViewManager() {}

    public static synchronized ViewManager getInstance() {
        if (instance == null) instance = new ViewManager();
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void showLogin() {
        switchScene("LoginView.fxml", "Login", "LOGIN_CONTROLLER");
    }

    public void showLobby() {
        switchScene("LobbyView.fxml", "Lobby", "LOBBY_CONTROLLER");
    }

    public void showRoom() {
        switchScene("RoomView.fxml", "Room", "ROOM_CONTROLLER");
    }

    private void switchScene(String fxmlFile, String title, String controllerKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            
            // Save the controller so we can update it later
            controllers.put(controllerKey, loader.getController());
            
            primaryStage.setTitle("Caro Game - " + title);
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Object getController(String key) {
        return controllers.get(key);
    }
}
