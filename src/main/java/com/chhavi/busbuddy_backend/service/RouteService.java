package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.request.AddRouteRequest;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ConflictException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.exception.ForbiddenException;
import com.chhavi.busbuddy_backend.exception.UnauthorizedException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.RouteDocumentService;
import com.chhavi.busbuddy_backend.security.RouteResource;
import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.chhavi.busbuddy_backend.persistence.model.Stop;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.chhavi.busbuddy_backend.util.ScheduleUtils;
import com.chhavi.busbuddy_backend.util.SearchKeyUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns route lifecycle operations and the consistency rules between routes,
 * stops, and derived delay information.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;
    private final RouteDocumentService routeDocumentService;
    private final AuditLogService auditLogService;

    public RouteService(FirebaseGateway firebaseGateway,
                        AuthorizationService authorizationService,
                        RouteDocumentService routeDocumentService,
                        AuditLogService auditLogService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
        this.routeDocumentService = routeDocumentService;
        this.auditLogService = auditLogService;
    }

    /**
     * Expands stop references into full stop objects because API consumers need
     * route details in one response rather than resolving Firestore references
     * themselves.
     */
    public Route getRoute(String id) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        RouteResource routeResource = routeDocumentService.getRouteResource(id);

        try {
            // Expand stop references (stops.forward/back) into full Stop objects for API consumers.
            DocumentSnapshot document = FirestoreUtils.getDocumentById(routes, id);
            ForwardBackStops stops = routeResource.getRoute().buildStopOutboundReturn(document);
            routeResource.getRoute().setStops(stops);
            return routeResource.getRoute();
        } catch (Exception exception) {
            throw new ApplicationException("Unable to build route details", exception);
        }
    }

    public Map<String, List<Route>> getAllRoutesGroupedByCompany() {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        try {
            List<Route> allRoutes = routes.get().get().toObjects(Route.class);
            for (Route route : allRoutes) {
                DocumentSnapshot document = FirestoreUtils.getDocumentById(routes, route.getId());
                ForwardBackStops stops = route.buildStopOutboundReturn(document);
                route.setStops(stops);
            }

            Map<String, List<Route>> groupedRoutes = new HashMap<>();
            for (Route route : allRoutes) {
                if (route.getCompany() != null) {
                    groupedRoutes.computeIfAbsent(route.getCompany(), key -> new ArrayList<>()).add(route);
                }
            }
            return groupedRoutes;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve routes", exception);
        }
    }

    public boolean addRoute(AddRouteRequest request) {
        Route route = mapToRoute(request);
        return addRouteInternal(route);
    }

    /**
     * Creates a route while reusing existing stop documents by address. This
     * avoids duplicating physical stops across multiple routes of the same or
     * different companies.
     */
    private boolean addRouteInternal(Route route) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        CollectionReference stopsCollection = firebaseGateway.collection(FirestoreCollections.STOPS);

        try {
            authorizationService.requireAdminOrCompanyOwnership(route.getCompany());
            List<QueryDocumentSnapshot> existingRoutes = routes.whereEqualTo("code", route.getCode())
                    .whereEqualTo("company", route.getCompany())
                    .get().get().getDocuments();
            if (!existingRoutes.isEmpty()) {
                throw new ConflictException("Route already exists for company and code");
            }

            List<DocumentReference> forwardRefs = getOrCreateStops(stopsCollection, route.getStops().getForwardStops());
            List<DocumentReference> backRefs = getOrCreateStops(stopsCollection, route.getStops().getBackStops());

            // Stops are stored as Firestore references on the route document,
            // so the expanded stop objects must be removed before persistence.
            route.setStops(null);
            DocumentReference routeRef = routes.add(route).get();
            routeRef.update("searchKey", SearchKeyUtils.normalize(route.getCode()));
            var keys = com.chhavi.busbuddy_backend.util.RouteKeyUtils.parseFromTo(route.getCode());
            if (keys != null) {
                routeRef.update("fromKey", keys.fromKey());
                routeRef.update("toKey", keys.toKey());
            }
            routeRef.update("stops.forward", forwardRefs);
            routeRef.update("stops.back", backRefs);

            for (DocumentReference stopRef : forwardRefs) {
                stopRef.update("routes", FieldValue.arrayUnion(routeRef));
            }
            for (DocumentReference stopRef : backRefs) {
                stopRef.update("routes", FieldValue.arrayUnion(routeRef));
            }
            log.info("Added route code={} company={} with routeId={}", route.getCode(), route.getCompany(), routeRef.getId());
            auditLogService.log(
                    authorizationService.currentUser(),
                    route.getCompany(),
                    "CREATE_ROUTE",
                    "route",
                    routeRef.getId(),
                    Map.of("code", route.getCode()));
            return true;
        } catch (ConflictException exception) {
            throw exception;
        } catch (UnauthorizedException | ForbiddenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to add route and stops", exception);
        }
    }

    public boolean deleteRoute(String id) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        RouteResource routeResource = routeDocumentService.getRouteResource(id);

        try {
            authorizationService.requireAdminOrCompanyOwnership(routeResource.getCompanyId());
            Set<DocumentReference> stopsRefs = new HashSet<>();

            // Read stop references directly from Firestore document fields to avoid relying on expanded Stop objects.
            DocumentSnapshot routeDocument = FirestoreUtils.getDocumentById(routes, id);
            List<DocumentReference> forward = com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.forward");
            List<DocumentReference> back = com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.back");
            if (forward != null) {
                stopsRefs.addAll(forward);
            }
            if (back != null) {
                stopsRefs.addAll(back);
            }

            for (DocumentReference stopRef : stopsRefs) {
                stopRef.update("routes", FieldValue.arrayRemove(firebaseGateway.collection(FirestoreCollections.ROUTES).document(id)));
            }
            firebaseGateway.collection(FirestoreCollections.ROUTES).document(id).delete();
            log.info("Deleted route with id={}", id);
            auditLogService.log(
                    authorizationService.currentUser(),
                    routeResource.getCompanyId(),
                    "DELETE_ROUTE",
                    "route",
                    id,
                    Map.of());
            return true;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to delete route", exception);
        }
    }

    /**
     * Recomputes the derived delay schedule from recorded route history. The
     * stored delays act as a cached aggregate so callers do not have to recalc
     * all history on every request.
     */
    public boolean updateDelay(String routeId) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        RouteResource routeResource = routeDocumentService.getRouteResource(routeId);

        try {
            Route route = routeResource.getRoute();
            authorizationService.requireAdminOrCompanyOwnership(routeResource.getCompanyId());
            Map<String, Schedule> history = route.getHistory();
            if (history == null || history.isEmpty()) {
                throw new ResourceNotFoundException("History not found for route: " + routeId);
            }

            Iterator<Map.Entry<String, Schedule>> iterator = history.entrySet().iterator();
            Schedule delays = iterator.next().getValue();
            while (iterator.hasNext()) {
                Map.Entry<String, Schedule> entry = iterator.next();
                updateDelayTimes(delays.getForward(), entry.getValue().getForward());
                updateDelayTimes(delays.getBack(), entry.getValue().getBack());
            }
            firebaseGateway.collection(FirestoreCollections.ROUTES).document(routeId).update("delays", delays);
            log.info("Updated delays for routeId={}", routeId);
            auditLogService.log(
                    authorizationService.currentUser(),
                    routeResource.getCompanyId(),
                    "RECOMPUTE_DELAYS_ROUTE",
                    "route",
                    routeId,
                    Map.of());
            return true;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to update route delays", exception);
        }
    }

    /**
     * Refreshes cached delay aggregates for every route in bulk. This exists so
     * operators can recalculate analytics after history corrections.
     */
    public boolean updateAllDelays() {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        replaceDashInHistory();

        try {
            authorizationService.requireAdmin();
            ApiFuture<QuerySnapshot> future = routes.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                Route route = document.toObject(Route.class);
                Map<String, Schedule> history = route.getHistory();
                if (history != null && !history.isEmpty()) {
                    Iterator<Map.Entry<String, Schedule>> iterator = history.entrySet().iterator();
                    Schedule delays = iterator.next().getValue();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Schedule> entry = iterator.next();
                        updateDelayTimes(delays.getForward(), entry.getValue().getForward());
                        updateDelayTimes(delays.getBack(), entry.getValue().getBack());
                    }
                    document.getReference().update("delays", delays);
                }
            }
            log.info("Updated delays for all routes");
            auditLogService.log(
                    authorizationService.currentUser(),
                    "*",
                    "RECOMPUTE_DELAYS_GLOBAL",
                    "route",
                    "*",
                    Map.of());
            return true;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to update all delays", exception);
        }
    }

    public boolean updateTimetableMinutes(String routeId, com.chhavi.busbuddy_backend.dto.request.UpdateRouteTimetableRequest request) {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        RouteResource routeResource = routeDocumentService.getRouteResource(routeId);

        try {
            authorizationService.requireAdminOrCompanyOwnership(routeResource.getCompanyId());
            Route route = routeResource.getRoute();

            // Validate against actual stop references stored on the route document.
            DocumentSnapshot routeDocument = FirestoreUtils.getDocumentById(routes, routeId);
            List<DocumentReference> forwardRefs = com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.forward");
            List<DocumentReference> backRefs = com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper.getReferenceList(routeDocument, "stops.back");
            int forwardStops = forwardRefs == null ? 0 : forwardRefs.size();
            int backStops = backRefs == null ? 0 : backRefs.size();

            if (request.getForwardMinutesFromStart().size() != forwardStops) {
                throw new ApplicationException("forwardMinutesFromStart size must match forward stops count=" + forwardStops);
            }
            if (request.getBackMinutesFromStart().size() != backStops) {
                throw new ApplicationException("backMinutesFromStart size must match back stops count=" + backStops);
            }

            Schedule timetable = new Schedule();
            timetable.setForward(minutesListToScheduleMap(request.getForwardMinutesFromStart()));
            timetable.setBack(minutesListToScheduleMap(request.getBackMinutesFromStart()));

            routes.document(routeId).update("timetable", timetable);
            auditLogService.log(
                    authorizationService.currentUser(),
                    routeResource.getCompanyId(),
                    "UPDATE_TIMETABLE",
                    "route",
                    routeId,
                    Map.of("forwardStops", forwardStops, "backStops", backStops));
            return true;
        } catch (UnauthorizedException | ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to update timetable", exception);
        }
    }

    private Map<String, List<String>> minutesListToScheduleMap(List<Integer> minutesFromStart) {
        Map<String, List<String>> map = new HashMap<>();
        for (int i = 0; i < minutesFromStart.size(); i++) {
            Integer minutes = minutesFromStart.get(i);
            if (minutes == null || minutes < 0) {
                throw new ApplicationException("Minutes must be >= 0 for stopIndex=" + i);
            }
            map.put(String.valueOf(i), List.of(String.valueOf(minutes)));
        }
        return map;
    }

    private Route mapToRoute(AddRouteRequest request) {
        Route route = new Route();
        route.setCompany(request.getCompany());
        route.setCode(request.getCode());
        route.setStops(request.getStops());
        return route;
    }

    private List<DocumentReference> getOrCreateStops(CollectionReference stopsCollection, List<Stop> stops) throws Exception {
        List<DocumentReference> refs = new ArrayList<>();
        for (Stop stop : stops) {
            List<QueryDocumentSnapshot> stopList = stopsCollection.whereEqualTo("address", stop.getAddress()).get().get().getDocuments();
            if (stopList.isEmpty()) {
                refs.add(stopsCollection.add(stop).get());
            } else {
                refs.add(stopList.get(0).getReference());
            }
        }
        return refs;
    }

    private List<DocumentReference> mapStopsToReferences(List<Stop> stops) {
        if (stops == null) {
            return List.of();
        }
        return stops.stream()
                .map(stop -> firebaseGateway.collection(FirestoreCollections.STOPS).document(stop.getId()))
                .toList();
    }

    private void updateDelayTimes(Map<String, List<String>> delaysTimes, Map<String, List<String>> times) {
        for (Map.Entry<String, List<String>> entry : times.entrySet()) {
            List<String> stopTimes = entry.getValue();
            List<String> stopDelaysTimes = delaysTimes.get(entry.getKey());
            for (int index = 0; index < stopTimes.size(); index++) {
                String time = stopTimes.get(index);
                if (time != null) {
                    String delayTime = stopDelaysTimes.get(index);
                    stopDelaysTimes.set(index, delayTime != null ? ScheduleUtils.averageTime(delayTime, time) : time);
                }
            }
        }
    }

    /**
     * Normalizes placeholder values before delay aggregation because '-' means
     * "not yet reached" in operational history, while aggregation logic should
     * treat missing observations as null.
     */
    private void replaceDashInHistory() {
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);
        try {
            ApiFuture<QuerySnapshot> future = routes.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                Route route = document.toObject(Route.class);
                Map<String, Schedule> history = route.getHistory();
                if (history != null && !history.isEmpty()) {
                    for (Map.Entry<String, Schedule> entry : history.entrySet()) {
                        Schedule schedule = entry.getValue();
                        ScheduleUtils.replaceDashWithNull(schedule.getForward());
                        ScheduleUtils.replaceDashWithNull(schedule.getBack());
                    }
                    document.getReference().update("history", history);
                }
            }
        } catch (Exception exception) {
            throw new ApplicationException("Unable to normalize route history", exception);
        }
    }
}

