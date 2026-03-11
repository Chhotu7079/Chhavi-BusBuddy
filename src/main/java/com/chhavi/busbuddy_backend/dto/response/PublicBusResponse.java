package com.chhavi.busbuddy_backend.dto.response;

public class PublicBusResponse {
    private String id;
    private String code;
    private PublicRouteSummaryResponse route;

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

    public PublicRouteSummaryResponse getRoute() {
        return route;
    }

    public void setRoute(PublicRouteSummaryResponse route) {
        this.route = route;
    }
}
