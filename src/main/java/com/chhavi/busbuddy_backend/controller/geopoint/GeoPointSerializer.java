package com.chhavi.busbuddy_backend.controller.geopoint;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.cloud.firestore.GeoPoint;

public class GeoPointSerializer extends JsonSerializer<GeoPoint> {

    /**
     * Serializes a {@link GeoPoint} into a JSON object with two fields:
     * "latitude" and "longitude".
     *
     * @param value the GeoPoint to serialize
     * @param gen the JsonGenerator to write to
     * @param serializers the SerializerProvider to use
     * @throws IOException if there is an error while writing to the JsonGenerator
     */
    @Override
    public void serialize(GeoPoint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // Start the JSON object
        gen.writeStartObject();

        // Write the latitude field
        gen.writeFieldName("latitude");
        gen.writeNumber(value.getLatitude());

        // Write the longitude field
        gen.writeFieldName("longitude");
        gen.writeNumber(value.getLongitude());

        // End the JSON object
        gen.writeEndObject();
    }
}
