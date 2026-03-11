package com.chhavi.busbuddy_backend.dto.request;

import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;

/**
 * API request payload for creating a route and associating its stops.
 */
public class AddRouteRequest {
    private String company;
    private String code;
    private ForwardBackStops stops;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ForwardBackStops getStops() {
        return stops;
    }

    public void setStops(ForwardBackStops stops) {
        this.stops = stops;
    }
}
