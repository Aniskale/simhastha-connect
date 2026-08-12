package com.simhastha;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        Label label = new Label("SIMHASTHA CONNECT");

        Scene scene = new Scene(label, 800, 500);

        stage.setTitle("Simhastha Connect");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}