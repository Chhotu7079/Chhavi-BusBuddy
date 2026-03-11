package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides guarded extraction helpers for Firestore documents so reference
 * casting logic is centralized instead of repeated throughout services.
 */
public final class FirestoreDocumentMapper {

    private FirestoreDocumentMapper() {
    }

    public static DocumentReference getRequiredReference(DocumentSnapshot document, String fieldName) {
        Object value = getValue(document, fieldName);
        if (!(value instanceof DocumentReference reference)) {
            throw new ApplicationException("Expected Firestore reference for field: " + fieldName);
        }
        return reference;
    }

    public static List<DocumentReference> getReferenceList(DocumentSnapshot document, String fieldName) {
        Object value = getValue(document, fieldName);
        if (value == null) {
            return new ArrayList<>();
        }
        if (!(value instanceof List<?> list)) {
            throw new ApplicationException("Expected Firestore reference list for field: " + fieldName);
        }
        List<DocumentReference> references = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof DocumentReference reference)) {
                throw new ApplicationException("Expected Firestore reference item in field: " + fieldName);
            }
            references.add(reference);
        }
        return references;
    }

    private static Object getValue(DocumentSnapshot document, String fieldName) {
        if (fieldName != null && fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            return document.get(FieldPath.of(parts));
        }
        return document.get(fieldName);
    }
}
