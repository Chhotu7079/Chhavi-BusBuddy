package com.chhavi.busbuddy_backend.security;

import com.chhavi.busbuddy_backend.constant.UserRole;

public class AuthenticatedUser {
    private final String subject;
    private final UserRole role;
    private final String companyId;

    public AuthenticatedUser(String subject, UserRole role, String companyId) {
        this.subject = subject;
        this.role = role;
        this.companyId = companyId;
    }

    public String getSubject() {
        return subject;
    }

    public UserRole getRole() {
        return role;
    }

    public String getCompanyId() {
        return companyId;
    }
}
