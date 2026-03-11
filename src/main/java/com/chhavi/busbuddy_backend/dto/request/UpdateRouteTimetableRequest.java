package com.chhavi.busbuddy_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Updates a route timetable using minutes-from-start per stop.
 *
 * For each stop index (0..n-1), provide minutes as integer.
 */
public class UpdateRouteTimetableRequest {

    @NotNull
    private List<Integer> forwardMinutesFromStart;

    @NotNull
    private List<Integer> backMinutesFromStart;

    public List<Integer> getForwardMinutesFromStart() {
        return forwardMinutesFromStart;
    }

    public void setForwardMinutesFromStart(List<Integer> forwardMinutesFromStart) {
        this.forwardMinutesFromStart = forwardMinutesFromStart;
    }

    public List<Integer> getBackMinutesFromStart() {
        return backMinutesFromStart;
    }

    public void setBackMinutesFromStart(List<Integer> backMinutesFromStart) {
        this.backMinutesFromStart = backMinutesFromStart;
    }
}
