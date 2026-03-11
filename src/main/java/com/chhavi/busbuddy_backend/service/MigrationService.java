package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.chhavi.busbuddy_backend.security.AuthorizationService;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    private final FirebaseGateway firebaseGateway;
    private final AuthorizationService authorizationService;

    public MigrationService(FirebaseGateway firebaseGateway, AuthorizationService authorizationService) {
        this.firebaseGateway = firebaseGateway;
        this.authorizationService = authorizationService;
    }

    /**
     * Migrates legacy resources that stored company UID as the company identifier.
     *
     * @param legacyCompanyUid old value stored in route.company / bus.company
     * @param newCompanyId new stable companyId (companies/{companyId})
     * @return number of updated documents
     */
    public int migrateCompanyUidToCompanyId(String legacyCompanyUid, String newCompanyId) {
        authorizationService.requireAdmin();

        if (legacyCompanyUid == null || legacyCompanyUid.isBlank()) {
            throw new ApplicationException("legacyCompanyUid must not be blank");
        }
        if (newCompanyId == null || newCompanyId.isBlank()) {
            throw new ApplicationException("newCompanyId must not be blank");
        }

        try {
            int updated = 0;

            // Update routes where company == legacyCompanyUid
            List<QueryDocumentSnapshot> routes = firebaseGateway.collection(FirestoreCollections.ROUTES)
                    .whereEqualTo("company", legacyCompanyUid)
                    .get()
                    .get()
                    .getDocuments();
            for (QueryDocumentSnapshot doc : routes) {
                doc.getReference().update("company", newCompanyId);
                updated++;
            }

            // Update buses where company == legacyCompanyUid
            List<QueryDocumentSnapshot> buses = firebaseGateway.collection(FirestoreCollections.BUSES)
                    .whereEqualTo("company", legacyCompanyUid)
                    .get()
                    .get()
                    .getDocuments();
            for (QueryDocumentSnapshot doc : buses) {
                doc.getReference().update("company", newCompanyId);
                updated++;
            }

            log.info("Migration complete legacyCompanyUid={} -> newCompanyId={}, updatedDocs={}", legacyCompanyUid, newCompanyId, updated);
            return updated;
        } catch (Exception exception) {
            throw new ApplicationException("Migration failed", exception);
        }
    }

    /**
     * Backfills searchKey fields for existing documents.
     *
     * @return number of updated documents
     */
    public int backfillSearchKeys() {
        authorizationService.requireAdmin();

        try {
            int updated = 0;

            int pageSize = migrationPageSize();
            updated += backfillCollectionSearchKey(FirestoreCollections.ROUTES, pageSize);
            updated += backfillCollectionSearchKey(FirestoreCollections.BUSES, pageSize);

            log.info("Backfilled searchKey fields for routes/buses, updatedDocs={}", updated);
            return updated;
        } catch (Exception exception) {
            throw new ApplicationException("SearchKey backfill failed", exception);
        }
    }

    private int migrationPageSize() {
        String raw = System.getenv("MIGRATION_PAGE_SIZE");
        if (raw == null || raw.isBlank()) {
            return 500;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1) {
                throw new IllegalArgumentException("must be > 0");
            }
            // Firestore batch write limit is 500 operations; keep upper bound 500.
            if (value > 500) {
                return 500;
            }
            return value;
        } catch (Exception exception) {
            throw new ApplicationException("Invalid MIGRATION_PAGE_SIZE: " + raw, exception);
        }
    }

    private int backfillCollectionSearchKey(String collectionName, int pageSize) {
        try {
            int updated = 0;
            com.google.cloud.firestore.CollectionReference collection = firebaseGateway.collection(collectionName);

            com.google.cloud.firestore.Query query = collection
                    .orderBy(com.google.cloud.firestore.FieldPath.documentId())
                    .limit(pageSize);

            com.google.cloud.firestore.DocumentSnapshot last = null;
            while (true) {
                com.google.cloud.firestore.Query pageQuery = last == null ? query : query.startAfter(last);
                com.google.cloud.firestore.QuerySnapshot snapshot = pageQuery.get().get();
                if (snapshot.isEmpty()) {
                    break;
                }

                com.google.cloud.firestore.WriteBatch batch = firebaseGateway.firestore().batch();
                int batchUpdates = 0;

                for (com.google.cloud.firestore.QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                    String code = doc.getString("code");
                    if (code == null || code.isBlank()) {
                        continue;
                    }
                    String desired = com.chhavi.busbuddy_backend.util.SearchKeyUtils.normalize(code);
                    String current = doc.getString("searchKey");
                    boolean changed = false;
                    if (current == null || current.isBlank() || !current.equals(desired)) {
                        batch.update(doc.getReference(), "searchKey", desired);
                        changed = true;
                    }

                    if (FirestoreCollections.ROUTES.equals(collectionName)) {
                        var keys = com.chhavi.busbuddy_backend.util.RouteKeyUtils.parseFromTo(code);
                        if (keys != null) {
                            String currentFrom = doc.getString("fromKey");
                            String currentTo = doc.getString("toKey");
                            if (currentFrom == null || !currentFrom.equals(keys.fromKey())) {
                                batch.update(doc.getReference(), "fromKey", keys.fromKey());
                                changed = true;
                            }
                            if (currentTo == null || !currentTo.equals(keys.toKey())) {
                                batch.update(doc.getReference(), "toKey", keys.toKey());
                                changed = true;
                            }
                        }
                    }

                    if (changed) {
                        batchUpdates++;
                        updated++;
                    }
                }

                if (batchUpdates > 0) {
                    batch.commit().get();
                }

                last = snapshot.getDocuments().get(snapshot.size() - 1);

                // If this page returned fewer than pageSize docs, we're done.
                if (snapshot.size() < pageSize) {
                    break;
                }
            }

            return updated;
        } catch (Exception exception) {
            throw new ApplicationException("SearchKey backfill failed for collection: " + collectionName, exception);
        }
    }
}

