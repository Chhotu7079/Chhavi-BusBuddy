package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.dto.request.CompanySignupRequest;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Groups company-related operations so authentication-backed onboarding and
 * profile lookup remain separate from bus and route operational logic.
 */
@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final FirebaseGateway firebaseGateway;

    public CompanyService(FirebaseGateway firebaseGateway) {
        this.firebaseGateway = firebaseGateway;
    }

    /**
     * Creates both the Firebase Auth identity and the Firestore company record.
     * The dual write exists because authentication data and business profile
     * data live in different Firebase products.
     */
    public String signupCompany(CompanySignupRequest request) {
        validateSignupRequest(request);

        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setPassword(request.getPassword())
                .setDisabled(false);

        try {
            UserRecord ownerRecord = firebaseGateway.auth().createUser(createRequest);

            // 1) Create company document with a stable companyId (not tied to owner UID).
            Map<String, Object> companyData = new HashMap<>();
            companyData.put("name", request.getCompany());
            companyData.put("email", request.getEmail());
            companyData.put("ownerUid", ownerRecord.getUid());

            String companyId = firebaseGateway.collection(FirestoreCollections.COMPANIES)
                    .add(companyData)
                    .get()
                    .getId();

            // 2) Create employee mapping for the owner.
            Map<String, Object> employeeData = new HashMap<>();
            employeeData.put("companyId", companyId);
            employeeData.put("role", "OWNER");
            employeeData.put("email", request.getEmail());
            employeeData.put("name", request.getCompany());
            employeeData.put("active", true);

            firebaseGateway.collection(FirestoreCollections.EMPLOYEES)
                    .document(ownerRecord.getUid())
                    .set(employeeData);

            log.info("Registered company '{}' with companyId={} ownerUid={}", request.getCompany(), companyId, ownerRecord.getUid());
            return "Azienda registrata con successo con ID: " + companyId;
        } catch (Exception exception) {
            throw new ApplicationException("Errore durante la registrazione dell'azienda: " + exception.getMessage(), exception);
        }
    }

    /**
     * Resolves a company name from its email because downstream callers usually
     * know the login email, not the Firestore document id.
     */
    public String getCompanyByEmail(String email) {
        try {
            Query query = firebaseGateway.collection(FirestoreCollections.COMPANIES).whereEqualTo("email", email);
            QuerySnapshot snapshot = query.get().get();
            if (snapshot.isEmpty()) {
                log.warn("Company lookup failed for email={}", email);
                throw new ResourceNotFoundException("Company not found for email: " + email);
            }
            String companyName = snapshot.getDocuments().get(0).getString("name");
            log.info("Resolved company '{}' for email={}", companyName, email);
            return companyName;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("Unable to retrieve company by email", exception);
        }
    }

    private void validateSignupRequest(CompanySignupRequest request) {
        if (request == null || isBlank(request.getCompany()) || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ApplicationException("I campi email, password e company sono obbligatori");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
