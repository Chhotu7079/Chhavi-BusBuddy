package com.chhavi.busbuddy_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class FirebaseLoginRequest {

    @NotBlank(message = "must not be blank")
    private String firebaseIdToken;

    public String getFirebaseIdToken() {
        return firebaseIdToken;
    }

    public void setFirebaseIdToken(String firebaseIdToken) {
        this.firebaseIdToken = firebaseIdToken;
    }
}
