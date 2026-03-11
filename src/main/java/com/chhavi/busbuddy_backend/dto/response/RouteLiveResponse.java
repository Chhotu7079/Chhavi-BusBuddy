package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;

public class RouteLiveResponse {
    private String routeId;
    private List<BusLiveResponse> activeBuses;

    public RouteLiveResponse() {
    }

    public RouteLiveResponse(String routeId, List<BusLiveResponse> activeBuses) {
        this.routeId = routeId;
        this.activeBuses = activeBuses;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public List<BusLiveResponse> getActiveBuses() {
        return activeBuses;
    }

    public void setActiveBuses(List<BusLiveResponse> activeBuses) {
        this.activeBuses = activeBuses;
    }
}
