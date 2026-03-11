package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.response.BusSearchResponse;
import com.chhavi.busbuddy_backend.service.BusSearchService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BusSearchController {

    private final BusSearchService busSearchService;

    public BusSearchController(BusSearchService busSearchService) {
        this.busSearchService = busSearchService;
    }

    /**
     * Search buses by partial bus code/name.
     * Example: GET /buses/search?query=101
     */
    @GetMapping(value = "/buses/search", params = "query")
    public ResponseEntity<List<BusSearchResponse>> search(@RequestParam String query) {
        RequestValidationUtils.requireNonBlank(query, "query");
        return ResponseEntity.ok(busSearchService.search(query));
    }
}
