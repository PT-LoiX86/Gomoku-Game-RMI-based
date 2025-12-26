package com.caro.client.controller;

import com.caro.client.app.ViewManager;
import com.caro.client.rmi.RmiClientManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField serverAddressField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String address = serverAddressField.getText().trim();

        if (username.isEmpty()) {
            statusLabel.setText("Username cannot be empty.");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Connecting...");

        new Thread(() -> {
            RmiClientManager rmiManager = RmiClientManager.getInstance();
            
            // 1. Connect to Registry
            if (!rmiManager.connect(address)) {
                updateStatus("Failed to connect to server.", false);
                return;
            }
            
            // 2. Login
            if (rmiManager.login(username)) {
                updateStatus("Login Success!", true);
                // Switch scene on UI thread
                javafx.application.Platform.runLater(() -> {
                    ViewManager.getInstance().showLobby();
                });
            } else {
                updateStatus("Username taken or invalid.", false);
            }
        }).start();
    }

    private void updateStatus(String msg, boolean success) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            loginButton.setDisable(false);
        });
    }
}
