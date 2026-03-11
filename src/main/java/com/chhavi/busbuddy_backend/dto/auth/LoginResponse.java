package com.chhavi.busbuddy_backend.dto.auth;

public class LoginResponse {

    private String token;
    private String role;
    private String companyId;

    public LoginResponse() {
    }

    public LoginResponse(String token, String role, String companyId) {
        this.token = token;
        this.role = role;
        this.companyId = companyId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
}
