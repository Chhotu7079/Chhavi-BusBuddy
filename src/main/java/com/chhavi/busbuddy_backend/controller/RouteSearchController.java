package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.response.RouteSearchLiveResponse;
import com.chhavi.busbuddy_backend.dto.response.RouteSearchResponse;
import com.chhavi.busbuddy_backend.service.RouteSearchService;
import com.chhavi.busbuddy_backend.util.RequestValidationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RouteSearchController {

    private final RouteSearchService routeSearchService;

    public RouteSearchController(RouteSearchService routeSearchService) {
        this.routeSearchService = routeSearchService;
    }

    /**
     * Where-is-my-train style route search.
     * Example: GET /routes/search?from=Hajipur&to=Patna
     */
    @GetMapping(value = "/routes/search", params = {"from", "to"})
    public ResponseEntity<List<RouteSearchResponse>> search(@RequestParam String from, @RequestParam String to) {
        RequestValidationUtils.requireNonBlank(from, "from");
        RequestValidationUtils.requireNonBlank(to, "to");
        return ResponseEntity.ok(routeSearchService.search(from, to));
    }

    /**
     * Combined search + live result in one call.
     * Example: GET /routes/search-live?from=Hajipur&to=Patna
     */
    @GetMapping(value = "/routes/search-live", params = {"from", "to"})
    public ResponseEntity<List<RouteSearchLiveResponse>> searchLive(@RequestParam String from, @RequestParam String to) {
        RequestValidationUtils.requireNonBlank(from, "from");
        RequestValidationUtils.requireNonBlank(to, "to");
        return ResponseEntity.ok(routeSearchService.searchLive(from, to));
    }
}
