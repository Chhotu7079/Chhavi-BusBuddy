package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;

public class RouteSearchResponse {
    private String routeId;
    private String routeCode;

    public RouteSearchResponse() {
    }

    public RouteSearchResponse(String routeId, String routeCode) {
        this.routeId = routeId;
        this.routeCode = routeCode;
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
