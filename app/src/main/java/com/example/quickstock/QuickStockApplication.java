package com.example.quickstock;

import android.app.Application;

import com.example.quickstock.firebase.FirebaseClient;

public class QuickStockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Persistence must be configured before any
         * DatabaseReference is created.
         */
        FirebaseClient.enableOfflinePersistence();

        /*
         * Maintains an application-wide Firebase
         * connection state for repositories.
         */
        FirebaseClient.startConnectionStateTracking();

        /*
         * Keeps the current user's data cached and
         * automatically switches paths after login/logout.
         */
        FirebaseClient.startUserDataSyncManagement();
    }
}