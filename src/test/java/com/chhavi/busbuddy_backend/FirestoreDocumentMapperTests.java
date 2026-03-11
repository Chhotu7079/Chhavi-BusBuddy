package com.chhavi.busbuddy_backend;

import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirestoreDocumentMapperTests {

    @Test
    void getReferenceListWithDottedPathUsesFieldPath() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        DocumentReference ref = mock(DocumentReference.class);

        FieldPath expectedPath = FieldPath.of("stops", "forward");
        when(snapshot.get(expectedPath)).thenReturn(List.of(ref));

        List<DocumentReference> refs = FirestoreDocumentMapper.getReferenceList(snapshot, "stops.forward");

        assertEquals(1, refs.size());
        assertEquals(ref, refs.get(0));
        verify(snapshot).get(expectedPath);
    }

    @Test
    void getRequiredReferenceWithDottedPathUsesFieldPath() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        DocumentReference ref = mock(DocumentReference.class);

        FieldPath expectedPath = FieldPath.of("route", "ref");
        when(snapshot.get(expectedPath)).thenReturn(ref);

        DocumentReference result = FirestoreDocumentMapper.getRequiredReference(snapshot, "route.ref");

        assertEquals(ref, result);
        verify(snapshot).get(expectedPath);
    }

    @Test
    void getRequiredReferenceThrowsWhenWrongType() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.get("route")).thenReturn("not-a-reference");

        assertThrows(ApplicationException.class, () -> FirestoreDocumentMapper.getRequiredReference(snapshot, "route"));
    }
}
