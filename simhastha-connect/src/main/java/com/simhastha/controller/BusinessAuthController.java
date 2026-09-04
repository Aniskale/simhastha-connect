package com.simhastha.controller;

import com.simhastha.service.AuthService;
import com.simhastha.view.BusinessAuthPage;

import java.util.concurrent.CompletableFuture;

public final class BusinessAuthController {

    public boolean isFirebaseEnabled() {
        return AuthService.isFirebaseEnabled();
    }

    public CompletableFuture<AuthService.AuthOutcome> login(String userId, String password) {
        return AuthService.login(userId, password, "any");
    }

    public CompletableFuture<AuthService.AuthOutcome> registerBusiness(
            BusinessAuthPage.BusinessAccount account, String password) {
        return AuthService.registerBusiness(account, password);
    }

    public CompletableFuture<String> resetPassword(String email) {
        return AuthService.resetPassword(email);
    }
}
