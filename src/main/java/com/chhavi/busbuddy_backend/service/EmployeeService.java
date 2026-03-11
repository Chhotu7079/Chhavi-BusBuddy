package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.request.CreateEmployeeRequest;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.chhavi.busbuddy_backend.security.AuthenticatedUser;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;

    public EmployeeService(FirebaseGateway firebaseGateway, AuthorizationService authorizationService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
    }

    public String createEmployee(String companyId, CreateEmployeeRequest request) {
        requireOwnerOrAdmin(companyId);

        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisabled(false);

            UserRecord employeeRecord = firebaseGateway.auth().createUser(createRequest);

            Map<String, Object> employeeData = new HashMap<>();
            employeeData.put("companyId", companyId);
            employeeData.put("role", "EMPLOYEE");
            employeeData.put("email", request.getEmail());
            if (request.getName() != null && !request.getName().isBlank()) {
                employeeData.put("name", request.getName());
            }
            employeeData.put("active", true);

            firebaseGateway.collection(FirestoreCollections.EMPLOYEES)
                    .document(employeeRecord.getUid())
                    .set(employeeData);

            return employeeRecord.getUid();
        } catch (Exception exception) {
            throw new ApplicationException("Unable to create employee", exception);
        }
    }

    public List<Map<String, Object>> listEmployees(String companyId) {
        requireOwnerOrAdmin(companyId);

        try {
            QuerySnapshot snapshot = firebaseGateway.collection(FirestoreCollections.EMPLOYEES)
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .get();

            return snapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> map = new HashMap<>(doc.getData());
                        map.put("uid", doc.getId());
                        return map;
                    })
                    .toList();
        } catch (Exception exception) {
            throw new ApplicationException("Unable to list employees", exception);
        }
    }

    private void requireOwnerOrAdmin(String companyId) {
        AuthenticatedUser user = authorizationService.currentUser();
        if (user.getRole().name().equals("ADMIN")) {
            return;
        }
        // OWNER of the same company.
        if (user.getRole().name().equals("OWNER") && companyId != null && companyId.equals(user.getCompanyId())) {
            return;
        }
        throw new com.chhavi.busbuddy_backend.exception.ForbiddenException("Owner or admin access is required");
    }
}
