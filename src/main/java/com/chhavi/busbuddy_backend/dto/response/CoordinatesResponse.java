package com.chhavi.busbuddy_backend.dto.response;

/**
 * Simple API coordinate payload used instead of exposing Firestore GeoPoint
 * directly in responses.
 */
public class CoordinatesResponse {
    private double latitude;
    private double longitude;

    public CoordinatesResponse() {
    }

    public CoordinatesResponse(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
