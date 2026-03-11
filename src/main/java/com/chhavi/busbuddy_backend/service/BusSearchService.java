package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.response.BusSearchResponse;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BusSearchService {

    private final FirebaseGateway firebaseGateway;

    public BusSearchService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    /**
     * Public search for buses by partial code/name.
     *
     * Note: Firestore does not support contains-ignore-case queries without additional indexes.
     * This implementation loads bus codes and filters in-memory.
     */
    public List<BusSearchResponse> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        try {
            List<QueryDocumentSnapshot> buses = firebaseGateway.collection(FirestoreCollections.BUSES)
                    .get()
                    .get()
                    .getDocuments();

            List<BusSearchResponse> results = new ArrayList<>();
            for (QueryDocumentSnapshot busDoc : buses) {
                String code = busDoc.getString("code");
                if (code == null) {
                    continue;
                }
                String searchKey = busDoc.getString("searchKey");
                String haystack = searchKey != null ? searchKey : normalize(code);
                if (!haystack.contains(normalizedQuery)) {
                    continue;
                }

                // Join route code for a nicer UX.
                String routeId = null;
                String routeCode = null;
                try {
                    DocumentReference routeRef = FirestoreDocumentMapper.getRequiredReference(busDoc, "route");
                    routeId = routeRef.getId();
                    DocumentSnapshot routeSnap = routeRef.get().get();
                    routeCode = routeSnap.getString("code");
                } catch (Exception ignored) {
                    // Best-effort join; search should still return bus result.
                }

                results.add(new BusSearchResponse(busDoc.getId(), code, routeId, routeCode));
            }

            // Simple cap to avoid returning too many results.
            return results.size() > 50 ? results.subList(0, 50) : results;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to search buses", exception);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
