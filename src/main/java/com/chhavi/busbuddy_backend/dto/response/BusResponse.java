package com.chhavi.busbuddy_backend.dto.response;

/**
 * API response representing a bus together with the route and location details
 * needed by clients.
 */
public class BusResponse {
    private String id;
    private String code;
    private int lastStop;
    private double speed;
    private CoordinatesResponse coords;
    private RouteResponse route;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLastStop() {
        return lastStop;
    }

    public void setLastStop(int lastStop) {
        this.lastStop = lastStop;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public CoordinatesResponse getCoords() {
        return coords;
    }

    public void setCoords(CoordinatesResponse coords) {
        this.coords = coords;
    }

    public RouteResponse getRoute() {
        return route;
    }

    public void setRoute(RouteResponse route) {
        this.route = route;
    }
}
