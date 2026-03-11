package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.response.RouteLiveResponse;
import com.chhavi.busbuddy_backend.service.LiveTrackingService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouteLiveController {

    private final LiveTrackingService liveTrackingService;

    public RouteLiveController(LiveTrackingService liveTrackingService) {
        this.liveTrackingService = liveTrackingService;
    }

    /**
     * Public endpoint that returns active buses (live positions) for a route.
     */
    @GetMapping("/routes/{id}/live")
    public ResponseEntity<RouteLiveResponse> getRouteLive(@PathVariable("id") String routeId) {
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        return ResponseEntity.ok(liveTrackingService.getRouteLive(routeId));
    }
}
