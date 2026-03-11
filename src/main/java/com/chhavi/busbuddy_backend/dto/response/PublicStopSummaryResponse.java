package com.chhavi.busbuddy_backend.dto.response;

public class PublicStopSummaryResponse {
    private String name;
    private CoordinatesResponse coords;

    public PublicStopSummaryResponse() {
    }

    public PublicStopSummaryResponse(String name, CoordinatesResponse coords) {
        this.name = name;
        this.coords = coords;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinatesResponse getCoords() {
        return coords;
    }

    public void setCoords(CoordinatesResponse coords) {
        this.coords = coords;
    }
}
