package com.simhastha.view;

public final class AppSession {

    private static User currentUser;

    private AppSession() {
    }

    public static void set(User user) {
        currentUser = user;
    }

    public static User currentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }

    public record User(String uid, String email, String role, String idToken, String displayName, String status) {
        public boolean isAdmin() {
            return "admin".equals(role);
        }
    }
}
