package com.simhastha.controller;

import com.simhastha.service.AuthService;

import java.util.concurrent.CompletableFuture;

public final class AdminAuthController {

    public CompletableFuture<AuthService.AuthOutcome> login(String email, String password) {
        return AuthService.login(email, password, "admin");
    }
}
