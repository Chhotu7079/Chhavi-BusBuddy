package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.AddRouteRequest;
import com.chhavi.busbuddy_backend.dto.response.RouteResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicRouteResponse;
import com.chhavi.busbuddy_backend.service.BusManagementService;
import com.chhavi.busbuddy_backend.service.RouteService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import com.chhavi.busbuddy_backend.util.ResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes route management and route analytics endpoints, including route
 * creation, lookup, deletion, and delay recalculation APIs.
 */
@RestController
public class RouteController {

    private final RouteService routeService;
    private final BusManagementService busManagementService;

    public RouteController(RouteService routeService,
                           BusManagementService busManagementService) {
        this.routeService = routeService;
        this.busManagementService = busManagementService;
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<PublicRouteResponse> getRoute(@PathVariable String id) {
        RequestValidationUtils.requireNonBlank(id, "id");
        return ResponseEntity.ok(ResponseMapper.toPublicRouteResponse(routeService.getRoute(id)));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<PublicRouteResponse>> getAllRoutes() {
        // Public-safe: do not leak company IDs as grouping keys.
        return ResponseEntity.ok(routeService.getAllRoutesGroupedByCompany().values().stream()
                .flatMap(List::stream)
                .map(ResponseMapper::toPublicRouteResponse)
                .toList());
    }

    @GetMapping(value = "/routes", params = "busCode")
    public ResponseEntity<List<PublicRouteResponse>> getRoutesByBusCode(@RequestParam String busCode) {
        RequestValidationUtils.requireNonBlank(busCode, "busCode");
        return ResponseEntity.ok(busManagementService.getRoutesByBusCode(busCode).stream()
                .map(ResponseMapper::toPublicRouteResponse)
                .toList());
    }

    @PostMapping("/routes")
    public ResponseEntity<Boolean> addRoute(@RequestBody AddRouteRequest request) {
        RequestValidationUtils.requireAddRoutePayload(request);
        return ResponseEntity.ok(routeService.addRoute(request));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Boolean> deleteRoute(@PathVariable String id) {
        RequestValidationUtils.requireNonBlank(id, "id");
        return ResponseEntity.ok(routeService.deleteRoute(id));
    }

    @PostMapping("/routes/{id}/delays/recompute")
    public ResponseEntity<Boolean> updateDelay(@PathVariable("id") String routeId) {
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        return ResponseEntity.ok(routeService.updateDelay(routeId));
    }

    @PostMapping("/routes/delays/recompute")
    public ResponseEntity<Boolean> updateAllDelays() {
        return ResponseEntity.ok(routeService.updateAllDelays());
    }
}
