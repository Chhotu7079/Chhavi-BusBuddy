package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.UpdateRouteTimetableRequest;
import com.chhavi.busbuddy_backend.service.RouteService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouteTimetableController {

    private final RouteService routeService;

    public RouteTimetableController(RouteService routeService) {
        this.routeService = routeService;
    }

    /**
     * Updates timetable using minutes-from-start per stop index.
     */
    @PostMapping("/routes/{id}/timetable/minutes")
    public ResponseEntity<Boolean> updateTimetableMinutes(@PathVariable("id") String routeId,
                                                          @Valid @RequestBody UpdateRouteTimetableRequest request) {
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        return ResponseEntity.ok(routeService.updateTimetableMinutes(routeId, request));
    }
}
