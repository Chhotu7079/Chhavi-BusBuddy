package com.chhavi.busbuddy_backend;

import com.chhavi.busbuddy_backend.config.AppProperties;
import com.chhavi.busbuddy_backend.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppPropertiesTests {

    @Test
    void validateShouldPassForValidJwtSettings() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().getJwt().setSecret("12345678901234567890123456789012");
        properties.getSecurity().getJwt().setExpirationMs(60000);

        assertDoesNotThrow(properties::validate);
    }

    @Test
    void validateShouldFailForShortSecret() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().getJwt().setSecret("short");
        properties.getSecurity().getJwt().setExpirationMs(60000);

        assertThrows(ConfigurationException.class, properties::validate);
    }

    @Test
    void validateShouldFailForNonPositiveExpiration() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().getJwt().setSecret("12345678901234567890123456789012");
        properties.getSecurity().getJwt().setExpirationMs(0);

        assertThrows(ConfigurationException.class, properties::validate);
    }
}
