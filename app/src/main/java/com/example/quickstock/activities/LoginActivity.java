package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.quickstock.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private TextView txtForgotPassword;
    private TextView txtRegister;

    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;

    private FirebaseAuth firebaseAuth;

    private boolean authenticationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        configureSystemBars();

        firebaseAuth = FirebaseAuth.getInstance();

        initialiseViews();
        startAnimations();
        setupClickListeners();
    }

    private void configureSystemBars() {
        /*
         * QuickStock currently uses a controlled light theme.
         * Explicitly request dark system-bar icons so Samsung
         * dark mode does not place white icons on the light
         * Login background.
         */
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void initialiseViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtRegister = findViewById(R.id.txtRegister);

        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);
    }

    private void startAnimations() {
        Animation fadeAnimation = AnimationUtils.loadAnimation(
                this,
                R.anim.fade_in
        );

        findViewById(R.id.logoContainer)
                .startAnimation(fadeAnimation);

        findViewById(R.id.emailLayout)
                .startAnimation(fadeAnimation);

        findViewById(R.id.passwordLayout)
                .startAnimation(fadeAnimation);

        txtForgotPassword.startAnimation(fadeAnimation);
        btnLogin.startAnimation(fadeAnimation);
        txtRegister.startAnimation(fadeAnimation);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(view -> loginUser());

        txtRegister.setOnClickListener(view ->
                openRegisterActivity()
        );

        txtForgotPassword.setOnClickListener(view ->
                sendPasswordResetEmail()
        );
    }

    private void loginUser() {
        if (authenticationInProgress) {
            return;
        }

        String email = getNormalisedEmail();
        String password = getInputText(etPassword);

        clearInputErrors();

        if (!validateLoginInput(email, password)) {
            return;
        }

        setLoadingState(true, "Logging in...");

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false, "Log in");

                    if (task.isSuccessful()) {
                        openMainActivity();
                        return;
                    }

                    showAuthenticationError(
                            task.getException(),
                            "Unable to log in. Check your email and password."
                    );
                });
    }

    private boolean validateLoginInput(
            String email,
            String password
    ) {
        if (TextUtils.isEmpty(email)) {
            showInputError(
                    etEmail,
                    "Email address is required"
            );

            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInputError(
                    etEmail,
                    "Enter a valid email address"
            );

            return false;
        }

        if (TextUtils.isEmpty(password)) {
            showInputError(
                    etPassword,
                    "Password is required"
            );

            return false;
        }

        return true;
    }

    private void sendPasswordResetEmail() {
        if (authenticationInProgress) {
            return;
        }

        String email = getNormalisedEmail();

        clearInputErrors();

        if (TextUtils.isEmpty(email)) {
            showInputError(
                    etEmail,
                    "Enter your email address first"
            );

            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInputError(
                    etEmail,
                    "Enter a valid email address"
            );

            return;
        }

        setLoadingState(true, "Please wait...");

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false, "Log in");

                    if (task.isSuccessful()) {
                        showMessage(
                                "Password reset email sent. Check your inbox."
                        );

                        return;
                    }

                    showAuthenticationError(
                            task.getException(),
                            "Unable to send the password reset email."
                    );
                });
    }

    private void openRegisterActivity() {
        Intent intent = new Intent(
                LoginActivity.this,
                RegisterActivity.class
        );

        startActivity(intent);
    }

    private void openMainActivity() {
        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        /*
         * Removes LoginActivity and previous authentication activities
         * from the back stack.
         */
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String getNormalisedEmail() {
        return getInputText(etEmail)
                .toLowerCase(Locale.ROOT);
    }

    private String getInputText(
            TextInputEditText inputEditText
    ) {
        if (inputEditText.getText() == null) {
            return "";
        }

        return inputEditText
                .getText()
                .toString()
                .trim();
    }

    private void showInputError(
            TextInputEditText input,
            String message
    ) {
        input.setError(message);
        input.requestFocus();
    }

    private void clearInputErrors() {
        etEmail.setError(null);
        etPassword.setError(null);
    }

    private void showAuthenticationError(
            Exception exception,
            String fallbackMessage
    ) {
        String errorMessage = fallbackMessage;

        if (exception != null
                && exception.getLocalizedMessage() != null
                && !exception.getLocalizedMessage().trim().isEmpty()) {

            errorMessage = exception.getLocalizedMessage();
        }

        showMessage(errorMessage);
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

    private void setLoadingState(
            boolean loading,
            String buttonText
    ) {
        authenticationInProgress = loading;

        loginProgressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnLogin.setEnabled(!loading);
        txtRegister.setEnabled(!loading);
        txtForgotPassword.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);

        btnLogin.setText(buttonText);
    }
}