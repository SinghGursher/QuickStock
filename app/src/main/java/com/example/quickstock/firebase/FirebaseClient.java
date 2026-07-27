package com.example.quickstock.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseClient {

    private static final String DATABASE_URL =
            "https://quickstock-ed690-default-rtdb.europe-west1.firebasedatabase.app";

    private static final FirebaseDatabase database =
            FirebaseDatabase.getInstance(DATABASE_URL);

    public static final FirebaseAuth auth =
            FirebaseAuth.getInstance();

    public static final DatabaseReference products =
            database.getReference("products");

    public static final DatabaseReference sales =
            database.getReference("sales");

    public static final DatabaseReference users =
            database.getReference("users");

    private FirebaseClient() {
        // Prevent instantiation
    }
}