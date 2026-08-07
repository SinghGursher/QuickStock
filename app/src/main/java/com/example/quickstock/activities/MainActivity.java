package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.quickstock.R;
import com.example.quickstock.firebase.FirebaseClient;
import com.example.quickstock.fragments.DashboardFragment;
import com.example.quickstock.fragments.InventoryFragment;
import com.example.quickstock.fragments.SalesFragment;
import com.example.quickstock.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private TextView textOfflineBanner;

    private DatabaseReference connectionReference;
    private ValueEventListener connectionStateListener;

    private boolean connectionListenerAttached;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        if (redirectIfAuthenticationIsInvalid()) {
            return;
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_main
        );

        initialiseViews();
        configureSystemInsets();
        configureBottomNavigation();
        configureConnectionMonitoring();

        if (savedInstanceState == null) {
            loadFragment(
                    new DashboardFragment()
            );
        }
    }

    private boolean redirectIfAuthenticationIsInvalid() {
        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            openAuthenticationScreen(
                    LoginActivity.class,
                    null
            );

            return true;
        }

        /*
         * Use the locally cached verified state here rather than
         * calling reload(). This allows previously verified users
         * to continue opening QuickStock while offline.
         */
        if (!currentUser.isEmailVerified()) {
            Intent verificationIntent =
                    new Intent(
                            MainActivity.this,
                            VerifyEmailActivity.class
                    );

            verificationIntent.putExtra(
                    VerifyEmailActivity.EXTRA_EMAIL,
                    currentUser.getEmail()
            );

            verificationIntent.putExtra(
                    VerifyEmailActivity.EXTRA_EMAIL_SENT,
                    false
            );

            verificationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(verificationIntent);
            finish();

            return true;
        }

        return false;
    }

    private void openAuthenticationScreen(
            Class<?> destination,
            Bundle extras
    ) {
        Intent intent =
                new Intent(
                        MainActivity.this,
                        destination
                );

        if (extras != null) {
            intent.putExtras(extras);
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void initialiseViews() {
        bottomNavigation =
                findViewById(
                        R.id.bottomNavigation
                );

        textOfflineBanner =
                findViewById(
                        R.id.textOfflineBanner
                );
    }

    private void configureSystemInsets() {
        View root =
                findViewById(
                        R.id.main
                );

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                bottomNavigation,
                (view, windowInsets) -> {
                    view.setPadding(
                            0,
                            0,
                            0,
                            0
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(
                bottomNavigation
        );
    }

    private void configureBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(
                item -> {
                    Fragment selectedFragment =
                            null;

                    int id =
                            item.getItemId();

                    if (id == R.id.nav_dashboard) {
                        selectedFragment =
                                new DashboardFragment();

                    } else if (id
                            == R.id.nav_inventory) {

                        selectedFragment =
                                new InventoryFragment();

                    } else if (id
                            == R.id.nav_sales) {

                        selectedFragment =
                                new SalesFragment();

                    } else if (id
                            == R.id.nav_settings) {

                        selectedFragment =
                                new SettingsFragment();
                    }

                    if (selectedFragment != null) {
                        loadFragment(
                                selectedFragment
                        );

                        return true;
                    }

                    return false;
                }
        );
    }

    private void configureConnectionMonitoring() {
        connectionReference =
                FirebaseClient
                        .getConnectionReference();

        connectionStateListener =
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

                        displayConnectionState(
                                connected
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        /*
                         * A cancelled connectivity listener does
                         * not necessarily mean the device is offline.
                         */
                    }
                };
    }

    private void displayConnectionState(
            boolean connected
    ) {
        if (textOfflineBanner == null) {
            return;
        }

        textOfflineBanner.setVisibility(
                connected
                        ? View.GONE
                        : View.VISIBLE
        );

        if (!connected) {
            textOfflineBanner
                    .announceForAccessibility(
                            "QuickStock is offline. Showing saved data."
                    );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (connectionReference == null
                || connectionStateListener == null
                || connectionListenerAttached) {

            return;
        }

        connectionReference
                .addValueEventListener(
                        connectionStateListener
                );

        connectionListenerAttached = true;
    }

    @Override
    protected void onStop() {
        if (connectionReference != null
                && connectionStateListener != null
                && connectionListenerAttached) {

            connectionReference
                    .removeEventListener(
                            connectionStateListener
                    );

            connectionListenerAttached = false;
        }

        super.onStop();
    }

    public void setBottomNavigationVisible(
            boolean visible
    ) {
        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void loadFragment(
            Fragment fragment
    ) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        fragment
                )
                .commit();
    }

    @Override
    protected void onDestroy() {
        bottomNavigation = null;
        textOfflineBanner = null;
        connectionReference = null;
        connectionStateListener = null;

        super.onDestroy();
    }
}