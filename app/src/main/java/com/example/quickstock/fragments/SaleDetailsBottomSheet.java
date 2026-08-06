package com.example.quickstock.fragments;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.quickstock.R;
import com.example.quickstock.models.Sale;
import com.example.quickstock.models.SaleItem;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaleDetailsBottomSheet
        extends BottomSheetDialogFragment {

    public static final String TAG =
            "SaleDetailsBottomSheet";

    private static final String ARG_SALE_ID =
            "sale_id";

    private static final String ARG_TIMESTAMP =
            "sale_timestamp";

    private static final String ARG_TOTAL_UNITS =
            "sale_total_units";

    private static final String ARG_TOTAL_AMOUNT =
            "sale_total_amount";

    private static final String ARG_TOTAL_COST =
            "sale_total_cost";

    private static final String ARG_TOTAL_PROFIT =
            "sale_total_profit";

    private static final String ARG_TOTAL_SAVING =
            "sale_total_saving";

    private static final String ARG_ITEM_NAMES =
            "sale_item_names";

    private static final String ARG_ITEM_QUANTITIES =
            "sale_item_quantities";

    private static final String ARG_ITEM_SUBTOTALS =
            "sale_item_subtotals";

    private static final String ARG_ITEM_SAVINGS =
            "sale_item_savings";

    private static final String ARG_ITEM_OFFERS =
            "sale_item_offers";

    private static final Locale KENYAN_LOCALE =
            new Locale("en", "KE");

    private TextView textDetailsReference;
    private TextView textDetailsDate;
    private TextView textDetailsUnits;
    private TextView textDetailsRevenue;
    private TextView textDetailsCost;
    private TextView textDetailsSaving;
    private TextView textDetailsProfit;

    private View rowDetailsSaving;

    private LinearLayout saleItemsContainer;

    private MaterialButton buttonCloseSaleDetails;

    public static SaleDetailsBottomSheet newInstance(
            Sale sale
    ) {
        SaleDetailsBottomSheet sheet =
                new SaleDetailsBottomSheet();

        Bundle arguments =
                new Bundle();

        if (sale == null) {
            sheet.setArguments(arguments);
            return sheet;
        }

        /*
         * Recalculate each item and the complete sale before
         * creating the read-only display snapshot.
         */
        List<SaleItem> validItems =
                new ArrayList<>();

        List<SaleItem> saleItems =
                sale.getItems();

        if (saleItems != null) {
            for (SaleItem item : saleItems) {
                if (item == null
                        || item.getQuantity() <= 0) {

                    continue;
                }

                item.recalculate();
                validItems.add(item);
            }
        }

        sale.setItems(validItems);
        sale.recalculateTotals();

        arguments.putString(
                ARG_SALE_ID,
                sale.getId()
        );

        arguments.putLong(
                ARG_TIMESTAMP,
                sale.getTimestamp()
        );

        arguments.putInt(
                ARG_TOTAL_UNITS,
                sale.getTotalItems()
        );

        arguments.putDouble(
                ARG_TOTAL_AMOUNT,
                sale.getTotalAmount()
        );

        arguments.putDouble(
                ARG_TOTAL_COST,
                sale.getTotalCost()
        );

        arguments.putDouble(
                ARG_TOTAL_PROFIT,
                sale.getTotalProfit()
        );

        arguments.putDouble(
                ARG_TOTAL_SAVING,
                sale.getTotalCustomerSaving()
        );

        ArrayList<String> itemNames =
                new ArrayList<>();

        ArrayList<Integer> itemQuantities =
                new ArrayList<>();

        double[] itemSubtotals =
                new double[validItems.size()];

        double[] itemSavings =
                new double[validItems.size()];

        boolean[] itemOffers =
                new boolean[validItems.size()];

        for (int index = 0;
             index < validItems.size();
             index++) {

            SaleItem item =
                    validItems.get(index);

            itemNames.add(
                    getSafeProductName(item)
            );

            itemQuantities.add(
                    item.getQuantity()
            );

            itemSubtotals[index] =
                    item.getSubtotal();

            itemSavings[index] =
                    item.getCustomerSaving();

            itemOffers[index] =
                    item.isQuantityOfferApplied();
        }

        arguments.putStringArrayList(
                ARG_ITEM_NAMES,
                itemNames
        );

        arguments.putIntegerArrayList(
                ARG_ITEM_QUANTITIES,
                itemQuantities
        );

        arguments.putDoubleArray(
                ARG_ITEM_SUBTOTALS,
                itemSubtotals
        );

        arguments.putDoubleArray(
                ARG_ITEM_SAVINGS,
                itemSavings
        );

        arguments.putBooleanArray(
                ARG_ITEM_OFFERS,
                itemOffers
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
                R.layout.bottom_sheet_sale_details,
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
        displaySaleDetails();
        initialiseActions();
    }

    private void initialiseViews(
            View view
    ) {
        textDetailsReference =
                view.findViewById(
                        R.id.textDetailsReference
                );

        textDetailsDate =
                view.findViewById(
                        R.id.textDetailsDate
                );

        textDetailsUnits =
                view.findViewById(
                        R.id.textDetailsUnits
                );

        textDetailsRevenue =
                view.findViewById(
                        R.id.textDetailsRevenue
                );

        textDetailsCost =
                view.findViewById(
                        R.id.textDetailsCost
                );

        textDetailsSaving =
                view.findViewById(
                        R.id.textDetailsSaving
                );

        textDetailsProfit =
                view.findViewById(
                        R.id.textDetailsProfit
                );

        rowDetailsSaving =
                view.findViewById(
                        R.id.rowDetailsSaving
                );

        saleItemsContainer =
                view.findViewById(
                        R.id.saleItemsContainer
                );

        buttonCloseSaleDetails =
                view.findViewById(
                        R.id.buttonCloseSaleDetails
                );
    }

    private void displaySaleDetails() {
        Bundle arguments =
                getArguments();

        if (arguments == null) {
            displayUnavailableSale();
            return;
        }

        String saleId =
                arguments.getString(
                        ARG_SALE_ID
                );

        long timestamp =
                arguments.getLong(
                        ARG_TIMESTAMP,
                        0
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

        double totalCost =
                arguments.getDouble(
                        ARG_TOTAL_COST,
                        0
                );

        double totalProfit =
                arguments.getDouble(
                        ARG_TOTAL_PROFIT,
                        0
                );

        double totalSaving =
                arguments.getDouble(
                        ARG_TOTAL_SAVING,
                        0
                );

        textDetailsReference.setText(
                createFullReference(
                        saleId
                )
        );

        textDetailsDate.setText(
                formatDate(timestamp)
        );

        textDetailsUnits.setText(
                String.valueOf(
                        Math.max(
                                totalUnits,
                                0
                        )
                )
        );

        textDetailsRevenue.setText(
                formatMoney(
                        totalAmount
                )
        );

        textDetailsCost.setText(
                formatMoney(
                        totalCost
                )
        );

        textDetailsProfit.setText(
                formatMoney(
                        totalProfit
                )
        );

        int profitColor =
                totalProfit >= 0
                        ? R.color.primaryGreen
                        : R.color.error;

        textDetailsProfit.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        profitColor
                )
        );

        if (totalSaving > 0) {
            rowDetailsSaving.setVisibility(
                    View.VISIBLE
            );

            textDetailsSaving.setText(
                    formatMoney(
                            totalSaving
                    )
            );

        } else {
            rowDetailsSaving.setVisibility(
                    View.GONE
            );

            textDetailsSaving.setText("");
        }

        ArrayList<String> itemNames =
                arguments.getStringArrayList(
                        ARG_ITEM_NAMES
                );

        ArrayList<Integer> itemQuantities =
                arguments.getIntegerArrayList(
                        ARG_ITEM_QUANTITIES
                );

        double[] itemSubtotals =
                arguments.getDoubleArray(
                        ARG_ITEM_SUBTOTALS
                );

        double[] itemSavings =
                arguments.getDoubleArray(
                        ARG_ITEM_SAVINGS
                );

        boolean[] itemOffers =
                arguments.getBooleanArray(
                        ARG_ITEM_OFFERS
                );

        populateProductRows(
                itemNames,
                itemQuantities,
                itemSubtotals,
                itemSavings,
                itemOffers
        );
    }

    private void displayUnavailableSale() {
        textDetailsReference.setText(
                "Sale reference unavailable"
        );

        textDetailsDate.setText(
                "Date unavailable"
        );

        textDetailsUnits.setText("0");
        textDetailsRevenue.setText(
                formatMoney(0)
        );

        textDetailsCost.setText(
                formatMoney(0)
        );

        textDetailsProfit.setText(
                formatMoney(0)
        );

        rowDetailsSaving.setVisibility(
                View.GONE
        );

        addEmptyProductsMessage();
    }

    private void populateProductRows(
            @Nullable ArrayList<String> itemNames,
            @Nullable ArrayList<Integer> itemQuantities,
            @Nullable double[] itemSubtotals,
            @Nullable double[] itemSavings,
            @Nullable boolean[] itemOffers
    ) {
        saleItemsContainer.removeAllViews();

        if (itemNames == null
                || itemQuantities == null
                || itemSubtotals == null
                || itemNames.isEmpty()) {

            addEmptyProductsMessage();
            return;
        }

        int itemCount =
                Math.min(
                        itemNames.size(),
                        Math.min(
                                itemQuantities.size(),
                                itemSubtotals.length
                        )
                );

        if (itemCount <= 0) {
            addEmptyProductsMessage();
            return;
        }

        for (int index = 0;
             index < itemCount;
             index++) {

            String productName =
                    itemNames.get(index);

            Integer quantityValue =
                    itemQuantities.get(index);

            int quantity =
                    quantityValue == null
                            ? 0
                            : Math.max(
                            quantityValue,
                            0
                    );

            double subtotal =
                    itemSubtotals[index];

            double saving =
                    itemSavings != null
                            && index < itemSavings.length
                            ? itemSavings[index]
                            : 0;

            boolean offerApplied =
                    itemOffers != null
                            && index < itemOffers.length
                            && itemOffers[index];

            addProductRow(
                    productName,
                    quantity,
                    subtotal,
                    saving,
                    offerApplied
            );

            if (index
                    < itemCount - 1) {

                addDivider();
            }
        }
    }

    private void addProductRow(
            String productName,
            int quantity,
            double subtotal,
            double saving,
            boolean offerApplied
    ) {
        LinearLayout row =
                new LinearLayout(
                        requireContext()
                );

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        row.setPadding(
                0,
                dp(14),
                0,
                dp(14)
        );

        LinearLayout headingRow =
                new LinearLayout(
                        requireContext()
                );

        headingRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        headingRow.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );

        TextView nameView =
                new TextView(
                        requireContext()
                );

        nameView.setText(
                getSafeText(
                        productName,
                        "Product"
                )
        );

        nameView.setTextSize(15);
        nameView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        nameView.setTextColor(
                MaterialColors.getColor(
                        saleItemsContainer,
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

        headingRow.addView(
                nameView,
                nameParameters
        );

        TextView subtotalView =
                new TextView(
                        requireContext()
                );

        subtotalView.setText(
                formatMoney(
                        subtotal
                )
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
                dp(12)
        );

        headingRow.addView(
                subtotalView,
                subtotalParameters
        );

        row.addView(
                headingRow
        );

        TextView quantityView =
                new TextView(
                        requireContext()
                );

        String quantityText =
                quantity == 1
                        ? "1 unit"
                        : quantity + " units";

        if (offerApplied) {
            quantityText +=
                    " \u2022 Quantity offer applied";
        }

        quantityView.setText(
                quantityText
        );

        quantityView.setTextSize(13);

        quantityView.setTextColor(
                MaterialColors.getColor(
                        saleItemsContainer,
                        com.google.android.material.R.attr
                                .colorOnSurfaceVariant
                )
        );

        LinearLayout.LayoutParams quantityParameters =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        quantityParameters.topMargin =
                dp(4);

        row.addView(
                quantityView,
                quantityParameters
        );

        if (saving > 0) {
            TextView savingView =
                    new TextView(
                            requireContext()
                    );

            savingView.setText(
                    "Customer saved "
                            + formatMoney(
                            saving
                    )
            );

            savingView.setTextSize(12);
            savingView.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            savingView.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.accentOrange
                    )
            );

            LinearLayout.LayoutParams savingParameters =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            savingParameters.topMargin =
                    dp(4);

            row.addView(
                    savingView,
                    savingParameters
            );
        }

        saleItemsContainer.addView(
                row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void addDivider() {
        View divider =
                new View(
                        requireContext()
                );

        divider.setBackgroundColor(
                MaterialColors.getColor(
                        saleItemsContainer,
                        com.google.android.material.R.attr
                                .colorOutlineVariant
                )
        );

        saleItemsContainer.addView(
                divider,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                )
        );
    }

    private void addEmptyProductsMessage() {
        saleItemsContainer.removeAllViews();

        TextView emptyView =
                new TextView(
                        requireContext()
                );

        emptyView.setText(
                "No product details are available for this sale."
        );

        emptyView.setGravity(
                android.view.Gravity.CENTER
        );

        emptyView.setPadding(
                dp(16),
                dp(24),
                dp(16),
                dp(24)
        );

        emptyView.setTextSize(14);

        emptyView.setTextColor(
                MaterialColors.getColor(
                        saleItemsContainer,
                        com.google.android.material.R.attr
                                .colorOnSurfaceVariant
                )
        );

        saleItemsContainer.addView(
                emptyView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void initialiseActions() {
        buttonCloseSaleDetails
                .setOnClickListener(
                        view -> dismiss()
                );
    }

    private String createFullReference(
            String saleId
    ) {
        if (saleId == null
                || saleId.trim().isEmpty()) {

            return "Sale reference unavailable";
        }

        return "Reference: "
                + saleId.trim();
    }

    private String formatDate(
            long timestamp
    ) {
        if (timestamp <= 0) {
            return "Date unavailable";
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE, d MMMM yyyy 'at' h:mm a",
                        KENYAN_LOCALE
                );

        return dateFormat.format(
                new Date(timestamp)
        );
    }

    private String formatMoney(
            double amount
    ) {
        return String.format(
                KENYAN_LOCALE,
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

    private static String getSafeProductName(
            SaleItem item
    ) {
        if (item == null
                || item.getProductName() == null
                || item.getProductName()
                .trim()
                .isEmpty()) {

            return "Product";
        }

        return item.getProductName()
                .trim();
    }

    private String getSafeText(
            String value,
            String fallback
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog =
                getDialog();

        if (!(dialog
                instanceof BottomSheetDialog)) {

            return;
        }

        BottomSheetDialog bottomSheetDialog =
                (BottomSheetDialog) dialog;

        View bottomSheet =
                bottomSheetDialog.findViewById(
                        com.google.android.material.R.id
                                .design_bottom_sheet
                );

        if (bottomSheet == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParameters =
                bottomSheet.getLayoutParams();

        layoutParameters.height =
                ViewGroup.LayoutParams.MATCH_PARENT;

        bottomSheet.setLayoutParams(
                layoutParameters
        );

        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(
                        bottomSheet
                );

        behavior.setState(
                BottomSheetBehavior.STATE_EXPANDED
        );

        behavior.setSkipCollapsed(
                true
        );
    }

    @Override
    public void onDestroyView() {
        textDetailsReference = null;
        textDetailsDate = null;
        textDetailsUnits = null;
        textDetailsRevenue = null;
        textDetailsCost = null;
        textDetailsSaving = null;
        textDetailsProfit = null;

        rowDetailsSaving = null;
        saleItemsContainer = null;
        buttonCloseSaleDetails = null;

        super.onDestroyView();
    }
}