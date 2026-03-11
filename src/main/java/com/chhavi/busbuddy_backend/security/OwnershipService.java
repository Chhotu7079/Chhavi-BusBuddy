package com.chhavi.busbuddy_backend.security;

public interface OwnershipService {
    String getRouteOwnerCompanyId(String routeId);
    String getBusOwnerCompanyId(String busId);
}
