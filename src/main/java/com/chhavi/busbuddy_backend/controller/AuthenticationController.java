package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.auth.FirebaseLoginRequest;
import com.chhavi.busbuddy_backend.dto.auth.LoginResponse;
import com.chhavi.busbuddy_backend.service.AuthenticationService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes authentication-related endpoints used by clients that need backend-
 * issued JWTs on top of Firebase identity verification.
 */
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/auth/company/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseEntity.ok(authenticationService.loginWithFirebaseIdToken(request.getFirebaseIdToken()));
    }

    @GetMapping("/verify-custom-token")
    public ResponseEntity<Boolean> verifyCustomToken(@RequestHeader("Authorization") String token) {
        RequestValidationUtils.requireNonBlank(token, "Authorization");
        return ResponseEntity.ok(authenticationService.verifyCustomToken(token));
    }
}
