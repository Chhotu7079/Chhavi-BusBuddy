package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;

public class RouteSearchLiveResponse {
    private String routeId;
    private String routeCode;
    private List<BusLiveResponse> activeBuses;

    public RouteSearchLiveResponse() {
    }

    public RouteSearchLiveResponse(String routeId, String routeCode, List<BusLiveResponse> activeBuses) {
        this.routeId = routeId;
        this.routeCode = routeCode;
        this.activeBuses = activeBuses;
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

    public List<BusLiveResponse> getActiveBuses() {
        return activeBuses;
    }

    public void setActiveBuses(List<BusLiveResponse> activeBuses) {
        this.activeBuses = activeBuses;
    }
}
