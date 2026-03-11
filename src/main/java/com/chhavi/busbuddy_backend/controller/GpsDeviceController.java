package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.RegisterGpsDeviceRequest;
import com.chhavi.busbuddy_backend.service.GpsDeviceService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class GpsDeviceController {

    private final GpsDeviceService gpsDeviceService;

    public GpsDeviceController(GpsDeviceService gpsDeviceService) {
        this.gpsDeviceService = gpsDeviceService;
    }

    /**
     * Register GPS device (IMEI) to a bus. Returns the per-device secret.
     */
    @PostMapping("/companies/{companyId}/gps-devices")
    public ResponseEntity<Map<String, Object>> register(@PathVariable String companyId,
                                                        @Valid @RequestBody RegisterGpsDeviceRequest request) {
        RequestValidationUtils.requireNonBlank(companyId, "companyId");
        return ResponseEntity.ok(gpsDeviceService.register(companyId, request));
    }

    /**
     * List GPS devices for a company (does not return secrets).
     */
    @GetMapping("/companies/{companyId}/gps-devices")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String companyId) {
        RequestValidationUtils.requireNonBlank(companyId, "companyId");
        return ResponseEntity.ok(gpsDeviceService.list(companyId));
    }
}
