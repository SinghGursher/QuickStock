package com.example.quickstock.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quickstock.R;
import com.example.quickstock.activities.AddProductActivity;
import com.example.quickstock.models.Product;
import com.example.quickstock.models.Sale;
import com.example.quickstock.models.SaleItem;
import com.example.quickstock.repositories.ProductRepository;
import com.example.quickstock.repositories.SaleRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private TextView txtTopProduct;
    private TextView txtTopProductProfit;
    private TextView txtSales;
    private TextView txtLowStock;
    private TextView txtProfit;

    private MaterialButton buttonAddSale;
    private MaterialButton buttonAddProduct;
    private MaterialButton buttonViewInventory;
    private MaterialButton buttonSalesHistory;

    private ProductRepository productRepository;
    private SaleRepository saleRepository;

    private final NumberFormat moneyFormat =
            NumberFormat.getNumberInstance(
                    new Locale("en", "KE")
            );

    public DashboardFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_dashboard,
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

        productRepository =
                new ProductRepository();

        saleRepository =
                new SaleRepository();

        setupQuickActions();
        loadInventorySummary();
        loadSalesSummary();
    }

    private void initialiseViews(
            View view
    ) {

        txtTopProduct =
                view.findViewById(
                        R.id.txtTopProduct
                );

        txtTopProductProfit =
                view.findViewById(
                        R.id.txtTopProductProfit
                );

        txtSales =
                view.findViewById(
                        R.id.txtSales
                );

        txtLowStock =
                view.findViewById(
                        R.id.txtLowStock
                );

        txtProfit =
                view.findViewById(
                        R.id.txtProfit
                );

        buttonAddSale =
                view.findViewById(
                        R.id.buttonAddSale
                );

        buttonAddProduct =
                view.findViewById(
                        R.id.buttonAddProduct
                );

        buttonViewInventory =
                view.findViewById(
                        R.id.buttonViewInventory
                );

        buttonSalesHistory =
                view.findViewById(
                        R.id.buttonSalesHistory
                );
    }

    private void setupQuickActions() {

        buttonAddSale.setOnClickListener(
                view -> navigateToTab(
                        R.id.nav_sales
                )
        );

        buttonAddProduct.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    requireContext(),
                                    AddProductActivity.class
                            );

                    startActivity(intent);
                }
        );

        buttonViewInventory.setOnClickListener(
                view -> navigateToTab(
                        R.id.nav_inventory
                )
        );

        /*
         * Sales History is disabled in the XML for now.
         * This listener can remain as a fallback message.
         */
        buttonSalesHistory.setOnClickListener(
                view ->
                        Toast.makeText(
                                requireContext(),
                                "Sales History is coming soon.",
                                Toast.LENGTH_SHORT
                        ).show()
        );
    }

    private void loadInventorySummary() {

        productRepository.getProducts(
                new ProductRepository
                        .OnProductsLoadedListener() {

                    @Override
                    public void onProductsLoaded(
                            List<Product> products
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        int lowStockCount = 0;

                        if (products != null) {

                            for (Product product
                                    : products) {

                                if (product != null
                                        && product.isLowStock()) {

                                    lowStockCount++;
                                }
                            }
                        }

                        txtLowStock.setText(
                                String.valueOf(
                                        lowStockCount
                                )
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        txtLowStock.setText("0");

                        Toast.makeText(
                                requireContext(),
                                "Unable to load inventory: "
                                        + getErrorMessage(error),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadSalesSummary() {

        saleRepository.getSales(
                new SaleRepository
                        .OnSalesLoadedListener() {

                    @Override
                    public void onSalesLoaded(
                            List<Sale> sales
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        calculateTodayMetrics(
                                sales
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        resetSalesValues();

                        Toast.makeText(
                                requireContext(),
                                "Unable to load sales: "
                                        + getErrorMessage(error),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void calculateTodayMetrics(
            List<Sale> sales
    ) {

        long todayStart =
                getStartOfToday();

        long tomorrowStart =
                getStartOfTomorrow();

        double todayRevenue = 0;
        double todayProfit = 0;

        /*
         * Stores the combined profit generated by
         * each product during today's sales.
         */
        Map<String, Double> profitByProduct =
                new HashMap<>();

        /*
         * Stores product names using their Firebase IDs.
         */
        Map<String, String> productNames =
                new HashMap<>();

        if (sales != null) {

            for (Sale sale : sales) {

                if (sale == null) {
                    continue;
                }

                long timestamp =
                        sale.getTimestamp();

                /*
                 * Exclude older sales without timestamps
                 * and sales outside today's date range.
                 */
                if (timestamp < todayStart
                        || timestamp >= tomorrowStart) {

                    continue;
                }

                todayRevenue +=
                        sale.getTotalAmount();

                /*
                 * Use the stored sale profit snapshot.
                 */
                todayProfit +=
                        sale.getTotalProfit();

                List<SaleItem> saleItems =
                        sale.getItems();

                if (saleItems == null
                        || saleItems.isEmpty()) {

                    continue;
                }

                /*
                 * A Sale represents a whole transaction.
                 * Product information is stored inside
                 * each SaleItem.
                 */
                for (SaleItem item : saleItems) {

                    if (item == null) {
                        continue;
                    }

                    String productId =
                            item.getProductId();

                    if (productId == null
                            || productId
                            .trim()
                            .isEmpty()) {

                        continue;
                    }

                    double itemProfit =
                            item.getProfit();

                    double previousProfit =
                            profitByProduct
                                    .getOrDefault(
                                            productId,
                                            0.0
                                    );

                    profitByProduct.put(
                            productId,
                            previousProfit
                                    + itemProfit
                    );

                    String productName =
                            item.getProductName();

                    if (productName != null
                            && !productName
                            .trim()
                            .isEmpty()) {

                        productNames.put(
                                productId,
                                productName
                        );
                    }
                }
            }
        }

        txtSales.setText(
                formatMoney(
                        todayRevenue
                )
        );

        txtProfit.setText(
                formatMoney(
                        todayProfit
                )
        );

        displayTopProfitProduct(
                profitByProduct,
                productNames
        );
    }

    private void displayTopProfitProduct(
            Map<String, Double> profitByProduct,
            Map<String, String> productNames
    ) {

        String topProductId = null;
        double highestProfit = 0;

        for (Map.Entry<String, Double> entry
                : profitByProduct.entrySet()) {

            String productId =
                    entry.getKey();

            Double productProfit =
                    entry.getValue();

            if (productId == null
                    || productProfit == null) {

                continue;
            }

            if (topProductId == null
                    || productProfit
                    > highestProfit) {

                topProductId =
                        productId;

                highestProfit =
                        productProfit;
            }
        }

        if (topProductId == null
                || highestProfit <= 0) {

            txtTopProduct.setText(
                    "No sales today"
            );

            txtTopProductProfit.setText(
                    formatMoney(0)
                            + " profit"
            );

            return;
        }

        String productName =
                productNames.get(
                        topProductId
                );

        if (productName == null
                || productName
                .trim()
                .isEmpty()) {

            productName =
                    "Unknown product";
        }

        txtTopProduct.setText(
                productName
        );

        txtTopProductProfit.setText(
                formatMoney(
                        highestProfit
                ) + " profit"
        );
    }

    private void resetSalesValues() {

        txtSales.setText(
                formatMoney(0)
        );

        txtProfit.setText(
                formatMoney(0)
        );

        txtTopProduct.setText(
                "No sales today"
        );

        txtTopProductProfit.setText(
                formatMoney(0)
                        + " profit"
        );
    }

    private String formatMoney(
            double amount
    ) {

        return "KSh "
                + moneyFormat.format(
                amount
        );
    }

    private long getStartOfToday() {

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

        return calendar
                .getTimeInMillis();
    }

    private long getStartOfTomorrow() {

        Calendar calendar =
                Calendar.getInstance();

        calendar.setTimeInMillis(
                getStartOfToday()
        );

        calendar.add(
                Calendar.DAY_OF_YEAR,
                1
        );

        return calendar
                .getTimeInMillis();
    }

    private void navigateToTab(
            int menuItemId
    ) {

        if (getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNavigation =
                getActivity().findViewById(
                        R.id.bottomNavigation
                );

        if (bottomNavigation == null) {

            Toast.makeText(
                    requireContext(),
                    "Navigation could not be opened.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        bottomNavigation.setSelectedItemId(
                menuItemId
        );
    }

    private String getErrorMessage(
            String error
    ) {

        if (error == null
                || error.trim().isEmpty()) {

            return "Unknown error.";
        }

        return error;
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * Reload after returning from Add Product
         * or after completing a sale.
         */
        if (productRepository != null
                && saleRepository != null) {

            loadInventorySummary();
            loadSalesSummary();
        }
    }
}