package com.chhavi.busbuddy_backend.dto.response;

public class BusSearchLiveResponse {
    private String busId;
    private String busCode;
    private String routeId;
    private String routeCode;
    private BusLiveResponse live;

    public BusSearchLiveResponse() {
    }

    public BusSearchLiveResponse(String busId, String busCode, String routeId, String routeCode, BusLiveResponse live) {
        this.busId = busId;
        this.busCode = busCode;
        this.routeId = routeId;
        this.routeCode = routeCode;
        this.live = live;
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

    public BusLiveResponse getLive() {
        return live;
    }

    public void setLive(BusLiveResponse live) {
        this.live = live;
    }
}
