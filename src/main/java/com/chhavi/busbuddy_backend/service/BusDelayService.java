package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.constant.RouteDirection;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.chhavi.busbuddy_backend.util.DateTimeProvider;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.chhavi.busbuddy_backend.util.ScheduleUtils;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Calculates delay-oriented views from stored timetable, history, and cached
 * delay aggregates. This keeps analytics-related logic separate from route
 * mutation and bus operational updates.
 */
@Service
public class BusDelayService {

    private final FirebaseGateway firebaseGateway;
    private final DateTimeProvider dateTimeProvider;

    public BusDelayService(FirebaseGateway firebaseGateway, DateTimeProvider dateTimeProvider) {
        this.firebaseGateway = firebaseGateway;
        this.dateTimeProvider = dateTimeProvider;
    }

    /**
     * Returns a historical average delay by comparing aggregated observed times
     * against the timetable for one direction.
     */
    public int getAverageBusDelay(String busId, RouteDirection direction) {
        Route route = getRouteByBusId(busId);
        Schedule delays = route.getDelays();
        if (!FirestoreUtils.checkDelaysIntegrity(delays)) {
            throw new ApplicationException("Delays data is not complete for bus: " + busId);
        }

        try {
            Schedule timetable = route.getTimetable();
            Map<String, List<String>> timetableDirection = RouteDirection.FORWARD.equals(direction) ? timetable.getForward() : timetable.getBack();
            Map<String, List<String>> delaysDirection = RouteDirection.FORWARD.equals(direction) ? delays.getForward() : delays.getBack();

            int totalDelay = 0;
            int numDelays = 0;
            for (Map.Entry<String, List<String>> entry : timetableDirection.entrySet()) {
                List<String> timetableTimes = entry.getValue();
                List<String> delaysTimes = delaysDirection.get(entry.getKey());
                for (int index = 0; index < timetableTimes.size(); index++) {
                    if (delaysTimes.get(index) != null && !"-".equals(delaysTimes.get(index))) {
                        totalDelay += ScheduleUtils.calculateDelay(timetableTimes.get(index), delaysTimes.get(index));
                        numDelays++;
                    }
                }
            }
            return numDelays > 0 ? totalDelay / numDelays : 0;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to calculate average bus delay", exception);
        }
    }

    /**
     * Returns the latest known delay by locating the most recent observed time
     * in today's history and comparing it to the same timetable position.
     */
    public int getCurrentDelay(String busId, RouteDirection direction) {
        Route route = getRouteByBusId(busId);

        try {
            Schedule effectiveSchedule = FirestoreUtils.checkDelaysIntegrity(route.getDelays()) ? route.getDelays() : route.getTimetable();
            Map<String, List<String>> timetableDirection = RouteDirection.FORWARD.equals(direction) ? effectiveSchedule.getForward() : effectiveSchedule.getBack();
            Map<String, Schedule> history = route.getHistory();
            if (history == null) {
                return 0;
            }

            Schedule todayData = history.get(dateTimeProvider.currentDate());
            if (todayData == null) {
                return 0;
            }

            Map<String, List<String>> day = RouteDirection.FORWARD.equals(direction) ? todayData.getForward() : todayData.getBack();
            int numCols = day.get("0").size();
            for (int column = numCols - 1; column >= 0; column--) {
                for (int row = day.size() - 1; row >= 0; row--) {
                    String actualTime = day.get(String.valueOf(row)).get(column);
                    if (actualTime != null && !"-".equals(actualTime)) {
                        return ScheduleUtils.calculateDelay(timetableDirection.get(String.valueOf(row)).get(column), actualTime);
                    }
                }
            }
            return 0;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to calculate current bus delay", exception);
        }
    }

    private Route getRouteByBusId(String busId) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        DocumentSnapshot busDocument = FirestoreUtils.getDocumentById(buses, busId);

        try {
            DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(busDocument, "route");
            DocumentSnapshot routeDocument = routeRef.get().get();
            return routeDocument.toObject(Route.class);
        } catch (Exception exception) {
            throw new ApplicationException("Unable to resolve route for bus", exception);
        }
    }
}
