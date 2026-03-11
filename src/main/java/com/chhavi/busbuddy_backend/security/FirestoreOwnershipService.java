package com.chhavi.busbuddy_backend.security;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.util.FirestoreUtils;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

@Service
public class FirestoreOwnershipService implements OwnershipService {

    private final FirebaseGateway firebaseGateway;

    public FirestoreOwnershipService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    @Override
    public String getRouteOwnerCompanyId(String routeId) {
        DocumentSnapshot routeDocument = FirestoreUtils.getDocumentById(
                firebaseGateway.collection(FirestoreCollections.ROUTES), routeId);
        String companyId = routeDocument.getString("company");
        if (companyId == null || companyId.isBlank()) {
            throw new ApplicationException("Route does not contain an owning company: " + routeId);
        }
        return companyId;
    }

    @Override
    public String getBusOwnerCompanyId(String busId) {
        DocumentSnapshot busDocument = FirestoreUtils.getDocumentById(
                firebaseGateway.collection(FirestoreCollections.BUSES), busId);
        String companyId = busDocument.getString("company");
        if (companyId == null || companyId.isBlank()) {
            throw new ApplicationException("Bus does not contain an owning company: " + busId);
        }
        return companyId;
    }
}
