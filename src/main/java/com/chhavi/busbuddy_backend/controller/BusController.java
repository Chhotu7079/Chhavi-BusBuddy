package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.constant.RouteDirection;
import com.chhavi.busbuddy_backend.dto.response.BusResponse;
import com.chhavi.busbuddy_backend.dto.response.ForwardBackStopsResponse;
import com.chhavi.busbuddy_backend.dto.response.RouteResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicBusResponse;
import com.chhavi.busbuddy_backend.service.BusDelayService;
import com.chhavi.busbuddy_backend.service.BusHistoryService;
import com.chhavi.busbuddy_backend.service.BusManagementService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import com.chhavi.busbuddy_backend.util.ResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes bus-related operational APIs, including bus lookup, route assignment,
 * stop progression updates, arrival queries, and delay calculations.
 */
@RestController
public class BusController {

    private final BusManagementService busManagementService;
    private final BusHistoryService busHistoryService;
    private final BusDelayService busDelayService;

    public BusController(BusManagementService busManagementService,
                         BusHistoryService busHistoryService,
                         BusDelayService busDelayService) {
        this.busManagementService = busManagementService;
        this.busHistoryService = busHistoryService;
        this.busDelayService = busDelayService;
    }

    @GetMapping("/buses/{id}")
    public ResponseEntity<PublicBusResponse> getBus(@PathVariable String id) {
        RequestValidationUtils.requireNonBlank(id, "id");
        return ResponseEntity.ok(ResponseMapper.toPublicBusResponse(busManagementService.getBus(id)));
    }

    @GetMapping(value = "/buses", params = "code")
    public ResponseEntity<PublicBusResponse> getBusByCode(@RequestParam String code) {
        RequestValidationUtils.requireNonBlank(code, "code");
        return ResponseEntity.ok(ResponseMapper.toPublicBusResponse(busManagementService.getBusByCode(code)));
    }

    @GetMapping("/buses/{id}/stops")
    public ResponseEntity<ForwardBackStopsResponse> getStopsByBus(@PathVariable("id") String busId) {
        RequestValidationUtils.requireNonBlank(busId, "busId");
        return ResponseEntity.ok(ResponseMapper.toForwardBackStopsResponse(busManagementService.getStopsByBus(busId)));
    }

    @GetMapping("/buses/{id}/arrivals")
    public ResponseEntity<Map<String, List<String>>> getNextArrivalsByBus(@PathVariable("id") String busId,
                                                                          @RequestParam String direction) {
        RequestValidationUtils.requireNonBlank(busId, "busId");
        RouteDirection routeDirection = RequestValidationUtils.requireDirection(direction);
        return ResponseEntity.ok(busHistoryService.getNextArrivalsByBus(busId, routeDirection));
    }

    @PostMapping("/routes/{id}/stops/reached")
    public ResponseEntity<Boolean> updateStopReached(@PathVariable("id") String routeId,
                                                     @RequestParam String stopIndex,
                                                     @RequestParam String direction) {
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        RequestValidationUtils.requireNonBlank(stopIndex, "stopIndex");
        RouteDirection routeDirection = RequestValidationUtils.requireDirection(direction);
        return ResponseEntity.ok(busHistoryService.updateStopReached(routeId, stopIndex, routeDirection));
    }

    @PostMapping("/routes/{id}/history/fix-gaps")
    public ResponseEntity<Boolean> fixHistoryGaps(@PathVariable("id") String routeId,
                                                  @RequestParam String direction) {
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        RouteDirection routeDirection = RequestValidationUtils.requireDirection(direction);
        return ResponseEntity.ok(busHistoryService.fixHistoryGaps(routeId, routeDirection));
    }

    @PostMapping("/buses")
    public ResponseEntity<Boolean> addBus(@RequestParam String busCode, @RequestParam String routeId) {
        RequestValidationUtils.requireNonBlank(busCode, "busCode");
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        return ResponseEntity.ok(busManagementService.addBus(busCode, routeId));
    }

    @PutMapping("/buses/{busCode}/route")
    public ResponseEntity<Boolean> updateBusRoute(@PathVariable String busCode, @RequestParam String routeId) {
        RequestValidationUtils.requireNonBlank(busCode, "busCode");
        RequestValidationUtils.requireNonBlank(routeId, "routeId");
        return ResponseEntity.ok(busManagementService.updateBusRoute(busCode, routeId));
    }

    @GetMapping("/buses/{id}/delays/average")
    public ResponseEntity<Integer> getAverageBusDelay(@PathVariable("id") String busId,
                                                      @RequestParam String direction) {
        RequestValidationUtils.requireNonBlank(busId, "busId");
        RouteDirection routeDirection = RequestValidationUtils.requireDirection(direction);
        return ResponseEntity.ok(busDelayService.getAverageBusDelay(busId, routeDirection));
    }

    @GetMapping("/buses/{id}/delays/current")
    public ResponseEntity<Integer> getCurrentDelay(@PathVariable("id") String busId,
                                                   @RequestParam String direction) {
        RequestValidationUtils.requireNonBlank(busId, "busId");
        RouteDirection routeDirection = RequestValidationUtils.requireDirection(direction);
        return ResponseEntity.ok(busDelayService.getCurrentDelay(busId, routeDirection));
    }
}
