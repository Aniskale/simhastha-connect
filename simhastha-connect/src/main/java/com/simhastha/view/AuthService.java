package com.simhastha.view;

import java.util.concurrent.CompletableFuture;

public final class AuthService {

    private static final FirebaseConfig CONFIG = FirebaseConfig.load();
    private static final FirebaseAuthGateway AUTH = new FirebaseAuthGateway(CONFIG);
    private static final FirestoreGateway FIRESTORE = new FirestoreGateway(CONFIG);
    private static final java.util.Set<String> VALID_ROLES = java.util.Set.of(
            "user", "business", "transport_operator", "admin");

    private AuthService() {
    }

    public static boolean isFirebaseEnabled() {
        return CONFIG.isEnabled();
    }

    public static CompletableFuture<AuthOutcome> login(String email, String password, String expectedRole) {
        return CompletableFuture.supplyAsync(() -> {
            String stage = "FIREBASE_AUTH";
            if (!CONFIG.isEnabled()) {
                diagnostic("FIREBASE_AUTH", "result=skipped firebaseEnabled=false");
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                diagnostic("FIREBASE_AUTH", "result=started");
                FirebaseAuthGateway.AuthResult auth = AUTH.login(email, password);
                if (!auth.success) {
                    diagnostic("FIREBASE_AUTH", "result=failed");
                    return AuthOutcome.failure(auth.errorMessage);
                }
                if (auth.uid == null || auth.uid.isBlank()) {
                    diagnostic("FIREBASE_AUTH", "result=failed missingUid=true");
                    return AuthOutcome.failure("Firebase authentication succeeded, but no authenticated UID was returned.");
                }
                diagnostic("FIREBASE_AUTH", "result=success");

                stage = "USER_PROFILE";
                diagnostic(stage, "result=started");
                FirestoreGateway.UserProfile profile = FIRESTORE.loadUserProfile(auth.uid, auth.idToken);
                if (profile == null) {
                    diagnostic("USER_PROFILE", "result=failed missingProfile=true");
                    return AuthOutcome.failure("No Firestore profile was found at users/" + auth.uid + ". Please contact admin.");
                }
                diagnostic("USER_PROFILE", "result=success");
                if (!VALID_ROLES.contains(profile.role())) {
                    diagnostic("ROLE_RESOLUTION", "result=failed invalidRole=true");
                    return AuthOutcome.failure("Your account role is not configured. Please contact the administrator.");
                }

                stage = "ROLE_RESOLUTION";
                if (!profile.role().equals(expectedRole) && !"any".equals(expectedRole)) {
                    diagnostic("ROLE_RESOLUTION", "result=failed portalRoleMismatch=true");
                    return AuthOutcome.failure("Authentication succeeded, but this account is not authorized for this portal.");
                }
                if ("admin".equals(profile.role()) && !"active".equals(profile.status())) {
                    diagnostic("ROLE_RESOLUTION", "result=failed inactiveAdmin=true");
                    return AuthOutcome.failure("Admin access requires an active Firestore admin profile.");
                }

                if ("pending".equals(profile.status())) {
                    if ("business".equals(profile.role())) {
                        return AuthOutcome.failure("Your business registration is awaiting admin approval.");
                    }
                    return AuthOutcome.failure("Your account is pending admin approval.");
                }
                if ("disabled".equals(profile.status()) || "rejected".equals(profile.status())
                        || "suspended".equals(profile.status())) {
                    return AuthOutcome.failure("This account is not active. Please contact admin.");
                }
                if ("business".equals(profile.role()) && !"approved".equals(profile.status())) {
                    return AuthOutcome.failure("Your business registration is awaiting admin approval.");
                }
                diagnostic(stage, "result=success role=" + profile.role());

                stage = "SESSION";
                AppSession.User user = new AppSession.User(
                        auth.uid,
                        auth.email,
                        profile.role(),
                        auth.idToken,
                        profile.name(),
                        profile.status());
                AppSession.set(user);
                diagnostic("SESSION", "result=success");

                // Dashboard module data is optional.  It must never turn a completed
                // Firebase Authentication + profile resolution into a failed login.
                CompletableFuture.runAsync(() -> refreshDashboardDataSafely(auth.idToken));
                return AuthOutcome.success(user);
            } catch (FirestoreGateway.PermissionDeniedException exception) {
                diagnostic(stage, "result=failed error=permissionDenied");
                return AuthOutcome.failure("Firestore permission denied while reading your account profile.");
            } catch (FirestoreGateway.MalformedProfileException exception) {
                diagnostic(stage, "result=failed error=malformedProfile");
                if ("USER_PROFILE".equals(stage) && exception.getMessage() != null
                        && exception.getMessage().toLowerCase().contains("role")) {
                    return AuthOutcome.failure("Your account role is not configured. Please contact the administrator.");
                }
                return AuthOutcome.failure(exception.getMessage());
            } catch (Exception exception) {
                diagnostic(stage, "result=failed error=" + exception.getClass().getSimpleName());
                return AuthOutcome.failure("Unable to complete authentication. Check internet, Firebase, and account permissions.");
            }
        });
    }

    private static void refreshDashboardDataSafely(String idToken) {
        try {
            AppDataStore.refreshFirebaseData(idToken);
            diagnostic("SESSION", "dashboardDataRefresh=completed");
        } catch (Exception exception) {
            // This data is not required to establish an authenticated session.
            diagnostic("SESSION", "dashboardDataRefresh=failed error=" + exception.getClass().getSimpleName());
        }
    }

    private static void diagnostic(String stage, String detail) {
        System.out.println("AUTH_DIAGNOSTIC stage=" + stage + " " + detail);
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
                        auth.uid, account.contactPerson, auth.email, account.mobile, "transport_operator", "active");
                FIRESTORE.saveUserProfile(profile, auth.idToken);
                FIRESTORE.saveTransportOperatorProfile(auth.uid, account, auth.idToken);
                AppSession.set(new AppSession.User(auth.uid, auth.email, "transport_operator", auth.idToken,
                        account.contactPerson, "active"));
                AppDataStore.refreshFirebaseData(auth.idToken);
                return AuthOutcome.success(AppSession.currentUser());
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
