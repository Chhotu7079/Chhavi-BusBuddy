package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.response.BusLiveResponse;
import com.chhavi.busbuddy_backend.dto.response.CoordinatesResponse;
import com.chhavi.busbuddy_backend.dto.response.RouteLiveResponse;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class LiveTrackingService {

    private static final long DEFAULT_ACTIVE_WINDOW_MS = 2 * 60 * 1000; // 2 minutes

    private final FirebaseGateway firebaseGateway;

    public LiveTrackingService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    /**
     * Public live view: returns currently active buses for the given route.
     * A bus is considered active if lastUpdatedAt is within the active window.
     */
    public RouteLiveResponse getRouteLive(String routeId) {
        Map<String, String> busCodesById = loadBusCodesForRoute(routeId);
        List<BusLiveResponse> active = loadActiveBusesFromRealtimeIndexed(routeId, busCodesById, DEFAULT_ACTIVE_WINDOW_MS);
        return new RouteLiveResponse(routeId, active);
    }

    /**
     * Reads current live state for a single bus from Realtime Database.
     * Returns null if bus node is missing.
     */
    public BusLiveResponse getBusLive(String busId, String busCode) {
        try {
            DataSnapshot snapshot = readOnce(firebaseGateway.realtimeRoot().child("buses").child(busId));
            if (!snapshot.exists()) {
                return null;
            }

            Double latitude = snapshot.child("coords").child("latitude").getValue(Double.class);
            Double longitude = snapshot.child("coords").child("longitude").getValue(Double.class);
            Double speed = snapshot.child("speed").getValue(Double.class);
            Integer lastStop = snapshot.child("lastStop").getValue(Integer.class);
            String direction = snapshot.child("direction").getValue(String.class);
            String status = snapshot.child("status").getValue(String.class);
            Long lastUpdatedAt = snapshot.child("lastUpdatedAt").getValue(Long.class);
            String routeId = snapshot.child("routeId").getValue(String.class);

            BusLiveResponse response = new BusLiveResponse();
            response.setBusId(busId);
            response.setBusCode(busCode);
            if (latitude != null && longitude != null) {
                response.setCoords(new CoordinatesResponse(latitude, longitude));
            }
            response.setSpeed(speed);
            response.setLastStop(lastStop);
            response.setDirection(direction);
            response.setStatus(status);
            response.setLastUpdatedAt(lastUpdatedAt);
            response.setRouteId(routeId);
            return response;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to read live bus state", exception);
        }
    }

    private Map<String, String> loadBusCodesForRoute(String routeId) {
        try {
            // Firestore buses store route as DocumentReference. We can query by route reference.
            QuerySnapshot snapshot = firebaseGateway.collection(FirestoreCollections.BUSES)
                    .whereEqualTo("route", firebaseGateway.collection(FirestoreCollections.ROUTES).document(routeId))
                    .get()
                    .get();

            Map<String, String> codes = new HashMap<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String code = doc.getString("code");
                codes.put(doc.getId(), code);
            }
            return codes;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to load buses for route", exception);
        }
    }

    private List<BusLiveResponse> loadActiveBusesFromRealtimeIndexed(String routeId,
                                                                     Map<String, String> busCodesById,
                                                                     long activeWindowMs) {
        final long now = System.currentTimeMillis();
        List<BusLiveResponse> results = new ArrayList<>();

        // Read busIds for this route from index: /routeBuses/{routeId}/{busId}: true
        DataSnapshot indexSnapshot = readOnce(firebaseGateway.realtimeRoot().child("routeBuses").child(routeId));
        if (!indexSnapshot.exists() || !indexSnapshot.hasChildren()) {
            // Fallback to empty (no active buses) instead of scanning all buses.
            return results;
        }

        for (DataSnapshot busIdSnap : indexSnapshot.getChildren()) {
            String busId = busIdSnap.getKey();
            if (busId == null) {
                continue;
            }

            DataSnapshot busSnapshot = readOnce(firebaseGateway.realtimeRoot().child("buses").child(busId));
            if (!busSnapshot.exists()) {
                continue;
            }

            Long lastUpdatedAt = busSnapshot.child("lastUpdatedAt").getValue(Long.class);
            if (lastUpdatedAt == null || now - lastUpdatedAt > activeWindowMs) {
                continue;
            }

            Double latitude = busSnapshot.child("coords").child("latitude").getValue(Double.class);
            Double longitude = busSnapshot.child("coords").child("longitude").getValue(Double.class);
            Double speed = busSnapshot.child("speed").getValue(Double.class);
            Integer lastStop = busSnapshot.child("lastStop").getValue(Integer.class);
            String direction = busSnapshot.child("direction").getValue(String.class);
            String status = busSnapshot.child("status").getValue(String.class);

            BusLiveResponse response = new BusLiveResponse();
            response.setBusId(busId);
            response.setBusCode(busCodesById.get(busId));
            if (latitude != null && longitude != null) {
                response.setCoords(new CoordinatesResponse(latitude, longitude));
            }
            response.setSpeed(speed);
            response.setLastStop(lastStop);
            response.setDirection(direction);
            response.setRouteId(routeId);
            response.setLastUpdatedAt(lastUpdatedAt);
            response.setStatus(status);

            results.add(response);
        }

        return results;
    }

    private DataSnapshot readOnce(DatabaseReference reference) {
        CountDownLatch latch = new CountDownLatch(1);
        final DataSnapshot[] holder = new DataSnapshot[1];
        final RuntimeException[] errorHolder = new RuntimeException[1];

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                holder[0] = snapshot;
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                errorHolder[0] = new ApplicationException("Realtime DB read cancelled: " + error.getMessage());
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new ApplicationException("Timed out reading realtime bus data");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Interrupted while reading realtime bus data", exception);
        }

        if (errorHolder[0] != null) {
            throw errorHolder[0];
        }

        return holder[0];
    }
}
