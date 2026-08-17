package com.simhastha.view;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    public enum AppTheme {
        LIGHT,
        DARK
    }

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String THEME_KEY = "simhastha-theme";
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static AppTheme currentTheme = AppTheme.valueOf(PREFERENCES.get(THEME_KEY, AppTheme.LIGHT.name()));

    private ThemeManager() {
    }

    public static AppTheme getTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return currentTheme == AppTheme.DARK;
    }

    public static void setTheme(AppTheme theme) {
        if (theme == null || theme == currentTheme) {
            return;
        }

        currentTheme = theme;
        PREFERENCES.put(THEME_KEY, currentTheme.name());
        for (Runnable listener : List.copyOf(LISTENERS)) {
            listener.run();
        }
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void applyTo(Parent root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark", "welcome-root-light", "welcome-root-dark");
        root.getStyleClass().add(isDark() ? "theme-dark" : "theme-light");

        if (root.getStyleClass().contains("welcome-root")) {
            root.getStyleClass().add(isDark() ? "welcome-root-dark" : "welcome-root-light");
        }
    }

    public static void addTheme(Scene scene, Object owner) {
        URL cssUrl = owner.getClass().getResource("/css/simhastha-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        applyTo(scene.getRoot());
    }
}
