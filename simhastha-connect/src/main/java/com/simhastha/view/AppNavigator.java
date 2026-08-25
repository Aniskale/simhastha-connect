package com.simhastha.view;

import java.util.function.BiConsumer;

import javafx.stage.Stage;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void openDashboardFor(Stage stage, AppSession.User user, BiConsumer<String, String> showMessage) {
        switch (user.role()) {
            case "admin" -> stage.setScene(new AdminDashboardPage().createScene(stage));
            case "business" -> stage.setScene(new BusinessOwnerDashboardPage(
                    new BusinessAuthPage.BusinessAccount(user.displayName(), user.displayName(), "Business",
                            "", user.email(), "", "")).createScene(stage));
            case "transport_operator" -> stage.setScene(new OperatorDashboardPage(
                    new OperatorAuthPage.OperatorAccount(user.displayName(), user.displayName(), "",
                            user.email(), "Transport", "")).createScene(stage));
            case "user" -> stage.setScene(new DashboardPage().createScene(stage));
            default -> showMessage.accept("Login Failed", "Account role is not valid.");
        }
    }
}
