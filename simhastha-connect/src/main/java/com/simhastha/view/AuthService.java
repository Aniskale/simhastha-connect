package com.simhastha.view;

import java.util.concurrent.CompletableFuture;

public final class AuthService {

    private static final FirebaseConfig CONFIG = FirebaseConfig.load();
    private static final FirebaseAuthGateway AUTH = new FirebaseAuthGateway(CONFIG);
    private static final FirestoreGateway FIRESTORE = new FirestoreGateway(CONFIG);

    private AuthService() {
    }

    public static boolean isFirebaseEnabled() {
        return CONFIG.isEnabled();
    }

    public static CompletableFuture<AuthOutcome> login(String email, String password, String expectedRole) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.login(email, password);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                FirestoreGateway.UserProfile profile = FIRESTORE.loadUserProfile(auth.uid, auth.idToken);
                if (profile == null || profile.role().isBlank()) {
                    return AuthOutcome.failure("Your account profile is missing. Please contact admin.");
                }

                if (!profile.role().equals(expectedRole) && !"any".equals(expectedRole)) {
                    return AuthOutcome.failure("This account is registered as " + profile.role() + ".");
                }

                if ("pending".equals(profile.status())) {
                    return AuthOutcome.failure("Your account is pending admin approval.");
                }
                if ("disabled".equals(profile.status()) || "rejected".equals(profile.status())) {
                    return AuthOutcome.failure("This account is not active. Please contact admin.");
                }

                AppSession.User user = new AppSession.User(
                        auth.uid,
                        auth.email,
                        profile.role(),
                        auth.idToken,
                        profile.name(),
                        profile.status());
                AppSession.set(user);
                AppDataStore.refreshFirebaseData(auth.idToken);
                return AuthOutcome.success(user);
            } catch (Exception exception) {
                return AuthOutcome.failure("Unable to connect to Firebase. Check internet and Firebase rules.");
            }
        });
    }

    public static CompletableFuture<AuthOutcome> registerUser(String name, String mobile, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.register(email, password);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                FirestoreGateway.UserProfile profile = new FirestoreGateway.UserProfile(
                        auth.uid, name, auth.email, mobile, "user", "active");
                FIRESTORE.saveUserProfile(profile, auth.idToken);
                AppSession.set(new AppSession.User(auth.uid, auth.email, "user", auth.idToken, name, "active"));
                AppDataStore.refreshFirebaseData(auth.idToken);
                return AuthOutcome.success(AppSession.currentUser());
            } catch (Exception exception) {
                return AuthOutcome.failure("Registration failed. Check internet and Firebase rules.");
            }
        });
    }

    public static CompletableFuture<AuthOutcome> registerBusiness(BusinessAuthPage.BusinessAccount account,
            String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.register(account.email, password);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                FirestoreGateway.UserProfile profile = new FirestoreGateway.UserProfile(
                        auth.uid, account.ownerName, auth.email, account.mobile, "business", "pending");
                FIRESTORE.saveUserProfile(profile, auth.idToken);
                FIRESTORE.saveBusinessProfile(auth.uid, account, auth.idToken);
                FIRESTORE.saveApproval(new AppDataStore.ApprovalRequest(
                        "Business Registration",
                        account.businessName,
                        account.category + " | " + account.location + " | " + account.mobile,
                        "business",
                        auth.uid), auth.idToken);
                return AuthOutcome.failure("Business registration submitted. Admin approval is required before login.");
            } catch (Exception exception) {
                return AuthOutcome.failure("Business registration failed. Check internet and Firebase rules.");
            }
        });
    }

    public static CompletableFuture<AuthOutcome> registerOperator(OperatorAuthPage.OperatorAccount account,
            String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.register(account.email, password);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                FirestoreGateway.UserProfile profile = new FirestoreGateway.UserProfile(
                        auth.uid, account.contactPerson, auth.email, account.mobile, "transport_operator", "pending");
                FIRESTORE.saveUserProfile(profile, auth.idToken);
                FIRESTORE.saveTransportOperatorProfile(auth.uid, account, auth.idToken);
                FIRESTORE.saveApproval(new AppDataStore.ApprovalRequest(
                        "Transport Operator Registration",
                        account.organizationName,
                        account.serviceType + " | " + account.contactPerson + " | " + account.mobile,
                        "transport",
                        auth.uid), auth.idToken);
                return AuthOutcome.failure("Transport registration submitted. Admin approval is required before login.");
            } catch (Exception exception) {
                return AuthOutcome.failure("Transport registration failed. Check internet and Firebase rules.");
            }
        });
    }

    public static CompletableFuture<String> resetPassword(String email) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return "Firebase is not enabled. Check firebase.properties.";
            }
            try {
                boolean sent = AUTH.sendPasswordReset(email);
                return sent ? "Password reset email sent." : "Password reset could not be sent.";
            } catch (Exception exception) {
                return "Password reset failed. Check email and internet connection.";
            }
        });
    }

    public static CompletableFuture<AuthOutcome> loginWithGoogleIdToken(String googleIdToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.loginWithGoogleIdToken(googleIdToken);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                FirestoreGateway.UserProfile profile = FIRESTORE.loadUserProfile(auth.uid, auth.idToken);
                if (profile == null || profile.role().isBlank()) {
                    profile = new FirestoreGateway.UserProfile(auth.uid, auth.email, auth.email, "", "user", "active");
                    FIRESTORE.saveUserProfile(profile, auth.idToken);
                }

                AppSession.User user = new AppSession.User(
                        auth.uid, auth.email, profile.role(), auth.idToken, profile.name(), profile.status());
                AppSession.set(user);
                AppDataStore.refreshFirebaseData(auth.idToken);
                return AuthOutcome.success(user);
            } catch (Exception exception) {
                return AuthOutcome.failure("Google sign-in could not be completed.");
            }
        });
    }

    public record AuthOutcome(boolean success, AppSession.User user, String message) {
        public static AuthOutcome success(AppSession.User user) {
            return new AuthOutcome(true, user, "");
        }

        public static AuthOutcome failure(String message) {
            return new AuthOutcome(false, null, message);
        }
    }
}
