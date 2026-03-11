package com.chhavi.busbuddy_backend;

import com.chhavi.busbuddy_backend.constant.RouteDirection;
import com.chhavi.busbuddy_backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteDirectionTests {

    @Test
    void shouldParseForwardDirection() {
        assertEquals(RouteDirection.FORWARD, RouteDirection.from("forward"));
    }

    @Test
    void shouldParseBackDirectionCaseInsensitively() {
        assertEquals(RouteDirection.BACK, RouteDirection.from("BACK"));
    }

    @Test
    void shouldRejectInvalidDirection() {
        assertThrows(BadRequestException.class, () -> RouteDirection.from("left"));
    }
}
