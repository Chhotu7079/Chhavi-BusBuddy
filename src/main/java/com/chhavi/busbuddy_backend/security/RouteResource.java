package com.chhavi.busbuddy_backend.security;

import com.chhavi.busbuddy_backend.persistence.model.Route;

/**
 * Test- and service-friendly view of a route resource used by authorization- and
 * mutation-sensitive flows without exposing raw Firestore internals everywhere.
 */
public class RouteResource {
    private final String id;
    private final String companyId;
    private final Route route;

    public RouteResource(String id, String companyId, Route route) {
        this.id = id;
        this.companyId = companyId;
        this.route = route;
    }

    public String getId() {
        return id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public Route getRoute() {
        return route;
    }
}
