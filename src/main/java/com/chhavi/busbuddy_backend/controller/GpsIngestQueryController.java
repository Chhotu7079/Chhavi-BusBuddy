package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.GpsLocationUpdateRequest;
import com.chhavi.busbuddy_backend.service.GpsTrackingService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alternate GPS ingestion endpoint that accepts common query-parameter formats.
 *
 * Useful for simple devices/vendors that cannot send JSON bodies.
 */
@RestController
public class GpsIngestQueryController {

    private final GpsTrackingService gpsTrackingService;

    public GpsIngestQueryController(GpsTrackingService gpsTrackingService) {
        this.gpsTrackingService = gpsTrackingService;
    }

    /**
     * Example:
     * POST /gps/ingest/query?imei=123&lat=12.9&lng=77.5&speed=20&direction=forward&lastStop=3&ts=1710000000000
     */
    @PostMapping("/gps/ingest/query")
    public ResponseEntity<Boolean> ingestQuery(@RequestHeader(value = "X-GPS-KEY", required = false) String apiKey,
                                               @RequestHeader(value = "X-GPS-SIGNATURE", required = false) String signature,
                                               @RequestParam String imei,
                                               @RequestParam(name = "lat") double latitude,
                                               @RequestParam(name = "lng") double longitude,
                                               @RequestParam(required = false) Double speed,
                                               @RequestParam(required = false) String direction,
                                               @RequestParam(required = false) Integer lastStop,
                                               @RequestParam(name = "ts", required = false) Long deviceTimestamp) {

        RequestValidationUtils.requireNonBlank(imei, "imei");

        GpsLocationUpdateRequest request = new GpsLocationUpdateRequest();
        request.setImei(imei);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setSpeed(speed);
        request.setDirection(direction);
        request.setLastStop(lastStop);
        request.setDeviceTimestamp(deviceTimestamp);

        return ResponseEntity.ok(gpsTrackingService.ingest(apiKey, signature, request));
    }
}
