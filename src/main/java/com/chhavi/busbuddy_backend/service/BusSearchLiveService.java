package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.dto.response.BusLiveResponse;
import com.chhavi.busbuddy_backend.dto.response.BusSearchLiveResponse;
import com.chhavi.busbuddy_backend.dto.response.BusSearchResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusSearchLiveService {

    private final BusSearchService busSearchService;
    private final LiveTrackingService liveTrackingService;

    public BusSearchLiveService(BusSearchService busSearchService, LiveTrackingService liveTrackingService) {
        this.busSearchService = busSearchService;
        this.liveTrackingService = liveTrackingService;
    }

    public List<BusSearchLiveResponse> searchLive(String query) {
        List<BusSearchResponse> matches = busSearchService.search(query);
        return matches.stream()
                .map(match -> {
                    BusLiveResponse live = liveTrackingService.getBusLive(match.getBusId(), match.getBusCode());
                    return new BusSearchLiveResponse(
                            match.getBusId(),
                            match.getBusCode(),
                            match.getRouteId(),
                            match.getRouteCode(),
                            live);
                })
                .toList();
    }
}
