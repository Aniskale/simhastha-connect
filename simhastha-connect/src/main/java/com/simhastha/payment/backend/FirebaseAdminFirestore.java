package com.simhastha.payment.backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.simhastha.payment.PaymentException;

/** Backend-only Firebase Admin bootstrap. This class is never used by JavaFX. */
public final class FirebaseAdminFirestore {
    private FirebaseAdminFirestore() { }

    public static Firestore open(PaymentBackendConfig config) throws PaymentException {
        String path = config.serviceAccountPath();
        if (path == null || path.isBlank()) {
            throw new PaymentException("Business booking requires backend GOOGLE_APPLICATION_CREDENTIALS.");
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Path credentialFile = Path.of(path).toAbsolutePath().normalize();
                if (!Files.isRegularFile(credentialFile)) {
                    throw new PaymentException("Backend Firebase service-account credential file was not found.");
                }
                try (FileInputStream input = new FileInputStream(credentialFile.toFile())) {
                    FirebaseOptions.Builder options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(input));
                    if (config.firestoreProjectId() != null && !config.firestoreProjectId().isBlank()) {
                        options.setProjectId(config.firestoreProjectId());
                    }
                    FirebaseApp.initializeApp(options.build());
                }
            }
            return FirestoreClient.getFirestore();
        } catch (IOException | IllegalStateException exception) {
            throw new PaymentException("Backend Firebase Admin initialization failed.", exception);
        }
    }
}
