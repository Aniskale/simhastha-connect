package com.simhastha.util;

import com.simhastha.view.AdminDashboardPage;
import com.simhastha.view.BusinessAuthPage;
import com.simhastha.view.BusinessOwnerDashboardPage;
import com.simhastha.view.DashboardPage;
import com.simhastha.view.OperatorAuthPage;
import com.simhastha.view.OperatorDashboardPage;

import java.util.function.BiConsumer;

import javafx.stage.Stage;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void openDashboardFor(Stage stage, AppSession.User user, BiConsumer<String, String> showMessage) {
        switch (user.role()) {
            case "admin" -> NavigationUtil.navigate(stage, new AdminDashboardPage().createScene(stage));
            case "business" -> NavigationUtil.navigate(stage, new BusinessOwnerDashboardPage(
                    new BusinessAuthPage.BusinessAccount(user.displayName(), user.displayName(), "Business",
                            "", user.email(), "", "")).createScene(stage));
            case "transport_operator" -> NavigationUtil.navigate(stage, new OperatorDashboardPage(
                    new OperatorAuthPage.OperatorAccount(user.displayName(), user.displayName(), "",
                            user.email(), "Transport", "")).createScene(stage));
            case "user" -> NavigationUtil.navigate(stage, new DashboardPage().createScene(stage));
            default -> showMessage.accept("Login Failed", "Account role is not valid.");
        }
    }
}
