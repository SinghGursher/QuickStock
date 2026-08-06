package com.example.quickstock.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.adapters.SalesHistoryAdapter;
import com.example.quickstock.fragments.SaleDetailsBottomSheet;
import com.example.quickstock.models.Sale;
import com.example.quickstock.models.SaleItem;
import com.example.quickstock.repositories.SaleRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SalesHistoryActivity
        extends AppCompatActivity {

    private enum HistoryPeriod {
        TODAY,
        SEVEN_DAYS,
        THIRTY_DAYS,
        ALL
    }

    private View salesHistoryRoot;

    private MaterialToolbar toolbarSalesHistory;

    private TextView textHistoryPeriodDescription;
    private TextView textHistoryRevenue;
    private TextView textHistoryProfit;
    private TextView textHistoryCount;
    private TextView textHistoryResultCount;

    private TextView textEmptySalesHistoryTitle;
    private TextView textEmptySalesHistoryMessage;

    private ChipGroup chipGroupHistoryPeriod;

    private RecyclerView recyclerSalesHistory;
    private View layoutEmptySalesHistory;

    private CircularProgressIndicator
            progressSalesHistory;

    private SalesHistoryAdapter
            salesHistoryAdapter;

    private SaleRepository saleRepository;

    /*
     * Contains every valid sale loaded from Firebase,
     * sorted from newest to oldest.
     */
    private final List<Sale> allSales =
            new ArrayList<>();

    private HistoryPeriod currentPeriod =
            HistoryPeriod.ALL;

    private boolean loading;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_sales_history
        );

        initialiseViews();
        initialiseToolbar();
        initialiseRecyclerView();
        initialiseFilters();

        saleRepository =
                new SaleRepository();

        loadSales();
    }

    private void initialiseViews() {
        salesHistoryRoot =
                findViewById(
                        R.id.salesHistoryRoot
                );

        toolbarSalesHistory =
                findViewById(
                        R.id.toolbarSalesHistory
                );

        textHistoryPeriodDescription =
                findViewById(
                        R.id.textHistoryPeriodDescription
                );

        textHistoryRevenue =
                findViewById(
                        R.id.textHistoryRevenue
                );

        textHistoryProfit =
                findViewById(
                        R.id.textHistoryProfit
                );

        textHistoryCount =
                findViewById(
                        R.id.textHistoryCount
                );

        textHistoryResultCount =
                findViewById(
                        R.id.textHistoryResultCount
                );

        textEmptySalesHistoryTitle =
                findViewById(
                        R.id.textEmptySalesHistoryTitle
                );

        textEmptySalesHistoryMessage =
                findViewById(
                        R.id.textEmptySalesHistoryMessage
                );

        chipGroupHistoryPeriod =
                findViewById(
                        R.id.chipGroupHistoryPeriod
                );

        recyclerSalesHistory =
                findViewById(
                        R.id.recyclerSalesHistory
                );

        layoutEmptySalesHistory =
                findViewById(
                        R.id.layoutEmptySalesHistory
                );

        progressSalesHistory =
                findViewById(
                        R.id.progressSalesHistory
                );
    }

    private void initialiseToolbar() {
        setSupportActionBar(
                toolbarSalesHistory
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setTitle(
                            "Sales History"
                    );

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(
                            true
                    );
        }

        toolbarSalesHistory
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );
    }

    private void initialiseRecyclerView() {
        salesHistoryAdapter =
                new SalesHistoryAdapter(
                        this::openSaleDetails
                );

        recyclerSalesHistory.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recyclerSalesHistory.setHasFixedSize(
                true
        );

        recyclerSalesHistory.setAdapter(
                salesHistoryAdapter
        );
    }

    @SuppressWarnings("deprecation")
    private void initialiseFilters() {
        /*
         * The older listener remains compatible across a
         * wider range of Material library versions.
         */
        chipGroupHistoryPeriod
                .setOnCheckedChangeListener(
                        (group, checkedId) -> {
                            if (checkedId
                                    == R.id.chipToday) {

                                currentPeriod =
                                        HistoryPeriod.TODAY;

                            } else if (checkedId
                                    == R.id.chipSevenDays) {

                                currentPeriod =
                                        HistoryPeriod.SEVEN_DAYS;

                            } else if (checkedId
                                    == R.id.chipThirtyDays) {

                                currentPeriod =
                                        HistoryPeriod.THIRTY_DAYS;

                            } else {
                                currentPeriod =
                                        HistoryPeriod.ALL;
                            }

                            applyCurrentFilter();
                        }
                );
    }

    private void loadSales() {
        if (loading) {
            return;
        }

        setLoading(true);

        saleRepository.getSales(
                new SaleRepository
                        .OnSalesLoadedListener() {

                    @Override
                    public void onSalesLoaded(
                            List<Sale> sales
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        allSales.clear();

                        if (sales != null) {
                            for (Sale sale : sales) {
                                Sale normalisedSale =
                                        normaliseSale(
                                                sale
                                        );

                                if (normalisedSale != null) {
                                    allSales.add(
                                            normalisedSale
                                    );
                                }
                            }
                        }

                        /*
                         * Sales History always displays the newest
                         * transaction first.
                         */
                        allSales.sort(
                                Comparator.comparingLong(
                                        Sale::getTimestamp
                                ).reversed()
                        );

                        setLoading(false);
                        applyCurrentFilter();
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        allSales.clear();

                        setLoading(false);
                        applyCurrentFilter();

                        showLoadError(
                                getErrorMessage(
                                        error,
                                        "Sales history could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private Sale normaliseSale(
            Sale sale
    ) {
        if (sale == null) {
            return null;
        }

        List<SaleItem> validItems =
                new ArrayList<>();

        List<SaleItem> items =
                sale.getItems();

        if (items != null) {
            for (SaleItem item : items) {
                if (item == null
                        || item.getQuantity() <= 0) {

                    continue;
                }

                /*
                 * Recreate totals from the pricing snapshot
                 * saved with the original transaction.
                 */
                item.recalculate();
                validItems.add(item);
            }
        }

        sale.setItems(validItems);
        sale.recalculateTotals();

        return sale;
    }

    private void applyCurrentFilter() {
        if (salesHistoryAdapter == null) {
            return;
        }

        List<Sale> filteredSales =
                new ArrayList<>();

        long periodStart =
                getPeriodStart(
                        currentPeriod
                );

        for (Sale sale : allSales) {
            if (sale == null) {
                continue;
            }

            if (currentPeriod
                    == HistoryPeriod.ALL
                    || sale.getTimestamp()
                    >= periodStart) {

                filteredSales.add(
                        sale
                );
            }
        }

        salesHistoryAdapter.submitList(
                new ArrayList<>(
                        filteredSales
                )
        );

        updatePeriodDescription();
        updateSummary(filteredSales);
        updateResultCount(
                filteredSales.size()
        );

        updateEmptyState(
                filteredSales.isEmpty()
        );
    }

    private long getPeriodStart(
            HistoryPeriod period
    ) {
        if (period == HistoryPeriod.ALL) {
            return Long.MIN_VALUE;
        }

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        if (period
                == HistoryPeriod.SEVEN_DAYS) {

            /*
             * Today plus the previous six calendar days.
             */
            calendar.add(
                    Calendar.DAY_OF_YEAR,
                    -6
            );

        } else if (period
                == HistoryPeriod.THIRTY_DAYS) {

            /*
             * Today plus the previous twenty-nine
             * calendar days.
             */
            calendar.add(
                    Calendar.DAY_OF_YEAR,
                    -29
            );
        }

        return calendar.getTimeInMillis();
    }

    private void updatePeriodDescription() {
        String description;

        switch (currentPeriod) {
            case TODAY:
                description =
                        "Sales completed today";
                break;

            case SEVEN_DAYS:
                description =
                        "Sales completed during the last 7 days";
                break;

            case THIRTY_DAYS:
                description =
                        "Sales completed during the last 30 days";
                break;

            case ALL:
            default:
                description =
                        "All completed sales";
                break;
        }

        textHistoryPeriodDescription.setText(
                description
        );
    }

    private void updateSummary(
            List<Sale> filteredSales
    ) {
        double totalRevenue = 0;
        double totalProfit = 0;

        if (filteredSales != null) {
            for (Sale sale : filteredSales) {
                if (sale == null) {
                    continue;
                }

                totalRevenue +=
                        sale.getTotalAmount();

                totalProfit +=
                        sale.getTotalProfit();
            }
        }

        textHistoryRevenue.setText(
                formatMoney(
                        totalRevenue
                )
        );

        textHistoryProfit.setText(
                formatMoney(
                        totalProfit
                )
        );

        int profitColor =
                totalProfit >= 0
                        ? R.color.primaryGreen
                        : R.color.error;

        textHistoryProfit.setTextColor(
                ContextCompat.getColor(
                        this,
                        profitColor
                )
        );

        int saleCount =
                filteredSales == null
                        ? 0
                        : filteredSales.size();

        textHistoryCount.setText(
                String.valueOf(
                        saleCount
                )
        );
    }

    private void updateResultCount(
            int resultCount
    ) {
        textHistoryResultCount.setText(
                String.format(
                        Locale.getDefault(),
                        resultCount == 1
                                ? "%d record"
                                : "%d records",
                        resultCount
                )
        );
    }

    private void updateEmptyState(
            boolean empty
    ) {
        recyclerSalesHistory.setVisibility(
                empty
                        ? View.GONE
                        : View.VISIBLE
        );

        layoutEmptySalesHistory.setVisibility(
                empty
                        ? View.VISIBLE
                        : View.GONE
        );

        if (!empty) {
            return;
        }

        if (allSales.isEmpty()) {
            textEmptySalesHistoryTitle.setText(
                    "No completed sales"
            );

            textEmptySalesHistoryMessage.setText(
                    "Complete a sale and it will appear here."
            );

            return;
        }

        textEmptySalesHistoryTitle.setText(
                "No sales in this period"
        );

        textEmptySalesHistoryMessage.setText(
                "Choose another date range to view your completed sales."
        );
    }

    private void openSaleDetails(
            Sale sale
    ) {
        if (sale == null
                || getSupportFragmentManager()
                .isStateSaved()) {

            return;
        }

        if (getSupportFragmentManager()
                .findFragmentByTag(
                        SaleDetailsBottomSheet.TAG
                ) != null) {

            return;
        }

        SaleDetailsBottomSheet sheet =
                SaleDetailsBottomSheet
                        .newInstance(
                                sale
                        );

        sheet.show(
                getSupportFragmentManager(),
                SaleDetailsBottomSheet.TAG
        );
    }

    private void setLoading(
            boolean loading
    ) {
        this.loading = loading;

        progressSalesHistory.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        recyclerSalesHistory.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );

        layoutEmptySalesHistory.setVisibility(
                View.GONE
        );

        setFiltersEnabled(
                !loading
        );
    }

    private void setFiltersEnabled(
            boolean enabled
    ) {
        for (int index = 0;
             index < chipGroupHistoryPeriod
                     .getChildCount();
             index++) {

            View child =
                    chipGroupHistoryPeriod
                            .getChildAt(index);

            child.setEnabled(enabled);
        }
    }

    private void showLoadError(
            String message
    ) {
        String safeMessage =
                getErrorMessage(
                        message,
                        "Sales history could not be loaded."
                );

        Snackbar.make(
                        salesHistoryRoot,
                        safeMessage,
                        Snackbar.LENGTH_INDEFINITE
                )
                .setAction(
                        "Retry",
                        view -> loadSales()
                )
                .show();
    }

    private String formatMoney(
            double amount
    ) {
        return String.format(
                new Locale("en", "KE"),
                "KSh %,.2f",
                amount
        );
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

    @Override
    protected void onDestroy() {
        if (recyclerSalesHistory != null) {
            recyclerSalesHistory.setAdapter(
                    null
            );
        }

        super.onDestroy();
    }
}