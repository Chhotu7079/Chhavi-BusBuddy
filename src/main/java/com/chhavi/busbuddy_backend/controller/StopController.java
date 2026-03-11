package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.response.StopResponse;
import com.chhavi.busbuddy_backend.service.StopService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import com.chhavi.busbuddy_backend.util.ResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes stop-focused APIs such as stop lookup, nearby stop discovery, and
 * next-bus queries from the perspective of a stop.
 */
@RestController
public class StopController {

    private final StopService stopService;

    public StopController(StopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping("/stops/{id}")
    public ResponseEntity<StopResponse> getStop(@PathVariable String id) {
        RequestValidationUtils.requireNonBlank(id, "id");
        return ResponseEntity.ok(ResponseMapper.toStopResponse(stopService.getStop(id)));
    }

    @GetMapping(value = "/stops", params = {"latitude", "longitude", "radius"})
    public ResponseEntity<List<StopResponse>> getStopsWithinRadius(@RequestParam double latitude,
                                                                    @RequestParam double longitude,
                                                                    @RequestParam double radius) {
        if (radius <= 0) {
            throw new com.chhavi.busbuddy_backend.exception.BadRequestException("radius must be greater than 0");
        }
        return ResponseEntity.ok(stopService.getStopsWithinRadius(latitude, longitude, radius).stream()
                .map(ResponseMapper::toStopResponse)
                .toList());
    }

    @GetMapping("/stops/{id}/next-buses")
    public ResponseEntity<Map<String, List<String>>> getNextBusesByStop(@PathVariable("id") String stopId) {
        RequestValidationUtils.requireNonBlank(stopId, "stopId");
        return ResponseEntity.ok(stopService.getNextBusesByStop(stopId));
    }
}
