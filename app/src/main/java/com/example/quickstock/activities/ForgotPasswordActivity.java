package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.quickstock.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseTooManyRequestsException;

import java.util.Locale;

public class ForgotPasswordActivity
        extends AppCompatActivity {

    public static final String EXTRA_EMAIL =
            "password_reset_email";

    private MaterialToolbar toolbarForgotPassword;

    private TextInputEditText editResetEmail;

    private MaterialButton buttonSendResetEmail;

    private CircularProgressIndicator
            progressPasswordReset;

    private FirebaseAuth firebaseAuth;

    private boolean requestInProgress;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_forgot_password
        );

        configureSystemBars();

        firebaseAuth =
                FirebaseAuth.getInstance();

        initialiseViews();
        prefillEmailAddress();
        initialiseActions();
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
        toolbarForgotPassword =
                findViewById(
                        R.id.toolbarForgotPassword
                );

        editResetEmail =
                findViewById(
                        R.id.editResetEmail
                );

        buttonSendResetEmail =
                findViewById(
                        R.id.buttonSendResetEmail
                );

        progressPasswordReset =
                findViewById(
                        R.id.progressPasswordReset
                );
    }

    private void prefillEmailAddress() {
        String existingEmail =
                getIntent().getStringExtra(
                        EXTRA_EMAIL
                );

        if (existingEmail == null
                || existingEmail.trim().isEmpty()) {

            return;
        }

        editResetEmail.setText(
                existingEmail.trim()
        );

        editResetEmail.setSelection(
                editResetEmail.length()
        );
    }

    private void initialiseActions() {
        toolbarForgotPassword
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );

        buttonSendResetEmail
                .setOnClickListener(
                        view ->
                                sendPasswordResetEmail()
                );

        editResetEmail
                .setOnEditorActionListener(
                        (textView, actionId, event) -> {
                            sendPasswordResetEmail();
                            return true;
                        }
                );
    }

    private void sendPasswordResetEmail() {
        if (requestInProgress) {
            return;
        }

        String email =
                getEmailAddress();

        editResetEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            editResetEmail.setError(
                    "Email address is required"
            );

            editResetEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            editResetEmail.setError(
                    "Enter a valid email address"
            );

            editResetEmail.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(
                        this,
                        task -> {
                            setLoading(false);

                            if (task.isSuccessful()) {
                                showResetEmailSent(
                                        email
                                );

                                return;
                            }

                            showPasswordResetError(
                                    task.getException()
                            );
                        }
                );
    }

    private void showResetEmailSent(
            String email
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Check your email")
                .setMessage(
                        "If a QuickStock account exists for "
                                + email
                                + ", password-reset instructions have been sent. Check your inbox and spam folder."
                )
                .setCancelable(false)
                .setNegativeButton(
                        "Stay here",
                        null
                )
                .setPositiveButton(
                        "Back to login",
                        (dialog, which) ->
                                returnToLogin()
                )
                .show();
    }

    private void showPasswordResetError(
            Exception exception
    ) {
        String message =
                "Unable to send the password-reset email. Please try again.";

        if (exception
                instanceof FirebaseNetworkException) {

            message =
                    "No internet connection. Connect to the internet and try again.";

        } else if (exception
                instanceof FirebaseTooManyRequestsException) {

            message =
                    "Too many reset attempts were made. Please wait a few minutes and try again.";
        }

        showMessage(message);
    }

    private String getEmailAddress() {
        if (editResetEmail.getText() == null) {
            return "";
        }

        return editResetEmail
                .getText()
                .toString()
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private void setLoading(
            boolean loading
    ) {
        requestInProgress = loading;

        progressPasswordReset.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        editResetEmail.setEnabled(!loading);
        buttonSendResetEmail.setEnabled(!loading);

        buttonSendResetEmail.setText(
                loading
                        ? "Sending..."
                        : "Send reset link"
        );
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

    private void returnToLogin() {
        Intent intent =
                new Intent(
                        ForgotPasswordActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }
}