package com.chhavi.busbuddy_backend.dto.response;

import java.util.List;
import java.util.Map;

/**
 * API response representing a route, including stop lists and schedule-related
 * views exposed to clients.
 */
public class RouteResponse {
    private String id;
    private String company;
    private String code;
    private Map<String, List<String>> timetableForward;
    private Map<String, List<String>> timetableBack;
    private Map<String, Map<String, List<String>>> history;
    private Map<String, List<String>> delaysForward;
    private Map<String, List<String>> delaysBack;
    private ForwardBackStopsResponse stops;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, List<String>> getTimetableForward() {
        return timetableForward;
    }

    public void setTimetableForward(Map<String, List<String>> timetableForward) {
        this.timetableForward = timetableForward;
    }

    public Map<String, List<String>> getTimetableBack() {
        return timetableBack;
    }

    public void setTimetableBack(Map<String, List<String>> timetableBack) {
        this.timetableBack = timetableBack;
    }

    public Map<String, Map<String, List<String>>> getHistory() {
        return history;
    }

    public void setHistory(Map<String, Map<String, List<String>>> history) {
        this.history = history;
    }

    public Map<String, List<String>> getDelaysForward() {
        return delaysForward;
    }

    public void setDelaysForward(Map<String, List<String>> delaysForward) {
        this.delaysForward = delaysForward;
    }

    public Map<String, List<String>> getDelaysBack() {
        return delaysBack;
    }

    public void setDelaysBack(Map<String, List<String>> delaysBack) {
        this.delaysBack = delaysBack;
    }

    public ForwardBackStopsResponse getStops() {
        return stops;
    }

    public void setStops(ForwardBackStopsResponse stops) {
        this.stops = stops;
    }
}
