package com.chhavi.busbuddy_backend.constant;

import com.chhavi.busbuddy_backend.exception.BadRequestException;

public enum RouteDirection {
    FORWARD("forward"),
    BACK("back");

    private final String value;

    RouteDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RouteDirection from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("direction must not be blank");
        }
        for (RouteDirection direction : values()) {
            if (direction.value.equalsIgnoreCase(value.trim())) {
                return direction;
            }
        }
        throw new BadRequestException("direction must be either 'forward' or 'back'");
    }
}
