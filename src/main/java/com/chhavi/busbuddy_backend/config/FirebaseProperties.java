package com.chhavi.busbuddy_backend.config;

import com.chhavi.busbuddy_backend.exception.ConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;

/**
 * Captures Firebase-specific connection settings so startup validation and
 * infrastructure initialization do not rely on scattered raw environment lookups.
 */
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private String serviceAccountPath;
    private String databaseUrl;

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    /**
     * Performs fail-fast validation because missing credentials or malformed
     * database URLs are deployment issues, not recoverable runtime conditions.
     */
    public void validate() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            throw new ConfigurationException("Missing required property: firebase.service-account-path");
        }
        File credentialsFile = new File(serviceAccountPath);
        if (!credentialsFile.exists()) {
            throw new ConfigurationException("Firebase credentials file does not exist at path: " + serviceAccountPath);
        }
        if (!credentialsFile.isFile()) {
            throw new ConfigurationException("Firebase credentials path is not a file: " + serviceAccountPath);
        }
        if (!credentialsFile.canRead()) {
            throw new ConfigurationException("Firebase credentials file is not readable: " + serviceAccountPath);
        }
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new ConfigurationException("Missing required property: firebase.database-url");
        }
        try {
            URI uri = URI.create(databaseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new ConfigurationException("firebase.database-url must be a valid absolute URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException("firebase.database-url must be a valid URL", exception);
        }
    }
}
