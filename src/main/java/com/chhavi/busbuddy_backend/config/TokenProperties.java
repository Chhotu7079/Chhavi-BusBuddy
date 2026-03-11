package com.chhavi.busbuddy_backend.config;

import org.springframework.stereotype.Component;

@Component
public class TokenProperties {

    private final String secretKey;
    private final long expirationTimeMs;

    public TokenProperties(AppProperties appProperties) {
        appProperties.validate();
        this.secretKey = appProperties.getSecurity().getJwt().getSecret();
        this.expirationTimeMs = appProperties.getSecurity().getJwt().getExpirationMs();
    }

    public String getSecretKey() {
        return secretKey;
    }

    public long getExpirationTimeMs() {
        return expirationTimeMs;
    }
}
