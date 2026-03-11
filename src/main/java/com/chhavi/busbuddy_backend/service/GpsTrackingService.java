package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.request.GpsLocationUpdateRequest;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.exception.UnauthorizedException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class GpsTrackingService {

    private final FirebaseGateway firebaseGateway;

    public GpsTrackingService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    /**
     * Validates device/vendor key, resolves IMEI -> busId, then updates RTDB live bus state.
     */
    public boolean ingest(String apiKey, String signature, GpsLocationUpdateRequest request) {
        validatePayload(request);

        try {
            DeviceMapping mapping = resolveMapping(request.getImei());
            validateAuth(apiKey, signature, mapping, request);

            DatabaseReference busRef = firebaseGateway.realtimeRoot().child("buses").child(mapping.busId());

            Map<String, Object> update = new HashMap<>();
            Map<String, Object> coords = new HashMap<>();
            coords.put("latitude", request.getLatitude());
            coords.put("longitude", request.getLongitude());
            update.put("coords", coords);

            if (request.getSpeed() != null) {
                update.put("speed", request.getSpeed());
            }
            if (request.getDirection() != null) {
                update.put("direction", request.getDirection());
            }
            if (request.getLastStop() != null) {
                update.put("lastStop", request.getLastStop());
            }

            update.put("status", "RUNNING");
            update.put("lastUpdatedAt", System.currentTimeMillis());
            if (request.getDeviceTimestamp() != null) {
                update.put("deviceTimestamp", request.getDeviceTimestamp());
            }

            // Fire-and-forget is ok for GPS ingestion; failures should be visible via logs.
            busRef.updateChildrenAsync(update);

            // Keep route -> buses index present for live lookups.
            // If bus node already has routeId, use it to ensure index. (best-effort)
            try {
                String existingRouteId = readRouteId(busRef);
                if (existingRouteId != null && !existingRouteId.isBlank()) {
                    firebaseGateway.realtimeRoot().child("routeBuses")
                            .child(existingRouteId)
                            .child(mapping.busId())
                            .setValueAsync(true);
                }
            } catch (Exception ignored) {
                // ignore index repair failure
            }

            return true;
        } catch (ResourceNotFoundException | UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to ingest GPS update", exception);
        }
    }

    private void validateAuth(String apiKey, String signature, DeviceMapping mapping, GpsLocationUpdateRequest request) {
        // Preferred: per-device HMAC signature using secret stored in gps_devices/{imei}.secret
        if (signature != null && !signature.isBlank()) {
            if (request.getDeviceTimestamp() == null) {
                throw new UnauthorizedException("deviceTimestamp is required when using signature auth");
            }
            String payload = signingPayload(mapping.imei(), request);
            String expected = hmacSha256Base64Url(mapping.secret(), payload);
            if (!constantTimeEquals(expected, signature)) {
                throw new UnauthorizedException("Invalid GPS signature");
            }
            // basic replay protection: require deviceTimestamp within window
            validateTimestampWindow(request.getDeviceTimestamp());
            return;
        }

        // Backwards compatible: global shared key
        String expectedGlobal = System.getenv("GPS_INGEST_API_KEY");
        if (expectedGlobal == null || expectedGlobal.isBlank()) {
            throw new ApplicationException("GPS ingestion is not configured: missing GPS_INGEST_API_KEY env var");
        }
        if (apiKey == null || apiKey.isBlank() || !expectedGlobal.equals(apiKey)) {
            throw new UnauthorizedException("Invalid GPS ingest key");
        }
    }

    private void validatePayload(GpsLocationUpdateRequest request) {
        if (request == null) {
            throw new ApplicationException("GPS payload must not be null");
        }
        if (request.getImei() == null || request.getImei().isBlank()) {
            throw new ApplicationException("imei must not be blank");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new ApplicationException("latitude and longitude are required");
        }
    }

    private DeviceMapping resolveMapping(String imei) {
        try {
            DocumentSnapshot snapshot = firebaseGateway.collection(FirestoreCollections.GPS_DEVICES)
                    .document(imei)
                    .get()
                    .get();
            if (!snapshot.exists()) {
                throw new ResourceNotFoundException("GPS device not registered: " + imei);
            }
            String busId = snapshot.getString("busId");
            if (busId == null || busId.isBlank()) {
                throw new ApplicationException("gps_devices." + imei + " is missing busId");
            }
            Boolean active = snapshot.getBoolean("active");
            if (active != null && !active) {
                throw new UnauthorizedException("GPS device is disabled");
            }
            String secret = snapshot.getString("secret");
            if (secret == null || secret.isBlank()) {
                throw new ApplicationException("gps_devices." + imei + " is missing secret");
            }
            return new DeviceMapping(imei, busId, secret);
        } catch (ResourceNotFoundException | UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to resolve GPS device mapping", exception);
        }
    }

    private record DeviceMapping(String imei, String busId, String secret) {
    }

    private String signingPayload(String imei, GpsLocationUpdateRequest request) {
        // Replay-safe payload includes timestamp + coordinates.
        // If you want stronger protection, add a nonce and persist last nonce per IMEI.
        return imei + "|" + request.getDeviceTimestamp() + "|" + request.getLatitude() + "|" + request.getLongitude();
    }

    private void validateTimestampWindow(Long deviceTimestamp) {
        if (deviceTimestamp == null) {
            throw new UnauthorizedException("deviceTimestamp is required");
        }
        long now = System.currentTimeMillis();
        long windowMs = gpsTimestampWindowMs();
        if (Math.abs(now - deviceTimestamp) > windowMs) {
            throw new UnauthorizedException("GPS timestamp is outside allowed window");
        }
    }

    private long gpsTimestampWindowMs() {
        String raw = System.getenv("GPS_TIMESTAMP_WINDOW_SEC");
        if (raw == null || raw.isBlank()) {
            return 120_000L;
        }
        try {
            long sec = Long.parseLong(raw.trim());
            if (sec < 1) {
                sec = 1;
            }
            return sec * 1000L;
        } catch (Exception exception) {
            return 120_000L;
        }
    }

    private String hmacSha256Base64Url(String secret, String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception exception) {
            throw new ApplicationException("Unable to compute HMAC", exception);
        }
    }

    private String readRouteId(DatabaseReference busRef) {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] holder = new String[1];

        busRef.child("routeId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                holder[0] = value == null ? null : String.valueOf(value);
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        return holder[0];
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
