package com.chhavi.busbuddy_backend.constant;

import com.chhavi.busbuddy_backend.exception.UnauthorizedException;

/**
 * Company-scoped roles.
 */
public enum EmployeeRole {
    OWNER,
    EMPLOYEE;

    public static EmployeeRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException("Missing employee role");
        }
        try {
            return EmployeeRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Unsupported employee role: " + value);
        }
    }
}
