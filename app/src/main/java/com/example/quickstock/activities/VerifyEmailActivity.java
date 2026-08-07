package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.quickstock.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL =
            "verification_email";

    public static final String EXTRA_EMAIL_SENT =
            "verification_email_sent";

    private TextView textVerificationEmail;
    private TextView textVerificationProgress;

    private MaterialButton buttonCheckVerification;
    private MaterialButton buttonResendVerification;
    private MaterialButton buttonVerificationSignOut;

    private CircularProgressIndicator progressVerification;

    private FirebaseAuth firebaseAuth;

    private boolean requestInProgress;
    private boolean navigationStarted;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_verify_email
        );

        configureSystemBars();

        firebaseAuth =
                FirebaseAuth.getInstance();

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        initialiseViews();
        displayEmailAddress(currentUser);
        initialiseActions();
        displayInitialMessage();
    }

    private void configureSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void initialiseViews() {
        textVerificationEmail =
                findViewById(
                        R.id.textVerificationEmail
                );

        textVerificationProgress =
                findViewById(
                        R.id.textVerificationProgress
                );

        buttonCheckVerification =
                findViewById(
                        R.id.buttonCheckVerification
                );

        buttonResendVerification =
                findViewById(
                        R.id.buttonResendVerification
                );

        buttonVerificationSignOut =
                findViewById(
                        R.id.buttonVerificationSignOut
                );

        progressVerification =
                findViewById(
                        R.id.progressVerification
                );
    }

    private void displayEmailAddress(
            FirebaseUser currentUser
    ) {
        String emailAddress =
                currentUser.getEmail();

        if (emailAddress == null
                || emailAddress.trim().isEmpty()) {

            emailAddress =
                    getIntent().getStringExtra(
                            EXTRA_EMAIL
                    );
        }

        if (emailAddress == null
                || emailAddress.trim().isEmpty()) {

            emailAddress =
                    "your registered email address";
        }

        textVerificationEmail.setText(
                emailAddress.trim()
        );
    }

    private void initialiseActions() {
        buttonCheckVerification.setOnClickListener(
                view ->
                        checkVerificationStatus(true)
        );

        buttonResendVerification.setOnClickListener(
                view ->
                        resendVerificationEmail()
        );

        buttonVerificationSignOut.setOnClickListener(
                view ->
                        confirmSignOut()
        );
    }

    private void displayInitialMessage() {
        boolean emailSent =
                getIntent().getBooleanExtra(
                        EXTRA_EMAIL_SENT,
                        false
                );

        if (emailSent) {
            showMessage(
                    "Verification email sent. Check your inbox and spam folder."
            );

        } else {
            showMessage(
                    "Tap Resend verification email if you have not received the message."
            );
        }
    }

    private void checkVerificationStatus(
            boolean showUnverifiedMessage
    ) {
        if (requestInProgress) {
            return;
        }

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        setLoading(
                true,
                "Checking verification status..."
        );

        currentUser
                .reload()
                .addOnCompleteListener(
                        this,
                        task -> {
                            setLoading(false, "");

                            if (!task.isSuccessful()) {
                                showAuthenticationError(
                                        task.getException(),
                                        "Unable to check your verification status. Check your connection and try again."
                                );

                                return;
                            }

                            FirebaseUser refreshedUser =
                                    firebaseAuth
                                            .getCurrentUser();

                            if (refreshedUser != null
                                    && refreshedUser
                                    .isEmailVerified()) {

                                showVerificationSuccess();
                                return;
                            }

                            if (showUnverifiedMessage) {
                                showMessage(
                                        "Your email is not verified yet. Open the verification link and try again."
                                );
                            }
                        }
                );
    }

    private void resendVerificationEmail() {
        if (requestInProgress) {
            return;
        }

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginActivity();
            return;
        }

        if (currentUser.isEmailVerified()) {
            showVerificationSuccess();
            return;
        }

        setLoading(
                true,
                "Sending another verification email..."
        );

        currentUser
                .sendEmailVerification()
                .addOnCompleteListener(
                        this,
                        task -> {
                            setLoading(false, "");

                            if (task.isSuccessful()) {
                                showMessage(
                                        "Verification email sent. Check your inbox and spam folder."
                                );

                                return;
                            }

                            showAuthenticationError(
                                    task.getException(),
                                    "Unable to send another verification email. Please try again shortly."
                            );
                        }
                );
    }


    private void showVerificationSuccess() {
        if (navigationStarted
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        navigationStarted = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Email verified")
                .setMessage(
                        "Your email address has been verified successfully. You can now continue to QuickStock."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Continue",
                        (dialog, which) ->
                                openMainActivity()
                )
                .show();
    }

    private void confirmSignOut() {
        if (requestInProgress) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Use a different account?")
                .setMessage(
                        "You will be signed out and returned to the login screen."
                )
                .setNegativeButton(
                        "Stay",
                        null
                )
                .setPositiveButton(
                        "Sign out",
                        (dialog, which) -> {
                            firebaseAuth.signOut();
                            openLoginActivity();
                        }
                )
                .show();
    }

    private void setLoading(
            boolean loading,
            String progressMessage
    ) {
        requestInProgress = loading;

        progressVerification.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        textVerificationProgress.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        textVerificationProgress.setText(
                progressMessage == null
                        ? ""
                        : progressMessage
        );

        buttonCheckVerification.setEnabled(!loading);
        buttonResendVerification.setEnabled(!loading);
        buttonVerificationSignOut.setEnabled(!loading);
    }

    private void showAuthenticationError(
            Exception exception,
            String fallbackMessage
    ) {
        String message =
                fallbackMessage;

        if (exception != null
                && exception.getLocalizedMessage() != null
                && !exception
                .getLocalizedMessage()
                .trim()
                .isEmpty()) {

            message =
                    exception
                            .getLocalizedMessage()
                            .trim();
        }

        showMessage(message);
    }

    private void showMessage(
            String message
    ) {
        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar.make(
                findViewById(
                        android.R.id.content
                ),
                safeMessage,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void openMainActivity() {
        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        Intent intent =
                new Intent(
                        VerifyEmailActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void openLoginActivity() {
        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        Intent intent =
                new Intent(
                        VerifyEmailActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}