package com.chhavi.busbuddy_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload used when a bus company account is created through the API.
 */
public class CompanySignupRequest {
    @NotBlank(message = "must not be blank")
    @Email(message = "must be a valid email")
    private String email;

    @NotBlank(message = "must not be blank")
    @Size(min = 8, message = "must be at least 8 characters")
    private String password;

    @NotBlank(message = "must not be blank")
    private String company;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
