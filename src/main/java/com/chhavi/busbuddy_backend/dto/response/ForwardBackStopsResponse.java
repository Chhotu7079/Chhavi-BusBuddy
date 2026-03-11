package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;

/**
 * API response wrapper for the forward and return stop sequences of a route.
 */
public class ForwardBackStopsResponse {
    private List<StopResponse> forwardStops;
    private List<StopResponse> backStops;

    public List<StopResponse> getForwardStops() {
        return forwardStops;
    }

    public void setForwardStops(List<StopResponse> forwardStops) {
        this.forwardStops = forwardStops;
    }

    public List<StopResponse> getBackStops() {
        return backStops;
    }

    public void setBackStops(List<StopResponse> backStops) {
        this.backStops = backStops;
    }
}
