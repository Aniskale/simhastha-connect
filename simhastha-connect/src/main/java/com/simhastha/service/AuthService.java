package com.simhastha.service;

import com.simhastha.dao.ApprovalDao;
import com.simhastha.dao.BusinessDao;
import com.simhastha.dao.OperatorDao;
import com.simhastha.dao.UserDao;
import com.simhastha.dao.implementation.FirestoreApprovalDao;
import com.simhastha.dao.implementation.FirestoreBusinessDao;
import com.simhastha.dao.implementation.FirestoreOperatorDao;
import com.simhastha.dao.implementation.FirestoreUserDao;
import com.simhastha.gateway.firebase.FirebaseAuthGateway;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.UserProfile;
import com.simhastha.util.AppSession;
import com.simhastha.view.AppDataStore;
import com.simhastha.view.BusinessAuthPage;
import com.simhastha.view.OperatorAuthPage;

import java.util.concurrent.CompletableFuture;

public final class AuthService {

    private static final FirebaseConfig CONFIG = FirebaseConfig.load();
    private static final FirebaseAuthGateway AUTH = new FirebaseAuthGateway(CONFIG);
    private static final FirestoreGateway FIRESTORE = new FirestoreGateway(CONFIG);
    private static final UserDao USER_DAO = new FirestoreUserDao(FIRESTORE);
    private static final BusinessDao BUSINESS_DAO = new FirestoreBusinessDao(FIRESTORE);
    private static final OperatorDao OPERATOR_DAO = new FirestoreOperatorDao(FIRESTORE);
    private static final ApprovalDao APPROVAL_DAO = new FirestoreApprovalDao(FIRESTORE);
    private static final java.util.Set<String> VALID_ROLES = java.util.Set.of(
            "user", "business", "transport_operator", "admin");

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
                if (auth.uid == null || auth.uid.isBlank()) {
                    return AuthOutcome.failure("Firebase authentication succeeded, but no authenticated UID was returned.");
                }

                UserProfile profile = USER_DAO.findProfile(auth.uid, auth.idToken).orElse(null);
                if (profile == null) {
                    return AuthOutcome.failure("No Firestore profile was found at users/" + auth.uid + ". Please contact admin.");
                }
                if (!VALID_ROLES.contains(profile.role())) {
                    return AuthOutcome.failure("Your account role is not valid. Expected one of: user, business, transport_operator, admin.");
                }

                boolean adminOverride = "admin".equals(profile.role());
                if (!adminOverride && !profile.role().equals(expectedRole) && !"any".equals(expectedRole)) {
                    return AuthOutcome.failure("Authentication succeeded, but this account is not authorized for this portal.");
                }
                if (adminOverride && !"active".equals(profile.status())) {
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
            } catch (FirestoreGateway.PermissionDeniedException exception) {
                return AuthOutcome.failure("Firestore permission denied while reading your account profile.");
            } catch (FirestoreGateway.MalformedProfileException exception) {
                return AuthOutcome.failure(exception.getMessage());
            } catch (Exception exception) {
                return AuthOutcome.failure("Unable to complete authentication. Check internet, Firebase, and account permissions.");
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

                UserProfile profile = new UserProfile(
                        auth.uid, name, auth.email, mobile, "user", "active");
                USER_DAO.saveProfile(profile, auth.idToken);
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

                UserProfile profile = new UserProfile(
                        auth.uid, account.ownerName, auth.email, account.mobile, "business", "pending");
                USER_DAO.saveProfile(profile, auth.idToken);
                BUSINESS_DAO.saveRegistration(auth.uid, account, auth.idToken);
                APPROVAL_DAO.save(new AppDataStore.ApprovalRequest(
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

                UserProfile profile = new UserProfile(
                        auth.uid, account.contactPerson, auth.email, account.mobile, "transport_operator", "active");
                USER_DAO.saveProfile(profile, auth.idToken);
                OPERATOR_DAO.saveRegistration(auth.uid, account, auth.idToken);
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

                UserProfile profile = USER_DAO.findProfile(auth.uid, auth.idToken).orElse(null);
                if (profile == null || profile.role().isBlank()) {
                    profile = new UserProfile(auth.uid, auth.email, auth.email, "", "user", "active");
                    USER_DAO.saveProfile(profile, auth.idToken);
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
