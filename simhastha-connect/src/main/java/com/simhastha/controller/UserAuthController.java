package com.simhastha.controller;

import com.simhastha.service.AuthService;

import java.util.concurrent.CompletableFuture;

public final class UserAuthController {

    public CompletableFuture<AuthService.AuthOutcome> login(String userId, String password) {
        return AuthService.login(userId, password, "any");
    }

    public CompletableFuture<AuthService.AuthOutcome> registerUser(
            String name, String mobile, String email, String password) {
        return AuthService.registerUser(name, mobile, email, password);
    }

    public CompletableFuture<String> resetPassword(String email) {
        return AuthService.resetPassword(email);
    }

    public CompletableFuture<AuthService.AuthOutcome> loginWithGoogleIdToken(String googleIdToken) {
        return AuthService.loginWithGoogleIdToken(googleIdToken);
    }
}
