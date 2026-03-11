package com.chhavi.busbuddy_backend.persistence.model;

import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.annotation.DocumentId;

public class Bus {
    @DocumentId
    private String id;
    private String code;
    private int lastStop;
    private double speed;
    private Route route;
    private GeoPoint coords;

    public Bus() {
    }

    public Bus(String id, Route route, GeoPoint coords, double speed, int lastStop, String code) {
        this.id = id;
        this.route = route;
        this.coords = coords;
        this.speed = speed;
        this.lastStop = lastStop;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public GeoPoint getCoords() {
        return coords;
    }

    public void setCoords(GeoPoint coords) {
        this.coords = coords;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getLastStop() {
        return lastStop;
    }

    public void setLastStop(int lastStop) {
        this.lastStop = lastStop;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
