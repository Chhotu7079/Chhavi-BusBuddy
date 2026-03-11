package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.CompanySignupRequest;
import com.chhavi.busbuddy_backend.service.CompanyService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes company onboarding and lookup endpoints used by clients managing bus
 * operator accounts.
 */
@RestController
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/companies/signup")
    public ResponseEntity<String> signupCompany(@Valid @RequestBody CompanySignupRequest request) {
        return ResponseEntity.ok(companyService.signupCompany(request));
    }

    @GetMapping(value = "/companies", params = "email")
    public ResponseEntity<String> getCompanyByEmail(@RequestParam String email) {
        RequestValidationUtils.requireNonBlank(email, "email");
        return ResponseEntity.ok(companyService.getCompanyByEmail(email));
    }
}
