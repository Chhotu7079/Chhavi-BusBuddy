package com.chhavi.busbuddy_backend.security;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.persistence.model.Route;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

@Service
public class FirestoreRouteDocumentService implements RouteDocumentService {

    private final com.chhavi.busbuddy_backend.gateway.FirebaseGateway firebaseGateway;

    public FirestoreRouteDocumentService(com.chhavi.busbuddy_backend.gateway.FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    @Override
    public RouteResource getRouteResource(String routeId) {
        DocumentSnapshot document = FirestoreUtils.getDocumentById(firebaseGateway.collection(FirestoreCollections.ROUTES), routeId);
        Route route = document.toObject(Route.class);
        if (route == null) {
            throw new ApplicationException("Unable to load route resource: " + routeId);
        }
        String companyId = route.getCompany();
        if (companyId == null || companyId.isBlank()) {
            throw new ApplicationException("Route does not contain an owning company: " + routeId);
        }
        return new RouteResource(routeId, companyId, route);
    }
}
