package com.chhavi.busbuddy_backend.persistence.model;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * Firestore representation of an employee-to-company mapping.
 */
public class Employee {

    @DocumentId
    private String uid;

    private String companyId;

    /**
     * OWNER / EMPLOYEE / ADMIN (ADMIN is global).
     */
    private String role;

    private String email;

    private String name;

    private Boolean active;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
