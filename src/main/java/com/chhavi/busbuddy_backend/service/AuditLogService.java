package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditLogService {

    private final FirebaseGateway firebaseGateway;

    public AuditLogService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    public void log(AuthenticatedUser actor,
                    String companyId,
                    String action,
                    String resourceType,
                    String resourceId,
                    Map<String, Object> details) {
        try {
            Map<String, Object> entry = new HashMap<>();
            entry.put("timestamp", Instant.now().toString());
            entry.put("companyId", companyId);
            entry.put("actorUid", actor.getSubject());
            entry.put("actorRole", actor.getRole().name());
            entry.put("action", action);
            entry.put("resourceType", resourceType);
            entry.put("resourceId", resourceId);
            if (details != null && !details.isEmpty()) {
                entry.put("details", details);
            }

            firebaseGateway.collection(FirestoreCollections.AUDIT_LOGS).add(entry);
        } catch (Exception ignored) {
            // Audit logging must never break business operations.
        }
    }
}
