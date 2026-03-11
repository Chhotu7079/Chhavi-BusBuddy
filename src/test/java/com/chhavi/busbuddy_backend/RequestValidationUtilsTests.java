package com.chhavi.busbuddy_backend;

import com.chhavi.busbuddy_backend.exception.BadRequestException;
import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Stop;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidationUtilsTests {

    @Test
    void requireDirectionAcceptsValidForward() {
        assertDoesNotThrow(() -> RequestValidationUtils.requireDirection("forward"));
    }

    @Test
    void requireDirectionRejectsInvalidValue() {
        assertThrows(BadRequestException.class, () -> RequestValidationUtils.requireDirection("sideways"));
    }

    @Test
    void requireNonBlankRejectsBlankValue() {
        assertThrows(BadRequestException.class, () -> RequestValidationUtils.requireNonBlank("   ", "busId"));
    }

    @Test
    void requireRoutePayloadRejectsMissingForwardStops() {
        Route route = new Route();
        route.setCode("10_A - B");
        route.setCompany("BusBuddy");
        ForwardBackStops stops = new ForwardBackStops();
        stops.setForwardStops(List.of());
        stops.setBackStops(List.of());
        route.setStops(stops);

        assertThrows(BadRequestException.class, () -> RequestValidationUtils.requireRoutePayload(route));
    }

    @Test
    void requireRoutePayloadAcceptsValidRoute() {
        Route route = new Route();
        route.setCode("10_A - B");
        route.setCompany("BusBuddy");
        ForwardBackStops stops = new ForwardBackStops();
        stops.setForwardStops(List.of(new Stop()));
        stops.setBackStops(List.of());
        route.setStops(stops);

        assertDoesNotThrow(() -> RequestValidationUtils.requireRoutePayload(route));
    }
}
