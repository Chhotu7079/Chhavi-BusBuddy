package com.chhavi.busbuddy_backend.controller.geopoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.firestore.GeoPoint;

@Configuration
public class GeoPointJacksonConfig {

    /**
     * Configures the custom serializer and deserializer for the GeoPoint class.
     *
     * @return The configured module.
     */
    @Bean
    public Module customSerializerModule() {
        // Create a new SimpleModule to configure the serializer and deserializer.
        SimpleModule module = new SimpleModule();

        // Add the GeoPointSerializer to serialize GeoPoint objects.
        module.addSerializer(GeoPoint.class, new GeoPointSerializer());

        // Add the GeoPointDeserializer to deserialize GeoPoint objects.
        module.addDeserializer(GeoPoint.class, new GeoPointDeserializer());

        // Return the configured module.
        return module;
    }
}