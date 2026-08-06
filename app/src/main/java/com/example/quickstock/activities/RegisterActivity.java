package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickstock.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editOwnerName;
    private TextInputEditText editBusinessName;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirmPassword;

    private MaterialButton buttonRegister;
    private TextView textGoToLogin;
    private ProgressBar registerProgressBar;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        usersReference = FirebaseDatabase
                .getInstance()
                .getReference("users");

        initialiseViews();
        setClickListeners();
    }

    private void initialiseViews() {
        editOwnerName = findViewById(R.id.editOwnerName);
        editBusinessName = findViewById(R.id.editBusinessName);
        editEmail = findViewById(R.id.editRegisterEmail);
        editPassword = findViewById(R.id.editRegisterPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        buttonRegister = findViewById(R.id.buttonRegister);
        textGoToLogin = findViewById(R.id.textGoToLogin);
        registerProgressBar = findViewById(R.id.registerProgressBar);
    }

    private void setClickListeners() {
        buttonRegister.setOnClickListener(view -> registerUser());

        textGoToLogin.setOnClickListener(view -> {
            Intent intent = new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String ownerName = getText(editOwnerName);
        String businessName = getText(editBusinessName);
        String email = getText(editEmail).toLowerCase();
        String password = getText(editPassword);
        String confirmPassword = getText(editConfirmPassword);

        if (!validateInput(
                ownerName,
                businessName,
                email,
                password,
                confirmPassword
        )) {
            return;
        }

        setLoading(true);

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoading(false);

                        String errorMessage = "Registration failed.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        showMessage(errorMessage);

                        return;
                    }

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        setLoading(false);

                        showMessage(
                                "Account created, but the user session could not be found."
                        );

                        return;
                    }

                    saveUserProfile(
                            firebaseUser.getUid(),
                            ownerName,
                            businessName,
                            email
                    );
                });
    }

    private boolean validateInput(
            String ownerName,
            String businessName,
            String email,
            String password,
            String confirmPassword
    ) {
        clearErrors();

        if (TextUtils.isEmpty(ownerName)) {
            editOwnerName.setError("Owner name is required");
            editOwnerName.requestFocus();
            return false;
        }

        if (ownerName.length() < 2) {
            editOwnerName.setError(
                    "Enter a valid owner name"
            );
            editOwnerName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(businessName)) {
            editBusinessName.setError(
                    "Business name is required"
            );
            editBusinessName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email address is required");
            editEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError(
                    "Enter a valid email address"
            );
            editEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editPassword.setError(
                    "Password must contain at least 6 characters"
            );
            editPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editConfirmPassword.setError(
                    "Confirm your password"
            );
            editConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError(
                    "Passwords do not match"
            );
            editConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveUserProfile(
            String userId,
            String ownerName,
            String businessName,
            String email
    ) {
        Map<String, Object> userProfile = new HashMap<>();

        userProfile.put("userId", userId);
        userProfile.put("ownerName", ownerName);
        userProfile.put("businessName", businessName);
        userProfile.put("email", email);
        userProfile.put(
                "createdAt",
                System.currentTimeMillis()
        );

        usersReference
                .child(userId)
                .setValue(userProfile)
                .addOnCompleteListener(task -> {

                    setLoading(false);

                    if (task.isSuccessful()) {
                        openMainActivity();

                        openMainActivity();
                    } else {
                        String errorMessage =
                                "Account created, but the profile could not be saved.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        }

                        showMessage(errorMessage);
                    }
                });
    }

    private void openMainActivity() {
        Intent intent = new Intent(
                RegisterActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String getText(
            TextInputEditText editText
    ) {
        if (editText.getText() == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private void clearErrors() {
        editOwnerName.setError(null);
        editBusinessName.setError(null);
        editEmail.setError(null);
        editPassword.setError(null);
        editConfirmPassword.setError(null);
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
    private void setLoading(boolean loading) {
        registerProgressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonRegister.setEnabled(!loading);
        textGoToLogin.setEnabled(!loading);

        buttonRegister.setText(
                loading ? "Creating account..." : "Create Account"
        );
    }
}