package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.AuthenticatedUser;
import com.chhavi.busbuddy_backend.constant.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuditLogController {

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;

    public AuditLogController(FirebaseGateway firebaseGateway, AuthorizationService authorizationService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/companies/{companyId}/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs(@PathVariable String companyId,
                                                                  @RequestParam(required = false) String actorUid,
                                                                  @RequestParam(required = false) String action,
                                                                  @RequestParam(required = false) String resourceId) {
        requireOwnerOrAdmin(companyId);

        try {
            var query = firebaseGateway.collection(FirestoreCollections.AUDIT_LOGS)
                    .whereEqualTo("companyId", companyId);

            if (actorUid != null && !actorUid.isBlank()) {
                query = query.whereEqualTo("actorUid", actorUid);
            }
            if (action != null && !action.isBlank()) {
                query = query.whereEqualTo("action", action);
            }
            if (resourceId != null && !resourceId.isBlank()) {
                query = query.whereEqualTo("resourceId", resourceId);
            }

            var snapshot = query.get().get();
            return ResponseEntity.ok(snapshot.getDocuments().stream().map(doc -> {
                Map<String, Object> map = new HashMap<>(doc.getData());
                map.put("id", doc.getId());
                return map;
            }).toList());
        } catch (Exception exception) {
            throw new ApplicationException("Unable to load audit logs", exception);
        }
    }

    private void requireOwnerOrAdmin(String companyId) {
        AuthenticatedUser user = authorizationService.currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.OWNER && companyId != null && companyId.equals(user.getCompanyId())) {
            return;
        }
        throw new com.chhavi.busbuddy_backend.exception.ForbiddenException("Owner or admin access is required");
    }
}
