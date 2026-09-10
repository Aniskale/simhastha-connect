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
import com.simhastha.gateway.firebase.FirestoreCollections;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.UserProfile;
import com.simhastha.util.AppSession;
import com.simhastha.view.AppDataStore;
import com.simhastha.view.BusinessAuthPage;
import com.simhastha.view.OperatorAuthPage;

import com.simhastha.config.CloudinaryFolders;
import com.simhastha.model.CloudinaryUploadResult;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private static final ScheduledExecutorService PROFILE_RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "user-profile-retry");
        thread.setDaemon(true);
        return thread;
    });

    private AuthService() {
    }

    public static boolean isFirebaseEnabled() {
        return CONFIG.isEnabled();
    }

    public static CompletableFuture<AuthOutcome> login(String email, String password, String expectedRole) {
        return CompletableFuture.supplyAsync(() -> {
            String step = "START";
            System.out.println("USER_LOGIN_TRACE step=START firebaseEnabled=" + CONFIG.isEnabled());
            if (!CONFIG.isEnabled()) {
                System.err.println("USER_LOGIN_ERROR step=CONFIG CAUSE=Firebase configuration is disabled or incomplete");
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                step = "FIREBASE_AUTH";
                System.out.println("USER_LOGIN_TRACE step=FIREBASE_AUTH");
                FirebaseAuthGateway.AuthResult auth = AUTH.login(email, password);
                if (!auth.success) {
                    System.err.println("USER_LOGIN_ERROR step=FIREBASE_AUTH FIREBASE_ERROR=" + safeDiagnostic(auth.errorMessage));
                    return AuthOutcome.failure(auth.errorMessage);
                }
                if (auth.uid == null || auth.uid.isBlank()) {
                    System.err.println("USER_LOGIN_ERROR step=FIREBASE_AUTH CAUSE=No UID returned");
                    return AuthOutcome.failure("Firebase authentication succeeded, but no authenticated UID was returned.");
                }
                System.out.println("USER_LOGIN_TRACE step=AUTH_SUCCESS uid=" + auth.uid);

                step = "PROFILE_LOAD";
                UserProfile profile = loadProfileWithRetry(auth.uid, auth.idToken);
                if (profile == null) {
                    System.err.println("USER_LOGIN_ERROR step=PROFILE_LOAD HTTP_STATUS=404 path=" + FirestoreCollections.USERS + "/" + auth.uid);
                    return AuthOutcome.failure("Your account profile could not be found.");
                }
                step = "ROLE_CHECK";
                String role = profile.role() == null ? "" : profile.role().trim().toLowerCase(java.util.Locale.ROOT);
                String accountStatus = profile.status() == null ? "" : profile.status().trim().toLowerCase(java.util.Locale.ROOT);
                System.out.println("USER_LOGIN_TRACE step=ROLE_CHECK role=" + role + " status=" + accountStatus);
                if (!VALID_ROLES.contains(role)) {
                    System.err.println("USER_LOGIN_ERROR step=ROLE_CHECK CAUSE=Missing or unsupported role");
                    return AuthOutcome.failure("Your account role is not configured. Please contact the administrator.");
                }

                boolean adminOverride = "admin".equals(role);
                if (!adminOverride && !role.equals(expectedRole) && !"any".equals(expectedRole)) {
                    System.err.println("USER_LOGIN_ERROR step=ROLE_CHECK CAUSE=Portal role mismatch");
                    return AuthOutcome.failure("Authentication succeeded, but this account is not authorized for this portal.");
                }
                if (adminOverride && !"active".equals(accountStatus)) {
                    return AuthOutcome.failure("Admin access requires an active Firestore admin profile.");
                }

                if ("pending".equals(accountStatus)) {
                    if ("business".equals(role)) {
                        return AuthOutcome.failure("Your business registration is awaiting admin approval.");
                    }
                    return AuthOutcome.failure("Your account is pending admin approval.");
                }
                if ("disabled".equals(accountStatus) || "rejected".equals(accountStatus)
                        || "suspended".equals(accountStatus)) {
                    return AuthOutcome.failure("This account is not active. Please contact admin.");
                }
                if ("business".equals(role) && !"approved".equals(accountStatus)) {
                    return AuthOutcome.failure("Your business registration is awaiting admin approval.");
                }

                step = "SESSION";
                System.out.println("USER_LOGIN_TRACE step=SESSION uid=" + auth.uid);
                AppSession.User user = new AppSession.User(
                        auth.uid,
                        auth.email,
                        role,
                        auth.idToken,
                        profile.name(),
                        accountStatus,
                        profile.profilePhotoUrl(),
                        profile.profilePhotoPublicId());
                AppSession.set(user);
                step = "DASHBOARD_OPEN";
                System.out.println("USER_LOGIN_TRACE step=DASHBOARD_OPEN role=" + role);
                return AuthOutcome.success(user);
            } catch (FirestoreGateway.PermissionDeniedException exception) {
                System.err.println("USER_LOGIN_ERROR step=" + step + " HTTP_STATUS=403 CAUSE=" + safeDiagnostic(exception.getMessage()));
                return AuthOutcome.failure("Your account does not have permission to access the user portal.");
            } catch (FirestoreGateway.TooManyRequestsException exception) {
                System.err.println("USER_LOGIN_ERROR step=" + step + " HTTP_STATUS=429 CAUSE=" + safeDiagnostic(exception.getMessage()));
                return AuthOutcome.failure("Signed in successfully, but your profile could not be loaded right now. Please retry.");
            } catch (FirestoreGateway.MalformedProfileException exception) {
                System.err.println("USER_LOGIN_ERROR step=" + step + " CAUSE=" + safeDiagnostic(exception.getMessage()));
                return AuthOutcome.failure(exception.getMessage());
            } catch (Exception exception) {
                System.err.println("USER_LOGIN_ERROR step=" + step + " CAUSE=" + safeDiagnostic(exception.getMessage())
                        + " exception=" + exception.getClass().getSimpleName());
                String cause = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(java.util.Locale.ROOT);
                if (cause.contains("connect") || cause.contains("timeout") || cause.contains("reset") || cause.contains("network")) {
                    return AuthOutcome.failure("Unable to connect to Firebase. Check your internet connection.");
                }
                return AuthOutcome.failure("Unable to complete authentication. Please try again.");
            }
        });
    }

    private static UserProfile loadProfileWithRetry(String uid, String idToken) throws Exception {
        long[] delays = {2_000L, 5_000L};
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("USER_LOGIN_TRACE step=PROFILE_LOAD_ATTEMPT attempt=" + attempt + " uid=" + uid
                    + " path=" + FirestoreCollections.USERS + "/" + uid);
            try {
                UserProfile profile = USER_DAO.findProfile(uid, idToken).orElse(null);
                System.out.println("USER_LOGIN_TRACE step=PROFILE_LOAD_HTTP status=" + (profile == null ? 404 : 200));
                if (profile != null) System.out.println("USER_LOGIN_TRACE step=PROFILE_LOAD_SUCCESS uid=" + uid);
                return profile;
            } catch (FirestoreGateway.TooManyRequestsException exception) {
                System.out.println("USER_LOGIN_TRACE step=PROFILE_LOAD_HTTP status=429");
                if (attempt == 3) throw exception;
                long delayMillis = exception.retryAfterMillis() > 0 ? exception.retryAfterMillis() : delays[attempt - 1];
                System.err.println("USER_LOGIN_WARN step=PROFILE_LOAD status=429 action=RETRY attempt=" + attempt
                        + " delayMillis=" + delayMillis);
                CompletableFuture<Void> delay = new CompletableFuture<>();
                PROFILE_RETRY_EXECUTOR.schedule(() -> delay.complete(null), delayMillis, TimeUnit.MILLISECONDS);
                delay.join();
            }
        }
        throw new FirestoreGateway.TooManyRequestsException("Firestore profile read failed: 429");
    }

    private static String safeDiagnostic(String value) {
        if (value == null || value.isBlank()) return "No diagnostic message";
        return value.replaceAll("(?i)(idToken|refreshToken|password|apiKey)=[^\\s,]+", "$1=[redacted]");
    }

    public static CompletableFuture<AuthOutcome> registerUser(String name, String mobile, String email, String password) {
        return registerUser(name, mobile, email, password, null);
    }

    public static CompletableFuture<AuthOutcome> registerUser(String name, String mobile, String email, String password,
            File profilePhotoFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            try {
                FirebaseAuthGateway.AuthResult auth = AUTH.register(email, password);
                if (!auth.success) {
                    return AuthOutcome.failure(auth.errorMessage);
                }

                String profilePhotoUrl = "";
                String profilePhotoPublicId = "";
                if (profilePhotoFile != null) {
                    CloudinaryUploadResult photo = new CloudinaryService().uploadImage(profilePhotoFile,
                            CloudinaryFolders.USER_PROFILE);
                    profilePhotoUrl = photo.getSecureUrl();
                    profilePhotoPublicId = photo.getPublicId();
                }
                UserProfile profile = new UserProfile(
                        auth.uid, name, auth.email, mobile, "user", "active", profilePhotoUrl, profilePhotoPublicId);
                USER_DAO.saveProfile(profile, auth.idToken);
                AppSession.set(new AppSession.User(auth.uid, auth.email, "user", auth.idToken, name, "active",
                        profile.profilePhotoUrl(), profile.profilePhotoPublicId()));
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

                CloudinaryService cloudinary = new CloudinaryService();
                if (account.logoFile != null) {
                    CloudinaryUploadResult logo = cloudinary.uploadImage(account.logoFile, CloudinaryFolders.BUSINESS_LOGO);
                    account.logoUrl = logo.getSecureUrl();
                    account.logoPublicId = logo.getPublicId();
                }
                if (account.coverFile != null) {
                    CloudinaryUploadResult cover = cloudinary.uploadImage(account.coverFile, CloudinaryFolders.BUSINESS_GALLERY);
                    account.coverPhotoUrl = cover.getSecureUrl();
                    account.coverPhotoPublicId = cover.getPublicId();
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

    public static CompletableFuture<AuthOutcome> changePassword(String currentPassword, String newPassword) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CONFIG.isEnabled()) {
                return AuthOutcome.failure("Firebase is not enabled. Check firebase.properties.");
            }
            AppSession.User user = AppSession.currentUser();
            if (user == null || user.email() == null || user.email().isBlank()) {
                return AuthOutcome.failure("Please login again before changing your password.");
            }
            try {
                FirebaseAuthGateway.AuthResult login = AUTH.login(user.email(), currentPassword);
                if (!login.success) {
                    return AuthOutcome.failure("Current password is incorrect.");
                }
                FirebaseAuthGateway.AuthResult update = AUTH.updatePassword(login.idToken, newPassword);
                if (!update.success) {
                    return AuthOutcome.failure(update.errorMessage);
                }
                AppSession.User updated = new AppSession.User(user.uid(), user.email(), user.role(), update.idToken,
                        user.displayName(), user.status(), user.profilePhotoUrl(), user.profilePhotoPublicId());
                AppSession.set(updated);
                return AuthOutcome.success(updated);
            } catch (Exception exception) {
                return AuthOutcome.failure("Password could not be changed. Check internet and try again.");
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
                        auth.uid, auth.email, profile.role(), auth.idToken, profile.name(), profile.status(),
                        profile.profilePhotoUrl(), profile.profilePhotoPublicId());
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
