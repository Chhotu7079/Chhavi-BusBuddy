package com.chhavi.busbuddy_backend.persistence;

import com.chhavi.busbuddy_backend.config.FirebaseProperties;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Boots the Firebase Admin SDK once during application startup so the rest of
 * the application can use shared Firestore, Auth, and Realtime Database clients.
 */
@Component
public class FirestoreInitializer {

    private static final Logger log = LoggerFactory.getLogger(FirestoreInitializer.class);

    private final FirebaseProperties firebaseProperties;

    public FirestoreInitializer(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    /**
     * Initializes Firebase exactly once. Startup-time initialization is used so
     * configuration problems fail fast instead of surfacing only on first API use.
     */
    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            firebaseProperties.validate();
            String credentialsPath = firebaseProperties.getServiceAccountPath();
            String databaseUrl = firebaseProperties.getDatabaseUrl();

            log.info("Initializing Firebase using credentials file at {}", credentialsPath);

            try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (Exception exception) {
            throw new ApplicationException("Failed to initialize Firebase", exception);
        }
    }
}
