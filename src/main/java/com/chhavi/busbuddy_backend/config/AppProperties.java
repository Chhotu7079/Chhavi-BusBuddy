package com.chhavi.busbuddy_backend.config;

import com.chhavi.busbuddy_backend.exception.ConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds application-level settings that affect runtime behavior across several
 * layers, such as JWT handling, timezone selection, and allowed web origins.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private String timezone = "Asia/Kolkata";
    private String corsAllowedOrigins = "http://localhost:3000";

    public Security getSecurity() {
        return security;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    /**
     * Validates core settings early so the application fails fast with a clear
     * configuration message instead of surfacing broken runtime behavior later.
     */
    public void validate() {
        if (security.jwt.secret == null || security.jwt.secret.isBlank()) {
            throw new ConfigurationException("Missing required property: app.security.jwt.secret");
        }
        if (security.jwt.secret.length() < 32) {
            throw new ConfigurationException("app.security.jwt.secret must be at least 32 characters long");
        }
        if (security.jwt.expirationMs <= 0) {
            throw new ConfigurationException("app.security.jwt.expiration-ms must be greater than 0");
        }
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 604800000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }
}
