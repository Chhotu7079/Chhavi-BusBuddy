package com.chhavi.busbuddy_backend.dto.response;

/**
 * API response representing a stop and its client-facing location details.
 */
public class StopResponse {
    private String id;
    private String name;
    private String address;
    private CoordinatesResponse coords;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CoordinatesResponse getCoords() {
        return coords;
    }

    public void setCoords(CoordinatesResponse coords) {
        this.coords = coords;
    }
}
