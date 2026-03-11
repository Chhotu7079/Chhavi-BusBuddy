package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.constant.RouteDirection;
import com.chhavi.busbuddy_backend.dto.request.AddRouteRequest;
import com.chhavi.busbuddy_backend.exception.BadRequestException;
import com.chhavi.busbuddy_backend.persistence.model.Route;

/**
 * Keeps common request validation rules in one place so controllers can stay
 * thin while still producing consistent bad-request behavior.
 */
public final class RequestValidationUtils {

    private RequestValidationUtils() {
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }

    public static RouteDirection requireDirection(String direction) {
        return RouteDirection.from(direction);
    }

    public static void requireAddRoutePayload(AddRouteRequest request) {
        if (request == null) {
            throw new BadRequestException("route request body must not be null");
        }
        requireNonBlank(request.getCode(), "route.code");
        requireNonBlank(request.getCompany(), "route.company");
        if (request.getStops() == null || request.getStops().getForwardStops() == null || request.getStops().getForwardStops().isEmpty()) {
            throw new BadRequestException("route.stops.forwardStops must not be empty");
        }
        if (request.getStops().getBackStops() == null) {
            throw new BadRequestException("route.stops.backStops must not be null");
        }
    }

    public static void requireRoutePayload(Route route) {
        if (route == null) {
            throw new BadRequestException("route request body must not be null");
        }
        requireNonBlank(route.getCode(), "route.code");
        requireNonBlank(route.getCompany(), "route.company");
        if (route.getStops() == null || route.getStops().getForwardStops() == null || route.getStops().getForwardStops().isEmpty()) {
            throw new BadRequestException("route.stops.forwardStops must not be empty");
        }
        if (route.getStops().getBackStops() == null) {
            throw new BadRequestException("route.stops.backStops must not be null");
        }
    }
}
