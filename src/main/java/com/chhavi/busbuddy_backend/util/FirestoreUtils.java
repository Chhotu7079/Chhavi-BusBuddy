package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.persistence.model.Schedule;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Collects small Firestore-related helpers that are shared broadly enough to
 * avoid duplicating low-level access and integrity checks in services.
 */
public final class FirestoreUtils {

    private FirestoreUtils() {
    }

    public static DocumentSnapshot getDocumentById(CollectionReference collectionReference, String id) {
        try {
            DocumentSnapshot document = collectionReference.document(id).get().get();
            if (!document.exists()) {
                throw new ResourceNotFoundException("Document not found with id: " + id);
            }
            return document;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Thread interrupted while retrieving document " + id, exception);
        } catch (ExecutionException exception) {
            throw new ApplicationException("Unable to retrieve document " + id, exception);
        }
    }

    public static boolean checkDelaysIntegrity(Schedule delays) {
        if (delays == null || delays.getForward() == null || delays.getBack() == null) {
            return false;
        }

        for (Map.Entry<String, List<String>> entry : delays.getForward().entrySet()) {
            for (String time : entry.getValue()) {
                if (time == null) {
                    return false;
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : delays.getBack().entrySet()) {
            for (String time : entry.getValue()) {
                if (time == null) {
                    return false;
                }
            }
        }

        return true;
    }
}
