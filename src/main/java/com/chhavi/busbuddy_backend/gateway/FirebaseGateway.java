package com.chhavi.busbuddy_backend.gateway;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Component;

@Component
public class FirebaseGateway {

    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    public FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public DatabaseReference realtimeRoot() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public CollectionReference collection(String name) {
        return firestore().collection(name);
    }
}
