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

import com.example.quickstock.R;
import com.example.quickstock.activities.AddProductActivity;
import com.example.quickstock.activities.SalesHistoryActivity;
import com.example.quickstock.models.Product;
import com.example.quickstock.models.Sale;
import com.example.quickstock.models.SaleItem;
import com.example.quickstock.repositories.ProductRepository;
import com.example.quickstock.repositories.SaleRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private View dashboardRoot;

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

    private boolean dashboardLoading = false;

    private final NumberFormat moneyFormat =
            NumberFormat.getNumberInstance(
                    new Locale("en", "KE")
            );

    public DashboardFragment() {
        // Required empty constructor.
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
        super.onViewCreated(view, savedInstanceState);

        initialiseViews(view);

        productRepository = new ProductRepository();
        saleRepository = new SaleRepository();

        setupQuickActions();
    }

    private void initialiseViews(View view) {
        dashboardRoot =
                view.findViewById(R.id.dashboardRoot);

        txtTopProduct =
                view.findViewById(R.id.txtTopProduct);

        txtTopProductProfit =
                view.findViewById(R.id.txtTopProductProfit);

        txtSales =
                view.findViewById(R.id.txtSales);

        txtLowStock =
                view.findViewById(R.id.txtLowStock);

        txtProfit =
                view.findViewById(R.id.txtProfit);

        buttonAddSale =
                view.findViewById(R.id.buttonAddSale);

        buttonAddProduct =
                view.findViewById(R.id.buttonAddProduct);

        buttonViewInventory =
                view.findViewById(R.id.buttonViewInventory);

        buttonSalesHistory =
                view.findViewById(R.id.buttonSalesHistory);
    }

    private void setupQuickActions() {
        buttonAddSale.setOnClickListener(
                view -> navigateToTab(R.id.nav_sales)
        );

        buttonAddProduct.setOnClickListener(
                view -> {
                    Intent intent = new Intent(
                            requireContext(),
                            AddProductActivity.class
                    );

                    startActivity(intent);
                }
        );

        buttonViewInventory.setOnClickListener(
                view -> navigateToTab(R.id.nav_inventory)
        );

        buttonSalesHistory.setOnClickListener(
                view -> {
                    Intent intent = new Intent(
                            requireContext(),
                            SalesHistoryActivity.class
                    );

                    startActivity(intent);
                }
        );
    }

    private void loadDashboard() {
        if (dashboardLoading) {
            return;
        }

        if (productRepository == null
                || saleRepository == null) {
            return;
        }

        dashboardLoading = true;

        loadInventorySummary();
        loadSalesSummary();
    }

    private void loadInventorySummary() {
        productRepository.getProducts(
                new ProductRepository.OnProductsLoadedListener() {

                    @Override
                    public void onProductsLoaded(
                            List<Product> products
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        int lowStockCount = 0;

                        if (products != null) {
                            for (Product product : products) {
                                if (product != null
                                        && product.isLowStock()) {
                                    lowStockCount++;
                                }
                            }
                        }

                        txtLowStock.setText(
                                String.valueOf(lowStockCount)
                        );
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        txtLowStock.setText("0");

                        showMessage(
                                "Unable to load inventory: "
                                        + getErrorMessage(error)
                        );
                    }
                }
        );
    }

    private void loadSalesSummary() {
        saleRepository.getSales(
                new SaleRepository.OnSalesLoadedListener() {

                    @Override
                    public void onSalesLoaded(
                            List<Sale> sales
                    ) {
                        dashboardLoading = false;

                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        calculateTodayMetrics(sales);
                    }

                    @Override
                    public void onFailure(String error) {
                        dashboardLoading = false;

                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        resetSalesValues();

                        showMessage(
                                "Unable to load sales: "
                                        + getErrorMessage(error)
                        );
                    }
                }
        );
    }

    private void calculateTodayMetrics(
            List<Sale> sales
    ) {
        long todayStart = getStartOfToday();
        long tomorrowStart = getStartOfTomorrow();

        double todayRevenue = 0;
        double todayProfit = 0;

        Map<String, Double> profitByProduct =
                new HashMap<>();

        Map<String, String> productNames =
                new HashMap<>();

        if (sales != null) {
            for (Sale sale : sales) {
                if (sale == null) {
                    continue;
                }

                long timestamp = sale.getTimestamp();

                if (timestamp < todayStart
                        || timestamp >= tomorrowStart) {
                    continue;
                }

                todayRevenue += sale.getTotalAmount();
                todayProfit += sale.getTotalProfit();

                List<SaleItem> saleItems =
                        sale.getItems();

                if (saleItems == null
                        || saleItems.isEmpty()) {
                    continue;
                }

                for (SaleItem item : saleItems) {
                    if (item == null) {
                        continue;
                    }

                    String productId =
                            item.getProductId();

                    if (productId == null
                            || productId.trim().isEmpty()) {
                        continue;
                    }

                    double previousProfit =
                            profitByProduct.getOrDefault(
                                    productId,
                                    0.0
                            );

                    profitByProduct.put(
                            productId,
                            previousProfit + item.getProfit()
                    );

                    String productName =
                            item.getProductName();

                    if (productName != null
                            && !productName.trim().isEmpty()) {
                        productNames.put(
                                productId,
                                productName.trim()
                        );
                    }
                }
            }
        }

        txtSales.setText(
                formatMoney(todayRevenue)
        );

        txtProfit.setText(
                formatMoney(todayProfit)
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

            String productId = entry.getKey();
            Double productProfit = entry.getValue();

            if (productId == null
                    || productProfit == null) {
                continue;
            }

            if (topProductId == null
                    || productProfit > highestProfit) {
                topProductId = productId;
                highestProfit = productProfit;
            }
        }

        if (topProductId == null
                || highestProfit <= 0) {

            txtTopProduct.setText(
                    "No sales today"
            );

            txtTopProductProfit.setText(
                    formatMoney(0) + " profit"
            );

            return;
        }

        String productName =
                productNames.get(topProductId);

        if (productName == null
                || productName.trim().isEmpty()) {
            productName = "Unknown product";
        }

        txtTopProduct.setText(productName);

        txtTopProductProfit.setText(
                formatMoney(highestProfit)
                        + " profit"
        );
    }

    private void resetSalesValues() {
        txtSales.setText(formatMoney(0));
        txtProfit.setText(formatMoney(0));

        txtTopProduct.setText(
                "No sales today"
        );

        txtTopProductProfit.setText(
                formatMoney(0) + " profit"
        );
    }

    private String formatMoney(double amount) {
        return "KSh "
                + moneyFormat.format(amount);
    }

    private long getStartOfToday() {
        Calendar calendar =
                Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
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

        return calendar.getTimeInMillis();
    }

    private void navigateToTab(int menuItemId) {
        if (getActivity() == null) {
            return;
        }

        BottomNavigationView bottomNavigation =
                getActivity().findViewById(
                        R.id.bottomNavigation
                );

        if (bottomNavigation == null) {
            showMessage(
                    "Navigation could not be opened."
            );

            return;
        }

        bottomNavigation.setSelectedItemId(
                menuItemId
        );
    }

    private void showMessage(String message) {
        if (!isAdded()
                || dashboardRoot == null) {
            return;
        }

        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar.make(
                dashboardRoot,
                safeMessage,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private String getErrorMessage(String error) {
        if (error == null
                || error.trim().isEmpty()) {
            return "Unknown error.";
        }

        return error.trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
    }

    @Override
    public void onDestroyView() {
        dashboardLoading = false;

        dashboardRoot = null;

        txtTopProduct = null;
        txtTopProductProfit = null;
        txtSales = null;
        txtLowStock = null;
        txtProfit = null;

        buttonAddSale = null;
        buttonAddProduct = null;
        buttonViewInventory = null;
        buttonSalesHistory = null;

        super.onDestroyView();
    }
}