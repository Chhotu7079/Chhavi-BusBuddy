package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.constant.RouteDirection;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.OwnershipService;
import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.chhavi.busbuddy_backend.util.DateTimeProvider;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.chhavi.busbuddy_backend.util.ScheduleUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles route execution history updates. This service exists separately from
 * bus management because history recording follows operational events (stop
 * reached, direction change, daily reset) rather than CRUD-style bus actions.
 */
@Service
public class BusHistoryService {

    private final FirebaseGateway firebaseGateway;
    private final DateTimeProvider dateTimeProvider;
    private final AuthorizationService authorizationService;
    private final OwnershipService ownershipService;
    private final AuditLogService auditLogService;

    public BusHistoryService(FirebaseGateway firebaseGateway,
                             DateTimeProvider dateTimeProvider,
                             AuthorizationService authorizationService,
                             OwnershipService ownershipService,
                             AuditLogService auditLogService) {
        this.firebaseGateway = firebaseGateway;
        this.dateTimeProvider = dateTimeProvider;
        this.authorizationService = authorizationService;
        this.ownershipService = ownershipService;
        this.auditLogService = auditLogService;
    }

    /**
     * Produces upcoming arrivals for a bus in one direction by combining the
     * base timetable with today's observed progress. The frontend needs this
     * precomputed view to render live ETA-like stop lists without understanding
     * schedule history internals.
     */
    public Map<String, List<String>> getNextArrivalsByBus(String busId, RouteDirection direction) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        DocumentSnapshot document = FirestoreUtils.getDocumentById(buses, busId);
        if (!document.exists()) {
            throw new ResourceNotFoundException("Bus not found with id: " + busId);
        }

        try {
            DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(document, "route");
            DocumentSnapshot routeDocument = routeRef.get().get();
            Route route = routeDocument.toObject(Route.class);
            ForwardBackStops stops = route.buildStopOutboundReturn(routeDocument);
            Map<String, List<String>> nextArrivals = new HashMap<>();

            Map<String, Schedule> history = route.getHistory();
            if (history == null) {
                return nextArrivals;
            }

            String today = dateTimeProvider.currentDate();
            Schedule todayData = history.get(today);
            Schedule schedule = FirestoreUtils.checkDelaysIntegrity(route.getDelays()) ? route.getDelays() : route.getTimetable();
            Map<String, List<String>> timetable = RouteDirection.FORWARD.equals(direction) ? schedule.getForward() : schedule.getBack();

            for (Map.Entry<String, List<String>> entry : timetable.entrySet()) {
                List<String> times = entry.getValue();
                int startIndex = 0;
                if (todayData != null) {
                    List<String> entryHistory = RouteDirection.FORWARD.equals(direction)
                            ? todayData.getForward().get(entry.getKey())
                            : todayData.getBack().get(entry.getKey());
                    startIndex = ScheduleUtils.firstUpcomingIndex(entryHistory);
                }

                String stopId = RouteDirection.FORWARD.equals(direction)
                        ? stops.getForwardStops().get(Integer.parseInt(entry.getKey())).getId()
                        : stops.getBackStops().get(Integer.parseInt(entry.getKey())).getId();
                nextArrivals.put(stopId, times.subList(Math.min(startIndex, times.size()), times.size()));
            }
            return nextArrivals;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve next arrivals by bus", exception);
        }
    }

    /**
     * Marks the next outstanding placeholder for a stop as reached. History uses
     * '-' placeholders so the backend can distinguish "scheduled but not yet
     * observed" from null/irrelevant values.
     */
    public boolean updateStopReached(String routeId, String stopIndex, RouteDirection direction) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        try {
            DocumentReference routeRef = routes.document(routeId);
            DocumentSnapshot routeSnapshot = routeRef.get().get();
            if (!routeSnapshot.exists()) {
                throw new ResourceNotFoundException("Route not found with id: " + routeId);
            }

            String currentTime = dateTimeProvider.currentTime();
            Route route = routeSnapshot.toObject(Route.class);
            authorizationService.requireAdminOrCompanyOwnership(ownershipService.getRouteOwnerCompanyId(routeId));
            Map<String, Schedule> history = route.getHistory();
            if (history == null) {
                throw new ApplicationException("Route history is not initialized");
            }

            String today = dateTimeProvider.currentDate();
            Schedule todayData = history.get(today);
            if (todayData == null) {
                initializeTodayHistory(routeId);
                return updateStopReached(routeId, stopIndex, direction);
            }

            Map<String, List<String>> timetable = RouteDirection.FORWARD.equals(direction) ? todayData.getForward() : todayData.getBack();
            List<String> stopTimes = timetable.get(stopIndex);
            if (stopTimes == null) {
                stopTimes = new ArrayList<>();
                stopTimes.add(currentTime);
                timetable.put(stopIndex, stopTimes);
            } else {
                for (int index = 0; index < stopTimes.size(); index++) {
                    if ("-".equals(stopTimes.get(index))) {
                        stopTimes.set(index, currentTime);
                        break;
                    }
                }
            }

            Schedule updatedTodayData = todayData;
            ApiFuture<Boolean> future = firebaseGateway.firestore().runTransaction(transaction -> {
                DocumentSnapshot routeSnapshotAgain = transaction.get(routeRef).get();
                Route routeAgain = routeSnapshotAgain.toObject(Route.class);
                routeAgain.getHistory().put(today, updatedTodayData);
                transaction.update(routeRef, "history", routeAgain.getHistory());
                return true;
            });
            boolean result = future.get();
            if (result) {
                auditLogService.log(
                        authorizationService.currentUser(),
                        ownershipService.getRouteOwnerCompanyId(routeId),
                        "STOP_REACHED",
                        "route",
                        routeId,
                        Map.of("stopIndex", stopIndex, "direction", direction.getValue()));
            }
            return result;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to update stop reached", exception);
        }
    }

    public boolean fixHistoryGaps(String routeId, RouteDirection direction) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        try {
            DocumentReference routeRef = routes.document(routeId);
            DocumentSnapshot routeSnapshot = routeRef.get().get();
            if (!routeSnapshot.exists()) {
                throw new ResourceNotFoundException("Route not found with id: " + routeId);
            }

            Route route = routeSnapshot.toObject(Route.class);
            authorizationService.requireAdminOrCompanyOwnership(ownershipService.getRouteOwnerCompanyId(routeId));
            Map<String, Schedule> history = route.getHistory();
            if (history == null) {
                throw new ApplicationException("History not found for route: " + routeId);
            }

            String today = dateTimeProvider.currentDate();
            Schedule todayData = history.get(today);
            if (todayData == null) {
                throw new ResourceNotFoundException("No history for today on route: " + routeId);
            }

            Map<String, List<String>> day = RouteDirection.FORWARD.equals(direction) ? todayData.getForward() : todayData.getBack();
            if (day == null || day.isEmpty()) {
                throw new ApplicationException("Direction timetable not found");
            }

            int numCols = day.get("0").size();
            boolean fixed = false;
            for (int column = 0; column < numCols && !fixed; column++) {
                int count = 0;
                for (int row = 0; row < day.size() - 1; row++) {
                    if (day.get(String.valueOf(row)).get(column) != null && "-".equals(day.get(String.valueOf(row)).get(column))) {
                        count++;
                    }
                }
                if (count > 0 && count < day.size() - 1) {
                    for (int row = 0; row < day.size() - 1; row++) {
                        if (day.get(String.valueOf(row)).get(column) != null && "-".equals(day.get(String.valueOf(row)).get(column))) {
                            day.get(String.valueOf(row)).set(column, null);
                        }
                    }
                    fixed = true;
                }
            }

            firebaseGateway.firestore().runTransaction(transaction -> {
                DocumentSnapshot routeSnapshotAgain = transaction.get(routeRef).get();
                Route routeAgain = routeSnapshotAgain.toObject(Route.class);
                routeAgain.getHistory().put(today, todayData);
                transaction.update(routeRef, "history", routeAgain.getHistory());
                return true;
            }).get();

            auditLogService.log(
                    authorizationService.currentUser(),
                    ownershipService.getRouteOwnerCompanyId(routeId),
                    "FIX_HISTORY_GAPS",
                    "route",
                    routeId,
                    Map.of("direction", direction.getValue()));

            return true;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to fix history gaps", exception);
        }
    }

    /**
     * Seeds today's history from the timetable so operational tracking can fill
     * values progressively throughout the day. Keeping at most seven days limits
     * unbounded history growth in Firestore.
     */
    private boolean initializeTodayHistory(String routeId) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        try {
            DocumentReference routeRef = routes.document(routeId);
            DocumentSnapshot routeSnapshot = routeRef.get().get();
            if (!routeSnapshot.exists()) {
                throw new ResourceNotFoundException("Route not found with id: " + routeId);
            }

            Route route = routeSnapshot.toObject(Route.class);
            Map<String, Schedule> history = route.getHistory();
            if (history == null) {
                throw new ApplicationException("History map is not available for route: " + routeId);
            }

            Schedule timetable = route.getTimetable();
            Schedule scheduleForToday = new Schedule();
            scheduleForToday.setForward(copyAndPlaceholder(timetable.getForward()));
            scheduleForToday.setBack(copyAndPlaceholder(timetable.getBack()));
            String today = dateTimeProvider.currentDate();

            firebaseGateway.firestore().runTransaction(transaction -> {
                DocumentSnapshot routeSnapshotAgain = transaction.get(routeRef).get();
                Route routeAgain = routeSnapshotAgain.toObject(Route.class);
                routeAgain.getHistory().put(today, scheduleForToday);
                if (routeAgain.getHistory().size() > 7) {
                    List<String> dates = new ArrayList<>(routeAgain.getHistory().keySet());
                    ScheduleUtils.sortDatesAscending(dates);
                    routeAgain.getHistory().remove(dates.get(0));
                }
                transaction.update(routeRef, "history", routeAgain.getHistory());
                return true;
            }).get();
            return true;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to initialize today's history", exception);
        }
    }

    private Map<String, List<String>> copyAndPlaceholder(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        ScheduleUtils.replaceNonNullValuesWithPlaceholder(copy);
        return copy;
    }
}
