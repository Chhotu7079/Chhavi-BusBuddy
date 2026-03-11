package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.request.RegisterGpsDeviceRequest;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.exception.UnauthorizedException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.AuthenticatedUser;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GpsDeviceService {

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;

    public GpsDeviceService(FirebaseGateway firebaseGateway, AuthorizationService authorizationService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
    }

    /**
     * Registers or updates a GPS device mapping (imei -> busId) for a company.
     * Generates a per-device secret (used for HMAC/ingest authentication).
     */
    public Map<String, Object> register(String companyId, RegisterGpsDeviceRequest request) {
        requireOwnerOrAdmin(companyId);

        try {
            // Ensure bus exists and belongs to the company.
            DocumentSnapshot busDoc = firebaseGateway.collection(FirestoreCollections.BUSES)
                    .document(request.getBusId())
                    .get()
                    .get();
            if (!busDoc.exists()) {
                throw new ResourceNotFoundException("Bus not found: " + request.getBusId());
            }
            String busCompany = busDoc.getString("company");
            if (busCompany == null || !busCompany.equals(companyId)) {
                throw new UnauthorizedException("Bus does not belong to this company");
            }

            String secret = generateSecret();

            Map<String, Object> data = new HashMap<>();
            data.put("imei", request.getImei());
            data.put("busId", request.getBusId());
            data.put("companyId", companyId);
            data.put("active", true);
            data.put("secret", secret);

            firebaseGateway.collection(FirestoreCollections.GPS_DEVICES)
                    .document(request.getImei())
                    .set(data);

            // Return secret so the operator can configure the device.
            return Map.of(
                    "imei", request.getImei(),
                    "busId", request.getBusId(),
                    "companyId", companyId,
                    "active", true,
                    "secret", secret
            );
        } catch (ResourceNotFoundException | UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to register GPS device", exception);
        }
    }

    public List<Map<String, Object>> list(String companyId) {
        requireOwnerOrAdmin(companyId);

        try {
            QuerySnapshot snapshot = firebaseGateway.collection(FirestoreCollections.GPS_DEVICES)
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .get();

            return snapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> map = new HashMap<>(doc.getData());
                        map.put("imei", doc.getId());
                        // Do not return secret in list.
                        map.remove("secret");
                        return map;
                    })
                    .toList();
        } catch (Exception exception) {
            throw new ApplicationException("Unable to list GPS devices", exception);
        }
    }

    private void requireOwnerOrAdmin(String companyId) {
        AuthenticatedUser user = authorizationService.currentUser();
        if (user.getRole().name().equals("ADMIN")) {
            return;
        }
        if (user.getRole().name().equals("OWNER") && companyId != null && companyId.equals(user.getCompanyId())) {
            return;
        }
        throw new com.chhavi.busbuddy_backend.exception.ForbiddenException("Owner or admin access is required");
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
