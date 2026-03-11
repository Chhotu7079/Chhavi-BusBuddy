package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.chhavi.busbuddy_backend.persistence.model.Stop;
import com.chhavi.busbuddy_backend.util.DateTimeProvider;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.chhavi.busbuddy_backend.util.ScheduleUtils;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides stop-centric read operations, especially the views needed by users
 * who start from a stop and want nearby stops or upcoming buses.
 */
@Service
public class StopService {

    private final FirebaseGateway firebaseGateway;
    private final DateTimeProvider dateTimeProvider;

    public StopService(FirebaseGateway firebaseGateway, DateTimeProvider dateTimeProvider) {
        this.firebaseGateway = firebaseGateway;
        this.dateTimeProvider = dateTimeProvider;
    }

    /**
     * Returns a single stop by id for clients that already know the exact stop
     * they want to render or inspect.
     */
    public Stop getStop(String id) {
        CollectionReference stops = firebaseGateway.collection(FirestoreCollections.STOPS);
        DocumentSnapshot document = FirestoreUtils.getDocumentById(stops, id);
        return document.toObject(Stop.class);
    }

    /**
     * Performs an in-memory radius search because stop volume is expected to be
     * manageable and Firestore does not natively support the needed geospatial
     * radius query pattern used here.
     */
    public List<Stop> getStopsWithinRadius(double latitude, double longitude, double radius) {
        CollectionReference stops = firebaseGateway.collection(FirestoreCollections.STOPS);
        try {
            List<Stop> allStops = stops.get().get().toObjects(Stop.class);
            List<Stop> inRadius = new ArrayList<>();
            for (Stop stop : allStops) {
                if (isWithinRadius(latitude, longitude, radius, stop)) {
                    inRadius.add(stop);
                }
            }
            inRadius.sort((first, second) -> Double.compare(distance(latitude, longitude, first), distance(latitude, longitude, second)));
            return inRadius;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to find stops within radius", exception);
        }
    }

    /**
     * Resolves the next buses from the perspective of a stop by walking the
     * route references attached to that stop. This keeps stop responses fast for
     * the frontend without forcing it to compose route and schedule data itself.
     */
    public Map<String, List<String>> getNextBusesByStop(String stopId) {
        CollectionReference stops = firebaseGateway.collection(FirestoreCollections.STOPS);
        DocumentSnapshot document = FirestoreUtils.getDocumentById(stops, stopId);

        try {
            DocumentReference stopRef = document.getReference();
            List<DocumentReference> routeRefs = FirestoreDocumentMapper.getReferenceList(document, "routes");
            Map<String, List<String>> nextBuses = new HashMap<>();
            if (routeRefs == null) {
                return nextBuses;
            }

            for (DocumentReference routeRef : routeRefs) {
                DocumentSnapshot routeDocument = routeRef.get().get();
                Route route = routeDocument.toObject(Route.class);
                List<DocumentReference> forwardRefs = FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.forward");
                List<DocumentReference> backRefs = FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.back");
                Integer indexForward = forwardRefs != null && forwardRefs.contains(stopRef) ? forwardRefs.indexOf(stopRef) : null;
                Integer indexBack = backRefs != null && backRefs.contains(stopRef) ? backRefs.indexOf(stopRef) : null;

                Map<String, Schedule> history = route.getHistory();
                if (history == null) {
                    continue;
                }

                Schedule schedule = route.getDelays();
                if (!FirestoreUtils.checkDelaysIntegrity(schedule)) {
                    schedule = route.getTimetable();
                }

                Schedule todayData = history.get(dateTimeProvider.currentDate());
                if (todayData != null) {
                    addFromTodayHistory(nextBuses, route, schedule, todayData, indexForward, indexBack);
                } else {
                    addFromSchedule(nextBuses, route, schedule, indexForward, indexBack);
                }
            }
            return nextBuses;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve next buses for stop", exception);
        }
    }

    private void addFromTodayHistory(Map<String, List<String>> nextBuses, Route route, Schedule schedule,
                                     Schedule todayData, Integer indexForward, Integer indexBack) {
        List<String> forwardDay = indexForward == null ? null : todayData.getForward().get(String.valueOf(indexForward));
        List<String> backDay = indexBack == null ? null : todayData.getBack().get(String.valueOf(indexBack));
        int startIndexForward = ScheduleUtils.firstUpcomingIndex(forwardDay);
        int startIndexBack = ScheduleUtils.firstUpcomingIndex(backDay);

        List<String> forward = indexForward == null ? null : schedule.getForward().get(String.valueOf(indexForward));
        List<String> back = indexBack == null ? null : schedule.getBack().get(String.valueOf(indexBack));
        putDirectionEntries(nextBuses, route, forward, back, startIndexForward, startIndexBack);
    }

    private void addFromSchedule(Map<String, List<String>> nextBuses, Route route, Schedule schedule,
                                 Integer indexForward, Integer indexBack) {
        List<String> forward = indexForward == null ? null : schedule.getForward().get(String.valueOf(indexForward));
        List<String> back = indexBack == null ? null : schedule.getBack().get(String.valueOf(indexBack));
        putDirectionEntries(nextBuses, route, forward, back, 0, 0);
    }

    private void putDirectionEntries(Map<String, List<String>> nextBuses, Route route, List<String> forward,
                                     List<String> back, int startIndexForward, int startIndexBack) {
        if (forward == null) {
            return;
        }

        if (back != null) {
            // Destination inversion exists because the same route code is used
            // to present forward and return journeys as separate user-facing entries.
            String destination = route.getCode().split("_")[1];
            String invertedDestination = invertDestination(destination);
            nextBuses.put(route.getCode().split("_")[0] + "_" + destination, forward.subList(startIndexForward, forward.size()));
            nextBuses.put(route.getCode().split("_")[0] + "_" + invertedDestination, back.subList(startIndexBack, back.size()));
        } else {
            nextBuses.put(route.getCode(), forward.subList(startIndexForward, forward.size()));
        }
    }

    private String invertDestination(String destination) {
        String[] parts = destination.split(" - ");
        StringBuilder builder = new StringBuilder();
        for (int index = parts.length - 1; index >= 0; index--) {
            builder.append(parts[index]);
            if (index > 0) {
                builder.append(" - ");
            }
        }
        return builder.toString();
    }

    private boolean isWithinRadius(double latitude, double longitude, double radius, Stop stop) {
        return distance(latitude, longitude, stop) <= radius;
    }

    private double distance(double latitude, double longitude, Stop stop) {
        double lat1 = Math.toRadians(latitude);
        double lon1 = Math.toRadians(longitude);
        double lat2 = Math.toRadians(stop.getCoords().getLatitude());
        double lon2 = Math.toRadians(stop.getCoords().getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }
}
