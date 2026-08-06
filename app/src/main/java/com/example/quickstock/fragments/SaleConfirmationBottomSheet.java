package com.example.quickstock.fragments;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.quickstock.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Locale;

public class SaleConfirmationBottomSheet
        extends BottomSheetDialogFragment {

    public static final String TAG =
            "SaleConfirmationBottomSheet";

    public static final String REQUEST_KEY =
            "sale_confirmation_request";

    public static final String KEY_CONFIRMED =
            "sale_confirmed";

    private static final String ARG_NAMES =
            "product_names";

    private static final String ARG_QUANTITIES =
            "product_quantities";

    private static final String ARG_SUBTOTALS =
            "product_subtotals";

    private static final String ARG_TOTAL_UNITS =
            "total_units";

    private static final String ARG_TOTAL_AMOUNT =
            "total_amount";

    private static final String ARG_TOTAL_SAVING =
            "total_saving";

    private View reviewContent;
    private View resultContent;
    private View confirmationActions;
    private View rowCustomerSaving;

    private LinearLayout selectedItemsContainer;

    private TextView textConfirmationUnits;
    private TextView textConfirmationSaving;
    private TextView textConfirmationTotal;
    private TextView textConfirmationError;

    private TextView textResultSymbol;
    private TextView textResultTitle;
    private TextView textResultMessage;

    private MaterialButton buttonCancelConfirmation;
    private MaterialButton buttonConfirmSale;
    private MaterialButton buttonResultDone;

    private CircularProgressIndicator progressConfirmSale;

    private boolean processing;

    public static SaleConfirmationBottomSheet newInstance(
            ArrayList<String> productNames,
            ArrayList<Integer> quantities,
            double[] subtotals,
            int totalUnits,
            double totalAmount,
            double totalSaving
    ) {

        SaleConfirmationBottomSheet sheet =
                new SaleConfirmationBottomSheet();

        Bundle arguments = new Bundle();

        arguments.putStringArrayList(
                ARG_NAMES,
                productNames
        );

        arguments.putIntegerArrayList(
                ARG_QUANTITIES,
                quantities
        );

        arguments.putDoubleArray(
                ARG_SUBTOTALS,
                subtotals
        );

        arguments.putInt(
                ARG_TOTAL_UNITS,
                totalUnits
        );

        arguments.putDouble(
                ARG_TOTAL_AMOUNT,
                totalAmount
        );

        arguments.putDouble(
                ARG_TOTAL_SAVING,
                totalSaving
        );

        sheet.setArguments(arguments);

        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.bottom_sheet_sale_confirmation,
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
        displaySaleInformation();
        initialiseActions();
    }

    private void initialiseViews(
            View view
    ) {

        reviewContent =
                view.findViewById(
                        R.id.reviewContent
                );

        resultContent =
                view.findViewById(
                        R.id.resultContent
                );

        confirmationActions =
                view.findViewById(
                        R.id.confirmationActions
                );

        rowCustomerSaving =
                view.findViewById(
                        R.id.rowCustomerSaving
                );

        selectedItemsContainer =
                view.findViewById(
                        R.id.selectedItemsContainer
                );

        textConfirmationUnits =
                view.findViewById(
                        R.id.textConfirmationUnits
                );

        textConfirmationSaving =
                view.findViewById(
                        R.id.textConfirmationSaving
                );

        textConfirmationTotal =
                view.findViewById(
                        R.id.textConfirmationTotal
                );

        textConfirmationError =
                view.findViewById(
                        R.id.textConfirmationError
                );

        textResultSymbol =
                view.findViewById(
                        R.id.textResultSymbol
                );

        textResultTitle =
                view.findViewById(
                        R.id.textResultTitle
                );

        textResultMessage =
                view.findViewById(
                        R.id.textResultMessage
                );

        buttonCancelConfirmation =
                view.findViewById(
                        R.id.buttonCancelConfirmation
                );

        buttonConfirmSale =
                view.findViewById(
                        R.id.buttonConfirmSale
                );

        buttonResultDone =
                view.findViewById(
                        R.id.buttonResultDone
                );

        progressConfirmSale =
                view.findViewById(
                        R.id.progressConfirmSale
                );
    }

    private void displaySaleInformation() {

        Bundle arguments = getArguments();

        if (arguments == null) {
            showError(
                    "Sale information is unavailable."
            );

            return;
        }

        ArrayList<String> names =
                arguments.getStringArrayList(
                        ARG_NAMES
                );

        ArrayList<Integer> quantities =
                arguments.getIntegerArrayList(
                        ARG_QUANTITIES
                );

        double[] subtotals =
                arguments.getDoubleArray(
                        ARG_SUBTOTALS
                );

        int totalUnits =
                arguments.getInt(
                        ARG_TOTAL_UNITS,
                        0
                );

        double totalAmount =
                arguments.getDouble(
                        ARG_TOTAL_AMOUNT,
                        0
                );

        double totalSaving =
                arguments.getDouble(
                        ARG_TOTAL_SAVING,
                        0
                );

        populateProductRows(
                names,
                quantities,
                subtotals
        );

        textConfirmationUnits.setText(
                String.valueOf(totalUnits)
        );

        textConfirmationTotal.setText(
                formatMoney(totalAmount)
        );

        if (totalSaving > 0) {

            rowCustomerSaving.setVisibility(
                    View.VISIBLE
            );

            textConfirmationSaving.setText(
                    formatMoney(totalSaving)
            );

        } else {

            rowCustomerSaving.setVisibility(
                    View.GONE
            );
        }
    }

    private void populateProductRows(
            ArrayList<String> names,
            ArrayList<Integer> quantities,
            double[] subtotals
    ) {

        selectedItemsContainer.removeAllViews();

        if (names == null
                || quantities == null
                || subtotals == null) {

            return;
        }

        int itemCount =
                Math.min(
                        names.size(),
                        Math.min(
                                quantities.size(),
                                subtotals.length
                        )
                );

        for (int index = 0;
             index < itemCount;
             index++) {

            addProductRow(
                    names.get(index),
                    quantities.get(index),
                    subtotals[index]
            );

            if (index < itemCount - 1) {
                addDivider();
            }
        }
    }

    private void addProductRow(
            String productName,
            int quantity,
            double subtotal
    ) {

        LinearLayout row =
                new LinearLayout(
                        requireContext()
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                0,
                dp(12),
                0,
                dp(12)
        );

        TextView nameView =
                new TextView(
                        requireContext()
                );

        nameView.setText(
                productName == null
                        || productName.trim().isEmpty()
                        ? "Product"
                        : productName.trim()
        );

        nameView.setTextSize(15);
        nameView.setMaxLines(2);

        nameView.setTextColor(
                MaterialColors.getColor(
                        selectedItemsContainer,
                        com.google.android.material.R.attr
                                .colorOnSurface
                )
        );

        LinearLayout.LayoutParams nameParameters =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        row.addView(
                nameView,
                nameParameters
        );

        TextView quantityView =
                new TextView(
                        requireContext()
                );

        quantityView.setText(
                "× " + quantity
        );

        quantityView.setTextSize(14);

        LinearLayout.LayoutParams quantityParameters =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        quantityParameters.setMarginStart(
                dp(12)
        );

        row.addView(
                quantityView,
                quantityParameters
        );

        TextView subtotalView =
                new TextView(
                        requireContext()
                );

        subtotalView.setText(
                formatMoney(subtotal)
        );

        subtotalView.setTextSize(15);

        subtotalView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        subtotalView.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.primaryGreen
                )
        );

        LinearLayout.LayoutParams subtotalParameters =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        subtotalParameters.setMarginStart(
                dp(16)
        );

        row.addView(
                subtotalView,
                subtotalParameters
        );

        selectedItemsContainer.addView(row);
    }

    private void addDivider() {

        View divider =
                new View(
                        requireContext()
                );

        divider.setBackgroundColor(
                MaterialColors.getColor(
                        selectedItemsContainer,
                        com.google.android.material.R.attr
                                .colorOutlineVariant
                )
        );

        selectedItemsContainer.addView(
                divider,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                )
        );
    }

    private void initialiseActions() {

        buttonCancelConfirmation
                .setOnClickListener(
                        view -> dismiss()
                );

        buttonConfirmSale
                .setOnClickListener(
                        view -> submitConfirmation()
                );

        buttonResultDone
                .setOnClickListener(
                        view -> dismiss()
                );
    }

    private void submitConfirmation() {

        if (processing) {
            return;
        }

        setProcessing(true);

        Bundle result = new Bundle();

        result.putBoolean(
                KEY_CONFIRMED,
                true
        );

        getParentFragmentManager()
                .setFragmentResult(
                        REQUEST_KEY,
                        result
                );
    }

    private void setProcessing(
            boolean processing
    ) {

        this.processing = processing;

        setCancelable(
                !processing
        );

        progressConfirmSale.setVisibility(
                processing
                        ? View.VISIBLE
                        : View.GONE
        );

        buttonCancelConfirmation.setEnabled(
                !processing
        );

        buttonConfirmSale.setEnabled(
                !processing
        );

        buttonConfirmSale.setText(
                processing
                        ? "Completing..."
                        : "Complete sale"
        );

        textConfirmationError.setVisibility(
                View.GONE
        );
    }

    public void showSuccess(
            double completedTotal,
            String saleId
    ) {

        if (!isAdded()
                || getView() == null) {

            return;
        }

        processing = false;

        setCancelable(false);

        reviewContent.setVisibility(
                View.GONE
        );

        confirmationActions.setVisibility(
                View.GONE
        );

        progressConfirmSale.setVisibility(
                View.GONE
        );

        resultContent.setVisibility(
                View.VISIBLE
        );

        buttonResultDone.setVisibility(
                View.VISIBLE
        );

        textResultSymbol.setText("\u2713");

        textResultSymbol.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.success
                )
        );

        textResultTitle.setText(
                "Sale completed"
        );

        StringBuilder message =
                new StringBuilder();

        message.append(
                formatMoney(completedTotal)
        );

        message.append(
                " was recorded successfully."
        );

        message.append(
                "\n\nInventory quantities have been updated."
        );

        if (saleId != null
                && !saleId.trim().isEmpty()) {

            String cleanId =
                    saleId.trim();

            String reference =
                    cleanId.length() > 8
                            ? cleanId.substring(
                            cleanId.length() - 8
                    )
                            : cleanId;

            message.append(
                    "\n\nReference: "
            );

            message.append(reference);
        }

        textResultMessage.setText(
                message.toString()
        );
    }
    public void showQueued(
            double completedTotal,
            String saleId
    ) {

        if (!isAdded()
                || getView() == null) {

            return;
        }

        processing = false;

        setCancelable(false);

        reviewContent.setVisibility(
                View.GONE
        );

        confirmationActions.setVisibility(
                View.GONE
        );

        progressConfirmSale.setVisibility(
                View.GONE
        );

        resultContent.setVisibility(
                View.VISIBLE
        );

        buttonResultDone.setVisibility(
                View.VISIBLE
        );

        /*
         * Circular arrow represents pending synchronization.
         */
        textResultSymbol.setText("\u21BB");

        textResultSymbol.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.quickStockPurple
                )
        );

        textResultTitle.setText(
                "Sale saved offline"
        );

        StringBuilder message =
                new StringBuilder();

        message.append(
                formatMoney(completedTotal)
        );

        message.append(
                " was saved securely on this device."
        );

        message.append(
                "\n\nInventory quantities have been updated locally."
        );

        message.append(
                "\n\nThe sale will sync automatically when the internet connection is restored."
        );

        if (saleId != null
                && !saleId.trim().isEmpty()) {

            String cleanId =
                    saleId.trim();

            String reference =
                    cleanId.length() > 8
                            ? cleanId.substring(
                            cleanId.length() - 8
                    )
                            : cleanId;

            message.append(
                    "\n\nReference: "
            );

            message.append(reference);
        }

        textResultMessage.setText(
                message.toString()
        );
    }

    public void showError(
            String error
    ) {

        if (!isAdded()
                || getView() == null) {

            return;
        }

        processing = false;

        setCancelable(true);

        progressConfirmSale.setVisibility(
                View.GONE
        );

        buttonCancelConfirmation.setEnabled(
                true
        );

        buttonConfirmSale.setEnabled(
                true
        );

        buttonConfirmSale.setText(
                "Try again"
        );

        String message =
                error == null
                        || error.trim().isEmpty()
                        ? "The sale could not be completed."
                        : error.trim();

        textConfirmationError.setText(
                message
        );

        textConfirmationError.setVisibility(
                View.VISIBLE
        );
    }

    private String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.getDefault(),
                "KSh %,.2f",
                amount
        );
    }

    private int dp(
            int value
    ) {

        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    public void onStart() {

        super.onStart();

        Dialog dialog = getDialog();

        if (!(dialog
                instanceof BottomSheetDialog)) {

            return;
        }

        BottomSheetDialog bottomSheetDialog =
                (BottomSheetDialog) dialog;

        FrameLayout bottomSheet =
                bottomSheetDialog.findViewById(
                        com.google.android.material.R.id
                                .design_bottom_sheet
                );

        if (bottomSheet == null) {
            return;
        }

        ViewGroup.LayoutParams parameters =
                bottomSheet.getLayoutParams();

        parameters.height =
                ViewGroup.LayoutParams.MATCH_PARENT;

        bottomSheet.setLayoutParams(parameters);

        BottomSheetBehavior<FrameLayout> behavior =
                BottomSheetBehavior.from(
                        bottomSheet
                );

        behavior.setSkipCollapsed(true);

        behavior.setState(
                BottomSheetBehavior.STATE_EXPANDED
        );
    }
}