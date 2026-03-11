package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.response.BusSearchLiveResponse;
import com.chhavi.busbuddy_backend.service.BusSearchLiveService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BusSearchLiveController {

    private final BusSearchLiveService busSearchLiveService;

    public BusSearchLiveController(BusSearchLiveService busSearchLiveService) {
        this.busSearchLiveService = busSearchLiveService;
    }

    /**
     * One-call search that returns bus matches with current live coordinates.
     * Example: GET /buses/search-live?query=BUS-101
     */
    @GetMapping(value = "/buses/search-live", params = "query")
    public ResponseEntity<List<BusSearchLiveResponse>> searchLive(@RequestParam String query) {
        RequestValidationUtils.requireNonBlank(query, "query");
        return ResponseEntity.ok(busSearchLiveService.searchLive(query));
    }
}
