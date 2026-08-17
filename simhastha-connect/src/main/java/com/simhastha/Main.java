package com.simhastha;

import com.simhastha.view.WelcomePage;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        WelcomePage welcomePage = new WelcomePage();
        Scene scene = welcomePage.createScene(stage);

        stage.setTitle("SIMHASTHA CONNECT | Nashik Simhastha 2027");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
