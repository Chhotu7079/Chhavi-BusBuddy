package com.chhavi.busbuddy_backend.dto.response;

public class BusLiveResponse {
    private String busId;
    private String busCode;
    private CoordinatesResponse coords;
    private Double speed;
    private Integer lastStop;
    private String direction;
    private String routeId;
    private Long lastUpdatedAt;
    private String status;

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getBusCode() {
        return busCode;
    }

    public void setBusCode(String busCode) {
        this.busCode = busCode;
    }

    public CoordinatesResponse getCoords() {
        return coords;
    }

    public void setCoords(CoordinatesResponse coords) {
        this.coords = coords;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Integer getLastStop() {
        return lastStop;
    }

    public void setLastStop(Integer lastStop) {
        this.lastStop = lastStop;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
