package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.service.MigrationService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Admin-only endpoint to migrate legacy company UID stored in resources to the new stable companyId.
     *
     * Example:
     * POST /admin/migrations/company-id?legacyCompanyUid=<oldUid>&newCompanyId=<companyId>
     */
    @PostMapping("/admin/migrations/company-id")
    public ResponseEntity<Integer> migrateCompanyId(@RequestParam String legacyCompanyUid,
                                                    @RequestParam String newCompanyId) {
        RequestValidationUtils.requireNonBlank(legacyCompanyUid, "legacyCompanyUid");
        RequestValidationUtils.requireNonBlank(newCompanyId, "newCompanyId");
        return ResponseEntity.ok(migrationService.migrateCompanyUidToCompanyId(legacyCompanyUid, newCompanyId));
    }

    /**
     * Backfill Firestore searchKey fields for routes and buses.
     */
    @PostMapping("/admin/migrations/search-keys")
    public ResponseEntity<Integer> backfillSearchKeys() {
        return ResponseEntity.ok(migrationService.backfillSearchKeys());
    }
}
