package com.chhavi.busbuddy_backend.constant;

import com.chhavi.busbuddy_backend.exception.UnauthorizedException;

public enum UserRole {
    ADMIN,
    OWNER,
    EMPLOYEE;

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException("Missing user role");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Unsupported user role: " + value);
        }
    }
}
