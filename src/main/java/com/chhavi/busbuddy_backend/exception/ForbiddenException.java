package com.chhavi.busbuddy_backend.exception;

/**
 * Thrown when the caller is authenticated but does not have permission to
 * perform the requested operation.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
