package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;

public class PublicRouteResponse {
    private String id;
    private String code;
    private List<PublicStopSummaryResponse> stops;

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

    public List<PublicStopSummaryResponse> getStops() {
        return stops;
    }

    public void setStops(List<PublicStopSummaryResponse> stops) {
        this.stops = stops;
    }
}
