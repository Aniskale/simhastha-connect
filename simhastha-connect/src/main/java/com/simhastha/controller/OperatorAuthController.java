package com.simhastha.controller;

import com.simhastha.service.AuthService;
import com.simhastha.view.OperatorAuthPage;

import java.util.concurrent.CompletableFuture;

public final class OperatorAuthController {

    public boolean isFirebaseEnabled() {
        return AuthService.isFirebaseEnabled();
    }

    public CompletableFuture<AuthService.AuthOutcome> login(String userId, String password) {
        return AuthService.login(userId, password, "any");
    }

    public CompletableFuture<AuthService.AuthOutcome> registerOperator(
            OperatorAuthPage.OperatorAccount account, String password) {
        return AuthService.registerOperator(account, password);
    }

    public CompletableFuture<String> resetPassword(String email) {
        return AuthService.resetPassword(email);
    }
}
