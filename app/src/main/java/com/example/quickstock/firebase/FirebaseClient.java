package com.example.quickstock.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseClient {

    private static final String DATABASE_URL =
            "https://quickstock-ed690-default-rtdb.europe-west1.firebasedatabase.app";

    private static final FirebaseDatabase database =
            FirebaseDatabase.getInstance(DATABASE_URL);

    public static final FirebaseAuth auth =
            FirebaseAuth.getInstance();

    private FirebaseClient() {
        // Prevent instantiation
    }

    public static DatabaseReference getUsersReference() {
        return database.getReference("users");
    }

    public static DatabaseReference getCurrentUserReference() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("users")
                .child(currentUser.getUid());
    }

    public static DatabaseReference getProductsReference() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("products")
                .child(currentUser.getUid());
    }

    public static DatabaseReference getSalesReference() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("sales")
                .child(currentUser.getUid());
    }

    public static String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return currentUser.getUid();
    }

    public static boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}