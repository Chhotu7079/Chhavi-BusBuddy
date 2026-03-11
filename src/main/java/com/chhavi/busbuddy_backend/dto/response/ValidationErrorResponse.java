package com.chhavi.busbuddy_backend.dto.response;

import java.util.Map;

/**
 * Error payload used when request validation fails and field-level details are
 * needed by API consumers.
 */
public class ValidationErrorResponse extends ApiErrorResponse {
    private Map<String, String> validationErrors;

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
