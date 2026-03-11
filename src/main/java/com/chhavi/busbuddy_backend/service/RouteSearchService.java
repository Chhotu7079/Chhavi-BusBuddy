package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.response.RouteSearchLiveResponse;
import com.chhavi.busbuddy_backend.dto.response.RouteSearchResponse;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RouteSearchService {

    private final FirebaseGateway firebaseGateway;
    private final LiveTrackingService liveTrackingService;

    public RouteSearchService(FirebaseGateway firebaseGateway, LiveTrackingService liveTrackingService) {
        this.firebaseGateway = firebaseGateway;
        this.liveTrackingService = liveTrackingService;
    }

    public List<RouteSearchResponse> search(String from, String to) {
        String normalizedFrom = normalize(from);
        String normalizedTo = normalize(to);

        try {
            // Preferred: indexed query using fromKey/toKey.
            var querySnapshot = firebaseGateway.collection(FirestoreCollections.ROUTES)
                    .whereEqualTo("fromKey", normalizedFrom)
                    .whereEqualTo("toKey", normalizedTo)
                    .get()
                    .get();

            List<RouteSearchResponse> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                String code = doc.getString("code");
                if (code != null) {
                    results.add(new RouteSearchResponse(doc.getId(), code));
                }
            }

            // Fallback for legacy routes that haven't been migrated/backfilled yet.
            if (!results.isEmpty()) {
                return results;
            }

            List<QueryDocumentSnapshot> documents = firebaseGateway.collection(FirestoreCollections.ROUTES)
                    .get()
                    .get()
                    .getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                String code = doc.getString("code");
                if (code == null) {
                    continue;
                }
                String searchKey = doc.getString("searchKey");
                String haystack = (searchKey != null && !searchKey.isBlank()) ? searchKey : normalize(code);
                if (matchesFromToHaystack(haystack, normalizedFrom, normalizedTo)) {
                    results.add(new RouteSearchResponse(doc.getId(), code));
                }
            }

            return results;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to search routes", exception);
        }
    }

    public List<RouteSearchLiveResponse> searchLive(String from, String to) {
        List<RouteSearchResponse> routes = search(from, to);
        return routes.stream()
                .map(route -> new RouteSearchLiveResponse(
                        route.getRouteId(),
                        route.getRouteCode(),
                        liveTrackingService.getRouteLive(route.getRouteId()).getActiveBuses()))
                .toList();
    }

    private boolean matchesFromToHaystack(String haystack, String normalizedFrom, String normalizedTo) {
        if (haystack == null) {
            return false;
        }
        return haystack.contains(normalizedFrom) && haystack.contains(normalizedTo)
                && haystack.indexOf(normalizedFrom) < haystack.indexOf(normalizedTo);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" to ", " ")
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ");
    }
}
