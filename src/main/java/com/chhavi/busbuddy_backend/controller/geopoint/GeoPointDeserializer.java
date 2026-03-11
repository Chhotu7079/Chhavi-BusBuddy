package com.chhavi.busbuddy_backend.controller.geopoint;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.firestore.GeoPoint;

public class GeoPointDeserializer extends JsonDeserializer<GeoPoint> {

    @Override
    public GeoPoint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        JsonNode latNode = node.get("latitude");
        JsonNode lngNode = node.get("longitude");
        if (latNode == null || lngNode == null || !latNode.isNumber() || !lngNode.isNumber()) {
            return null;
        }
        return new GeoPoint(latNode.doubleValue(), lngNode.doubleValue());
    }
}