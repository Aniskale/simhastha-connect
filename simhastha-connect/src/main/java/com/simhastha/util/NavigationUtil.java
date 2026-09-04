package com.simhastha.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public final class NavigationUtil {

    private NavigationUtil() {
    }

    public static void navigate(Stage stage, Scene nextScene) {
        if (stage == null || nextScene == null) {
            return;
        }

        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();

        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(nextScene);
        } else {
            for (String stylesheet : nextScene.getStylesheets()) {
                if (!currentScene.getStylesheets().contains(stylesheet)) {
                    currentScene.getStylesheets().add(stylesheet);
                }
            }

            Parent nextRoot = nextScene.getRoot();
            nextScene.setRoot(new Pane());
            currentScene.setRoot(nextRoot);
        }

        stage.setMaximized(wasMaximized);
        if (wasFullScreen) {
            stage.setFullScreen(true);
        }
        stage.show();
    }
}
