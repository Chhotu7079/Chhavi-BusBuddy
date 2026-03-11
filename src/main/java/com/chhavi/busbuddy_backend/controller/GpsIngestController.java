package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.GpsLocationUpdateRequest;
import com.chhavi.busbuddy_backend.service.GpsTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * GPS ingestion endpoints.
 *
 * Supports:
 * - Direct device -> backend
 * - Vendor webhook -> backend
 */
@RestController
public class GpsIngestController {

    private final GpsTrackingService gpsTrackingService;

    public GpsIngestController(GpsTrackingService gpsTrackingService) {
        this.gpsTrackingService = gpsTrackingService;
    }

    /**
     * Direct device endpoint.
     */
    @PostMapping("/gps/ingest")
    public ResponseEntity<Boolean> ingestFromDevice(@RequestHeader(value = "X-GPS-KEY", required = false) String apiKey,
                                                    @RequestHeader(value = "X-GPS-SIGNATURE", required = false) String signature,
                                                    @Valid @RequestBody GpsLocationUpdateRequest request) {
        return ResponseEntity.ok(gpsTrackingService.ingest(apiKey, signature, request));
    }

    /**
     * Vendor webhook endpoint (same payload shape for simplicity).
     */
    @PostMapping("/webhooks/gps")
    public ResponseEntity<Boolean> ingestFromVendor(@RequestHeader(value = "X-GPS-KEY", required = false) String apiKey,
                                                    @RequestHeader(value = "X-GPS-SIGNATURE", required = false) String signature,
                                                    @Valid @RequestBody GpsLocationUpdateRequest request) {
        return ResponseEntity.ok(gpsTrackingService.ingest(apiKey, signature, request));
    }
}
