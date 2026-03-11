package com.chhavi.busbuddy_backend.dto.response;

public class BusSearchResponse {
    private String busId;
    private String busCode;
    private String routeId;
    private String routeCode;

    public BusSearchResponse() {
    }

    public BusSearchResponse(String busId, String busCode, String routeId, String routeCode) {
        this.busId = busId;
        this.busCode = busCode;
        this.routeId = routeId;
        this.routeCode = routeCode;
    }

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

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }
}
