package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.quickstock.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_DURATION =
            1100L;

    private final Handler splashHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private FirebaseAuth firebaseAuth;
    private View logoContainer;

    private boolean navigationStarted;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_splash
        );

        firebaseAuth =
                FirebaseAuth.getInstance();

        initialiseViews();
        startSplashAnimation();
        scheduleNavigation();
    }

    private void initialiseViews() {
        logoContainer =
                findViewById(
                        R.id.logoContainer
                );
    }

    private void startSplashAnimation() {
        Animation entranceAnimation =
                AnimationUtils.loadAnimation(
                        this,
                        R.anim.splash_logo_enter
                );

        logoContainer.startAnimation(
                entranceAnimation
        );
    }

    private void scheduleNavigation() {
        splashHandler.postDelayed(
                this::closeSplashScreen,
                SPLASH_DISPLAY_DURATION
        );
    }

    private void closeSplashScreen() {
        if (navigationStarted
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        navigationStarted = true;

        openCorrectScreen();
    }

    private void openCorrectScreen() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        Intent intent;

        if (currentUser == null) {
            intent =
                    new Intent(
                            SplashActivity.this,
                            LoginActivity.class
                    );

        } else if (currentUser.isEmailVerified()) {
            intent =
                    new Intent(
                            SplashActivity.this,
                            MainActivity.class
                    );

        } else {
            intent =
                    new Intent(
                            SplashActivity.this,
                            VerifyEmailActivity.class
                    );

            intent.putExtra(
                    VerifyEmailActivity.EXTRA_EMAIL,
                    currentUser.getEmail()
            );

            intent.putExtra(
                    VerifyEmailActivity.EXTRA_EMAIL_SENT,
                    false
            );
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        overridePendingTransition(0, 0);

        finish();
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacksAndMessages(
                null
        );

        super.onDestroy();
    }
}