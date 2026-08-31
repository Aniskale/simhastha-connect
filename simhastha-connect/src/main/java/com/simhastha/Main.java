package com.simhastha;

import com.simhastha.view.IntroPage;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {                    

        IntroPage introPage = new IntroPage();
        Scene scene = introPage.createScene(stage);

        stage.setTitle("SIMHASTHA CONNECT | Nashik Simhastha 2027");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(620);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
