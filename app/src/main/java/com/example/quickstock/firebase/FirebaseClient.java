package com.example.quickstock.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class FirebaseClient {

    private static final String TAG =
            "FirebaseClient";

    private static final String DATABASE_URL =
            "https://quickstock-ed690-default-rtdb.europe-west1.firebasedatabase.app";

    private static final FirebaseDatabase database =
            FirebaseDatabase.getInstance(DATABASE_URL);

    public static final FirebaseAuth auth =
            FirebaseAuth.getInstance();

    private static boolean persistenceConfigured = false;
    private static boolean syncManagementStarted = false;
    private static boolean connectionTrackingStarted = false;

    /*
     * Volatile allows repository callbacks running on
     * different threads to read the latest connection state.
     */
    private static volatile boolean connectionStateKnown = false;
    private static volatile boolean firebaseConnected = false;

    private static String synchronizedUserId;

    private FirebaseClient() {
        // Prevent instantiation.
    }

    public static synchronized void enableOfflinePersistence() {
        if (persistenceConfigured) {
            return;
        }

        try {
            database.setPersistenceEnabled(true);

            persistenceConfigured = true;

            Log.d(
                    TAG,
                    "Firebase offline persistence enabled."
            );

        } catch (DatabaseException exception) {
            persistenceConfigured = true;

            Log.w(
                    TAG,
                    "Firebase persistence was already configured "
                            + "or the database was already in use.",
                    exception
            );
        }
    }

    public static synchronized void startConnectionStateTracking() {
        if (connectionTrackingStarted) {
            return;
        }

        connectionTrackingStarted = true;

        getConnectionReference()
                .addValueEventListener(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {
                                Boolean connected =
                                        snapshot.getValue(
                                                Boolean.class
                                        );

                                if (connected == null) {
                                    return;
                                }

                                connectionStateKnown = true;
                                firebaseConnected = connected;

                                Log.d(
                                        TAG,
                                        connected
                                                ? "Firebase connection state: online."
                                                : "Firebase connection state: offline."
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                Log.w(
                                        TAG,
                                        "Firebase connection-state tracking was cancelled.",
                                        error.toException()
                                );
                            }
                        }
                );
    }

    public static boolean isConnectionStateKnown() {
        return connectionStateKnown;
    }

    public static boolean isFirebaseConnected() {
        return connectionStateKnown
                && firebaseConnected;
    }

    public static boolean isDefinitelyOffline() {
        return connectionStateKnown
                && !firebaseConnected;
    }

    public static synchronized void startUserDataSyncManagement() {
        if (syncManagementStarted) {
            return;
        }

        syncManagementStarted = true;

        auth.addAuthStateListener(
                firebaseAuth ->
                        updateSynchronizedUser(
                                firebaseAuth.getCurrentUser()
                        )
        );
    }

    private static synchronized void updateSynchronizedUser(
            FirebaseUser currentUser
    ) {
        String currentUserId =
                currentUser == null
                        ? null
                        : currentUser.getUid();

        if (synchronizedUserId != null
                && !synchronizedUserId.equals(currentUserId)) {

            setUserDataKeepSynced(
                    synchronizedUserId,
                    false
            );
        }

        if (currentUserId != null
                && !currentUserId.equals(synchronizedUserId)) {

            setUserDataKeepSynced(
                    currentUserId,
                    true
            );
        }

        synchronizedUserId = currentUserId;
    }

    private static void setUserDataKeepSynced(
            String userId,
            boolean keepSynced
    ) {
        if (userId == null
                || userId.trim().isEmpty()) {
            return;
        }

        String cleanUserId =
                userId.trim();

        try {
            database
                    .getReference("users")
                    .child(cleanUserId)
                    .keepSynced(keepSynced);

            database
                    .getReference("products")
                    .child(cleanUserId)
                    .keepSynced(keepSynced);

            database
                    .getReference("sales")
                    .child(cleanUserId)
                    .keepSynced(keepSynced);

            Log.d(
                    TAG,
                    keepSynced
                            ? "User data synchronization enabled."
                            : "User data synchronization disabled."
            );

        } catch (DatabaseException exception) {
            Log.e(
                    TAG,
                    "Could not update user synchronization.",
                    exception
            );
        }
    }

    public static DatabaseReference getConnectionReference() {
        return database.getReference(
                ".info/connected"
        );
    }

    public static DatabaseReference getUsersReference() {
        return database.getReference("users");
    }

    public static DatabaseReference getCurrentUserReference() {
        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("users")
                .child(currentUser.getUid());
    }

    public static DatabaseReference getProductsReference() {
        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("products")
                .child(currentUser.getUid());
    }

    public static DatabaseReference getSalesReference() {
        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return database
                .getReference("sales")
                .child(currentUser.getUid());
    }

    public static String getCurrentUserId() {
        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return currentUser.getUid();
    }

    public static boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}