package com.example.quickstock.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.example.quickstock.R;
import com.example.quickstock.activities.LoginActivity;
import com.example.quickstock.firebase.FirebaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private View settingsRoot;

    private TextView textProfileInitial;
    private TextView textProfileSummary;
    private TextView textAppVersion;

    private TextInputLayout layoutOwnerName;
    private TextInputLayout layoutBusinessName;

    private TextInputEditText editOwnerName;
    private TextInputEditText editBusinessName;
    private TextInputEditText editAccountEmail;

    private MaterialButton buttonSaveProfile;
    private MaterialButton buttonSignOut;

    private CircularProgressIndicator progressSettings;

    private DatabaseReference profileReference;
    private FirebaseUser currentUser;

    private String originalOwnerName = "";
    private String originalBusinessName = "";

    private boolean operationInProgress;

    public SettingsFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_settings,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(
                view,
                savedInstanceState
        );

        initialiseViews(view);
        initialiseAccount();
        initialiseActions();
        displayAppInformation();

        loadProfile();
    }

    private void initialiseViews(
            View view
    ) {
        settingsRoot =
                view.findViewById(
                        R.id.settingsRoot
                );

        textProfileInitial =
                view.findViewById(
                        R.id.textProfileInitial
                );

        textProfileSummary =
                view.findViewById(
                        R.id.textProfileSummary
                );

        textAppVersion =
                view.findViewById(
                        R.id.textAppVersion
                );

        layoutOwnerName =
                view.findViewById(
                        R.id.layoutOwnerName
                );

        layoutBusinessName =
                view.findViewById(
                        R.id.layoutBusinessName
                );

        editOwnerName =
                view.findViewById(
                        R.id.editOwnerName
                );

        editBusinessName =
                view.findViewById(
                        R.id.editBusinessName
                );

        editAccountEmail =
                view.findViewById(
                        R.id.editAccountEmail
                );

        buttonSaveProfile =
                view.findViewById(
                        R.id.buttonSaveProfile
                );

        buttonSignOut =
                view.findViewById(
                        R.id.buttonSignOut
                );

        progressSettings =
                view.findViewById(
                        R.id.progressSettings
                );
    }

    private void initialiseAccount() {
        currentUser =
                FirebaseClient.auth
                        .getCurrentUser();

        profileReference =
                FirebaseClient
                        .getCurrentUserReference();

        if (currentUser == null
                || profileReference == null) {

            returnToLogin();
            return;
        }

        editAccountEmail.setText(
                getSafeText(
                        currentUser.getEmail()
                )
        );
    }

    private void initialiseActions() {
        buttonSaveProfile.setOnClickListener(
                view -> saveProfile()
        );

        buttonSignOut.setOnClickListener(
                view -> showSignOutConfirmation()
        );
    }

    private void displayAppInformation() {
        String versionName = "1.0";

        try {
            PackageInfo packageInfo =
                    requireContext()
                            .getPackageManager()
                            .getPackageInfo(
                                    requireContext()
                                            .getPackageName(),
                                    0
                            );

            if (packageInfo.versionName != null
                    && !packageInfo.versionName
                    .trim()
                    .isEmpty()) {

                versionName =
                        packageInfo.versionName.trim();
            }

        } catch (
                PackageManager.NameNotFoundException exception
        ) {
            /*
             * Keep the safe fallback version.
             */
        }

        textAppVersion.setText(
                String.format(
                        Locale.getDefault(),
                        "Version %s",
                        versionName
                )
        );
    }

    private void loadProfile() {
        if (profileReference == null) {
            return;
        }

        setLoading(true);

        profileReference
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {
                                if (!isAdded()
                                        || getView() == null) {

                                    return;
                                }

                                setLoading(false);

                                String ownerName =
                                        snapshot
                                                .child(
                                                        "ownerName"
                                                )
                                                .getValue(
                                                        String.class
                                                );

                                String businessName =
                                        snapshot
                                                .child(
                                                        "businessName"
                                                )
                                                .getValue(
                                                        String.class
                                                );

                                String savedEmail =
                                        snapshot
                                                .child(
                                                        "email"
                                                )
                                                .getValue(
                                                        String.class
                                                );

                                originalOwnerName =
                                        getSafeText(
                                                ownerName
                                        );

                                originalBusinessName =
                                        getSafeText(
                                                businessName
                                        );

                                editOwnerName.setText(
                                        originalOwnerName
                                );

                                editBusinessName.setText(
                                        originalBusinessName
                                );

                                String authenticationEmail =
                                        currentUser == null
                                                ? ""
                                                : getSafeText(
                                                currentUser
                                                        .getEmail()
                                        );

                                String displayedEmail =
                                        !authenticationEmail.isEmpty()
                                                ? authenticationEmail
                                                : getSafeText(
                                                savedEmail
                                        );

                                editAccountEmail.setText(
                                        displayedEmail
                                );

                                updateProfileHeader(
                                        originalOwnerName,
                                        originalBusinessName
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                if (!isAdded()
                                        || getView() == null) {

                                    return;
                                }

                                setLoading(false);

                                showMessage(
                                        getErrorMessage(
                                                error.getMessage(),
                                                "Your profile could not be loaded."
                                        )
                                );
                            }
                        }
                );
    }

    private void saveProfile() {
        if (operationInProgress
                || profileReference == null
                || currentUser == null) {

            return;
        }

        clearErrors();

        String ownerName =
                getInputText(
                        editOwnerName
                );

        String businessName =
                getInputText(
                        editBusinessName
                );

        if (ownerName.isEmpty()) {
            layoutOwnerName.setError(
                    "Owner name is required."
            );

            editOwnerName.requestFocus();
            return;
        }

        if (ownerName.length() < 2) {
            layoutOwnerName.setError(
                    "Enter a valid owner name."
            );

            editOwnerName.requestFocus();
            return;
        }

        if (businessName.isEmpty()) {
            layoutBusinessName.setError(
                    "Business name is required."
            );

            editBusinessName.requestFocus();
            return;
        }

        if (ownerName.equals(
                originalOwnerName
        ) && businessName.equals(
                originalBusinessName
        )) {

            showMessage(
                    "No profile changes to save."
            );

            return;
        }

        setLoading(true);

        String email =
                getSafeText(
                        currentUser.getEmail()
                );

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "userId",
                currentUser.getUid()
        );

        updates.put(
                "ownerName",
                ownerName
        );

        updates.put(
                "businessName",
                businessName
        );

        updates.put(
                "email",
                email
        );

        updates.put(
                "updatedAt",
                ServerValue.TIMESTAMP
        );

        profileReference
                .updateChildren(updates)
                .addOnCompleteListener(
                        task -> {
                            if (!isAdded()
                                    || getView() == null) {

                                return;
                            }

                            setLoading(false);

                            if (!task.isSuccessful()) {
                                String error =
                                        task.getException() == null
                                                ? null
                                                : task.getException()
                                                .getLocalizedMessage();

                                showMessage(
                                        getErrorMessage(
                                                error,
                                                "Your profile could not be updated."
                                        )
                                );

                                return;
                            }

                            originalOwnerName =
                                    ownerName;

                            originalBusinessName =
                                    businessName;

                            updateProfileHeader(
                                    ownerName,
                                    businessName
                            );

                            showMessage(
                                    "Profile updated successfully."
                            );
                        }
                );
    }

    private void updateProfileHeader(
            String ownerName,
            String businessName
    ) {
        String safeOwnerName =
                getSafeText(
                        ownerName
                );

        String safeBusinessName =
                getSafeText(
                        businessName
                );

        String summary;

        if (!safeBusinessName.isEmpty()) {
            summary = safeBusinessName;

        } else if (!safeOwnerName.isEmpty()) {
            summary = safeOwnerName;

        } else {
            summary = "QuickStock business";
        }

        textProfileSummary.setText(
                summary
        );

        String initial =
                summary.substring(
                                0,
                                1
                        )
                        .toUpperCase(
                                Locale.getDefault()
                        );

        textProfileInitial.setText(
                initial
        );
    }

    private void showSignOutConfirmation() {
        if (operationInProgress) {
            return;
        }

        new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle(
                        "Sign out?"
                )
                .setMessage(
                        "You will need to enter your email and password to access QuickStock again."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Sign out",
                        (dialog, which) ->
                                signOut()
                )
                .show();
    }

    private void signOut() {
        FirebaseClient.auth.signOut();
        returnToLogin();
    }

    private void returnToLogin() {
        if (!isAdded()) {
            return;
        }

        Intent intent =
                new Intent(
                        requireContext(),
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        requireActivity().finish();
    }

    private void setLoading(
            boolean loading
    ) {
        operationInProgress = loading;

        progressSettings.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        editOwnerName.setEnabled(
                !loading
        );

        editBusinessName.setEnabled(
                !loading
        );

        buttonSaveProfile.setEnabled(
                !loading
        );

        buttonSignOut.setEnabled(
                !loading
        );

        buttonSaveProfile.setText(
                loading
                        ? "Saving..."
                        : "Save Changes"
        );
    }

    private void clearErrors() {
        layoutOwnerName.setError(null);
        layoutBusinessName.setError(null);
    }

    private String getInputText(
            TextInputEditText input
    ) {
        if (input.getText() == null) {
            return "";
        }

        return input
                .getText()
                .toString()
                .trim();
    }

    private String getSafeText(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String getErrorMessage(
            String error,
            String fallback
    ) {
        if (error == null
                || error.trim().isEmpty()) {

            return fallback;
        }

        return error.trim();
    }

    private void showMessage(
            String message
    ) {
        if (!isAdded()
                || settingsRoot == null) {

            return;
        }

        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar snackbar =
                Snackbar.make(
                        settingsRoot,
                        safeMessage,
                        Snackbar.LENGTH_LONG
                );

        if (buttonSaveProfile != null
                && buttonSaveProfile.getVisibility()
                == View.VISIBLE) {

            snackbar.setAnchorView(
                    buttonSaveProfile
            );
        }

        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        settingsRoot = null;

        textProfileInitial = null;
        textProfileSummary = null;
        textAppVersion = null;

        layoutOwnerName = null;
        layoutBusinessName = null;

        editOwnerName = null;
        editBusinessName = null;
        editAccountEmail = null;

        buttonSaveProfile = null;
        buttonSignOut = null;
        progressSettings = null;

        super.onDestroyView();
    }
}