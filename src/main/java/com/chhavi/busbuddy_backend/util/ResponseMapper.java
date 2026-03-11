package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.dto.response.BusResponse;
import com.chhavi.busbuddy_backend.dto.response.CoordinatesResponse;
import com.chhavi.busbuddy_backend.dto.response.ForwardBackStopsResponse;
import com.chhavi.busbuddy_backend.dto.response.RouteResponse;
import com.chhavi.busbuddy_backend.dto.response.StopResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicBusResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicRouteSummaryResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicRouteResponse;
import com.chhavi.busbuddy_backend.dto.response.PublicStopSummaryResponse;
import com.chhavi.busbuddy_backend.persistence.model.Bus;
import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.chhavi.busbuddy_backend.persistence.model.Stop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralizes conversion from internal models to API response DTOs so response
 * shaping stays consistent across controllers.
 */
public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static BusResponse toBusResponse(Bus bus) {
        BusResponse response = new BusResponse();
        response.setId(bus.getId());
        response.setCode(bus.getCode());
        response.setLastStop(bus.getLastStop());
        response.setSpeed(bus.getSpeed());
        if (bus.getCoords() != null) {
            response.setCoords(new CoordinatesResponse(bus.getCoords().getLatitude(), bus.getCoords().getLongitude()));
        }
        if (bus.getRoute() != null) {
            response.setRoute(toRouteResponse(bus.getRoute()));
        }
        return response;
    }

    public static PublicRouteResponse toPublicRouteResponse(Route route) {
        PublicRouteResponse response = new PublicRouteResponse();
        response.setId(route.getId());
        response.setCode(route.getCode());

        if (route.getStops() != null) {
            // Flatten forward + back stops into a single list for public display.
            List<Stop> forward = route.getStops().getForwardStops();
            List<Stop> back = route.getStops().getBackStops();
            List<Stop> combined = new java.util.ArrayList<>();
            if (forward != null) {
                combined.addAll(forward);
            }
            if (back != null) {
                combined.addAll(back);
            }

            response.setStops(combined.stream()
                    .filter(s -> s != null && s.getName() != null)
                    .map(s -> new PublicStopSummaryResponse(
                            s.getName(),
                            s.getCoords() == null ? null : new CoordinatesResponse(s.getCoords().getLatitude(), s.getCoords().getLongitude())))
                    .toList());
        }

        return response;
    }

    public static PublicBusResponse toPublicBusResponse(Bus bus) {
        PublicBusResponse response = new PublicBusResponse();
        response.setId(bus.getId());
        response.setCode(bus.getCode());
        if (bus.getRoute() != null) {
            response.setRoute(new PublicRouteSummaryResponse(bus.getRoute().getId(), bus.getRoute().getCode()));
        }
        return response;
    }

    public static StopResponse toStopResponse(Stop stop) {
        StopResponse response = new StopResponse();
        response.setId(stop.getId());
        response.setName(stop.getName());
        response.setAddress(stop.getAddress());
        if (stop.getCoords() != null) {
            response.setCoords(new CoordinatesResponse(stop.getCoords().getLatitude(), stop.getCoords().getLongitude()));
        }
        return response;
    }

    public static ForwardBackStopsResponse toForwardBackStopsResponse(ForwardBackStops stops) {
        ForwardBackStopsResponse response = new ForwardBackStopsResponse();
        response.setForwardStops(mapStops(stops.getForwardStops()));
        response.setBackStops(mapStops(stops.getBackStops()));
        return response;
    }

    public static RouteResponse toRouteResponse(Route route) {
        RouteResponse response = new RouteResponse();
        response.setId(route.getId());
        response.setCompany(route.getCompany());
        response.setCode(route.getCode());
        response.setStops(route.getStops() == null ? null : toForwardBackStopsResponse(route.getStops()));
        if (route.getTimetable() != null) {
            response.setTimetableForward(route.getTimetable().getForward());
            response.setTimetableBack(route.getTimetable().getBack());
        }
        if (route.getDelays() != null) {
            response.setDelaysForward(route.getDelays().getForward());
            response.setDelaysBack(route.getDelays().getBack());
        }
        if (route.getHistory() != null) {
            response.setHistory(mapHistory(route.getHistory()));
        }
        return response;
    }

    private static List<StopResponse> mapStops(List<Stop> stops) {
        if (stops == null) {
            return Collections.emptyList();
        }
        return stops.stream().map(ResponseMapper::toStopResponse).collect(Collectors.toList());
    }

    private static Map<String, Map<String, List<String>>> mapHistory(Map<String, Schedule> history) {
        Map<String, Map<String, List<String>>> mapped = new HashMap<>();
        for (Map.Entry<String, Schedule> entry : history.entrySet()) {
            Map<String, List<String>> flattened = new HashMap<>();
            if (entry.getValue().getForward() != null) {
                flattened.putAll(prefixKeys("forward.", entry.getValue().getForward()));
            }
            if (entry.getValue().getBack() != null) {
                flattened.putAll(prefixKeys("back.", entry.getValue().getBack()));
            }
            mapped.put(entry.getKey(), flattened);
        }
        return mapped;
    }

    private static Map<String, List<String>> prefixKeys(String prefix, Map<String, List<String>> values) {
        Map<String, List<String>> prefixed = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            prefixed.put(prefix + entry.getKey(), entry.getValue());
        }
        return prefixed;
    }
}
