package com.caro.client.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        ViewManager.getInstance().setPrimaryStage(primaryStage);
        ViewManager.getInstance().showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
