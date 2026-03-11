package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.OwnershipService;
import com.chhavi.busbuddy_backend.persistence.model.Bus;
import com.chhavi.busbuddy_backend.persistence.model.ForwardBackStops;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the stable identity and route assignment side of buses. This service
 * coordinates Firestore and Realtime Database because bus metadata and live
 * operational state are intentionally split across those stores.
 */
@Service
public class BusManagementService {

    private static final Logger log = LoggerFactory.getLogger(BusManagementService.class);

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;
    private final OwnershipService ownershipService;
    private final AuditLogService auditLogService;

    public BusManagementService(FirebaseGateway firebaseGateway,
                                AuthorizationService authorizationService,
                                OwnershipService ownershipService,
                                AuditLogService auditLogService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
        this.ownershipService = ownershipService;
        this.auditLogService = auditLogService;
    }

    /**
     * Returns a bus with its route expanded so API consumers do not have to
     * follow Firestore references themselves.
     */
    public Bus getBus(String id) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        DocumentSnapshot document = FirestoreUtils.getDocumentById(buses, id);

        try {
            DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(document, "route");
            DocumentSnapshot routeDocument = routeRef.get().get();
            Route route = routeDocument.toObject(Route.class);
            ForwardBackStops stops = route.buildStopOutboundReturn(routeDocument);
            route.setStops(stops);

            Bus bus = new Bus();
            bus.setId(document.getId());
            bus.setCode(document.getString("code"));
            bus.setRoute(route);
            return bus;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve bus", exception);
        }
    }

    public Bus getBusByCode(String code) {
        try {
            Query query = firebaseGateway.collection(FirestoreCollections.BUSES).whereEqualTo("code", code);
            QuerySnapshot querySnapshot = query.get().get();
            if (querySnapshot.isEmpty()) {
                throw new ResourceNotFoundException("Bus not found with code: " + code);
            }

            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(document, "route");
            DocumentSnapshot routeDocument = routeRef.get().get();
            Route route = routeDocument.toObject(Route.class);
            route.setStops(route.buildStopOutboundReturn(routeDocument));
            route.setTimetable(null);
            route.setHistory(null);

            Bus bus = new Bus();
            bus.setId(document.getId());
            bus.setRoute(route);
            bus.setCode(code);
            bus.setCoords(new GeoPoint(0, 0));
            return bus;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve bus by code", exception);
        }
    }

    public ForwardBackStops getStopsByBus(String busId) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        DocumentSnapshot document = FirestoreUtils.getDocumentById(buses, busId);

        try {
            DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(document, "route");
            DocumentSnapshot routeDocument = routeRef.get().get();
            Route route = routeDocument.toObject(Route.class);
            return route.buildStopOutboundReturn(routeDocument);
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve stops by bus", exception);
        }
    }

    /**
     * Creates the canonical bus document in Firestore and the live-tracking seed
     * record in Realtime Database so the bus is immediately ready for runtime
     * position updates.
     */
    public boolean addBus(String busCode, String routeId) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);

        try {
            DocumentReference routeRef = routes.document(routeId);
            String routeOwnerCompanyId = ownershipService.getRouteOwnerCompanyId(routeId);
            DocumentSnapshot routeSnapshot = FirestoreUtils.getDocumentById(routes, routeId);
            authorizationService.requireAdminOrCompanyOwnership(routeOwnerCompanyId);

            Map<String, Object> data = new HashMap<>();
            data.put("code", busCode);
            data.put("searchKey", com.chhavi.busbuddy_backend.util.SearchKeyUtils.normalize(busCode));
            data.put("route", routeRef);
            data.put("company", routeSnapshot.getString("company"));

            DocumentReference newBusRef = buses.add(data).get();
            String busId = newBusRef.getId();

            DatabaseReference busesRef = firebaseGateway.realtimeRoot().child("buses").child(busId);
            Map<String, Object> realtimeData = new HashMap<>();
            Map<String, Double> coords = new HashMap<>();
            coords.put("latitude", 0.0);
            coords.put("longitude", 0.0);
            realtimeData.put("coords", coords);
            realtimeData.put("routeId", routeId);
            realtimeData.put("direction", "");
            realtimeData.put("lastStop", 0);
            realtimeData.put("speed", 0);
            realtimeData.put("company", routeSnapshot.getString("company"));
            realtimeData.put("status", "IDLE");
            realtimeData.put("lastUpdatedAt", System.currentTimeMillis());

            executeRealtimeWrite(listener -> busesRef.setValue(realtimeData, listener), "Unable to save realtime bus data");
            // Maintain a lightweight RTDB index for route -> buses to avoid scanning all buses for live lookups.
            DatabaseReference routeBusesRef = firebaseGateway.realtimeRoot().child("routeBuses").child(routeId).child(busId);
            executeRealtimeWrite(listener -> routeBusesRef.setValue(true, listener), "Unable to save realtime route bus index");
            log.info("Added bus with code={} for routeId={} and realtime id={}", busCode, routeId, busId);
            auditLogService.log(
                    authorizationService.currentUser(),
                    routeOwnerCompanyId,
                    "ADD_BUS",
                    "bus",
                    busId,
                    Map.of("busCode", busCode, "routeId", routeId));
            return true;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to add bus", exception);
        }
    }

    public List<Route> getRoutesByBusCode(String busCode) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);

        try {
            QuerySnapshot busSnapshot = buses.whereEqualTo("code", busCode).get().get();
            if (busSnapshot.isEmpty()) {
                throw new ResourceNotFoundException("Bus not found with code: " + busCode);
            }

            DocumentSnapshot busDocument = busSnapshot.getDocuments().get(0);
            String company = busDocument.getString("company");
            authorizationService.requireAdminOrCompanyOwnership(company);
            QuerySnapshot routeSnapshot = routes.whereEqualTo("company", company).get().get();

            List<Route> routeList = new ArrayList<>();
            for (DocumentSnapshot document : routeSnapshot.getDocuments()) {
                Route route = document.toObject(Route.class);
                route.setStops(null);
                route.setTimetable(null);
                route.setHistory(null);
                routeList.add(route);
            }
            return routeList;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve routes by bus code", exception);
        }
    }

    public boolean updateBusRoute(String busCode, String routeId) {
        CollectionReference buses = firebaseGateway.collection(FirestoreCollections.BUSES);
        CollectionReference routes = firebaseGateway.collection(FirestoreCollections.ROUTES);

        try {
            QuerySnapshot querySnapshot = buses.whereEqualTo("code", busCode).get().get();
            if (querySnapshot.isEmpty()) {
                throw new ResourceNotFoundException("Bus not found with code: " + busCode);
            }

            DocumentSnapshot busDocument = querySnapshot.getDocuments().get(0);
            authorizationService.requireAdminOrCompanyOwnership(ownershipService.getBusOwnerCompanyId(busDocument.getId()));
            DocumentReference routeRef = routes.document(routeId);
            busDocument.getReference().update("route", routeRef);

            Map<String, Object> update = new HashMap<>();
            update.put("routeId", routeId);
            executeRealtimeWrite(listener -> firebaseGateway.realtimeRoot()
                    .child("buses")
                    .child(busDocument.getId())
                    .updateChildren(update, listener), "Realtime update failed");
            log.info("Updated bus route for busCode={} to routeId={}", busCode, routeId);
            auditLogService.log(
                    authorizationService.currentUser(),
                    ownershipService.getBusOwnerCompanyId(busDocument.getId()),
                    "UPDATE_BUS_ROUTE",
                    "bus",
                    busDocument.getId(),
                    Map.of("busCode", busCode, "routeId", routeId));
            return true;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to update bus route", exception);
        }
    }

    /**
     * Waits for Firebase callback completion so callers get a true success/fail
     * outcome instead of fire-and-forget behavior that could hide write errors.
     */
    private void executeRealtimeWrite(RealtimeWriteOperation operation, String failureMessage) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DatabaseError> errorRef = new AtomicReference<>();

        operation.execute((databaseError, databaseReference) -> {
            errorRef.set(databaseError);
            latch.countDown();
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new ApplicationException(failureMessage + ": timed out waiting for Firebase Realtime Database callback");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException(failureMessage + ": interrupted while waiting for Firebase Realtime Database callback", exception);
        }

        DatabaseError databaseError = errorRef.get();
        if (databaseError != null) {
            throw new ApplicationException(failureMessage + ": " + databaseError.getMessage());
        }
    }

    @FunctionalInterface
    private interface RealtimeWriteOperation {
        void execute(DatabaseReference.CompletionListener listener);
    }
}
