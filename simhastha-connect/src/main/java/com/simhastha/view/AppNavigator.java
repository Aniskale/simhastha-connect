package com.simhastha.view;

import java.util.function.BiConsumer;

import javafx.stage.Stage;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void openDashboardFor(Stage stage, AppSession.User user, BiConsumer<String, String> showMessage) {
        if (user == null) {
            System.err.println("AUTH_DIAGNOSTIC stage=NAVIGATION result=failed missingSessionUser=true");
            showMessage.accept("Login Failed", "Your authenticated session could not be opened.");
            return;
        }
        System.out.println("AUTH_DIAGNOSTIC stage=NAVIGATION result=started role=" + user.role());
        switch (user.role()) {
            case "admin" -> {
                NavigationUtil.navigate(stage, new AdminDashboardPage().createScene(stage));
                System.out.println("AUTH_DIAGNOSTIC stage=NAVIGATION result=success destination=adminDashboard");
            }
            case "business" -> NavigationUtil.navigate(stage, new BusinessOwnerDashboardPage(
                    new BusinessAuthPage.BusinessAccount(user.displayName(), user.displayName(), "Business",
                            "", user.email(), "", "")).createScene(stage));
            case "transport_operator" -> NavigationUtil.navigate(stage, new OperatorDashboardPage(
                    new OperatorAuthPage.OperatorAccount(user.displayName(), user.displayName(), "",
                            user.email(), "Transport", "")).createScene(stage));
            case "user" -> {
                NavigationUtil.navigate(stage, new DashboardPage().createScene(stage));
                System.out.println("AUTH_DIAGNOSTIC stage=NAVIGATION result=success destination=userDashboard");
            }
            default -> showMessage.accept("Login Failed", "Your account role is not configured. Please contact the administrator.");
        }
    }
}
