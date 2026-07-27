package com.example.quickstock.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.adapters.SalesProductAdapter;
import com.example.quickstock.models.Product;
import com.example.quickstock.models.SaleItem;
import com.example.quickstock.repositories.ProductRepository;
import com.example.quickstock.repositories.SaleRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesFragment extends Fragment {

    private RecyclerView recyclerSalesProducts;
    private SearchView searchProducts;

    private CircularProgressIndicator progressProducts;

    private TextView textEmptyProducts;
    private TextView textSelectedItems;
    private TextView textSaleTotal;
    private TextView textCustomerSaving;

    private MaterialButton buttonClearSale;
    private MaterialButton buttonCompleteSale;

    private SalesProductAdapter salesProductAdapter;

    private ProductRepository productRepository;
    private SaleRepository saleRepository;

    /*
     * Product ID -> offer-aware SaleItem.
     *
     * LinkedHashMap preserves the order in which products
     * were selected.
     */
    private final Map<String, SaleItem> cartItems =
            new LinkedHashMap<>();

    private boolean saleInProgress = false;

    public SalesFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_sales,
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
        initialiseRepositories();
        initialiseProductList();
        initialiseButtons();

        updateSaleSummary();
        loadProducts();
    }

    private void initialiseViews(
            View view
    ) {

        recyclerSalesProducts =
                view.findViewById(
                        R.id.recyclerSalesProducts
                );

        searchProducts =
                view.findViewById(
                        R.id.searchProducts
                );

        progressProducts =
                view.findViewById(
                        R.id.progressProducts
                );

        textEmptyProducts =
                view.findViewById(
                        R.id.textEmptyProducts
                );

        textSelectedItems =
                view.findViewById(
                        R.id.textSelectedItems
                );

        textSaleTotal =
                view.findViewById(
                        R.id.textSaleTotal
                );

        textCustomerSaving =
                view.findViewById(
                        R.id.textCustomerSaving
                );

        buttonClearSale =
                view.findViewById(
                        R.id.buttonClearSale
                );

        buttonCompleteSale =
                view.findViewById(
                        R.id.buttonCompleteSale
                );
    }

    private void initialiseRepositories() {

        productRepository =
                new ProductRepository();

        saleRepository =
                new SaleRepository();
    }

    private void initialiseProductList() {

        salesProductAdapter =
                new SalesProductAdapter(
                        new SalesProductAdapter
                                .OnQuantityChangedListener() {

                            @Override
                            public void onQuantityChanged(
                                    Product product,
                                    int quantity
                            ) {

                                updateCartItem(
                                        product,
                                        quantity
                                );
                            }

                            @Override
                            public void onQuantityClicked(
                                    Product product,
                                    int currentQuantity
                            ) {

                                showQuantityDialog(
                                        product,
                                        currentQuantity
                                );
                            }
                        }
                );

        recyclerSalesProducts.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        recyclerSalesProducts.setHasFixedSize(
                false
        );

        recyclerSalesProducts.setAdapter(
                salesProductAdapter
        );

        searchProducts.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {

                        salesProductAdapter.filter(
                                query
                        );

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {

                        salesProductAdapter.filter(
                                newText
                        );

                        return true;
                    }
                }
        );
    }

    private void initialiseButtons() {

        buttonClearSale.setOnClickListener(
                view -> clearSale()
        );

        buttonCompleteSale.setOnClickListener(
                view -> completeSale()
        );
    }

    private void loadProducts() {

        showLoading(true);

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

                        showLoading(false);

                        List<Product> safeProducts =
                                products == null
                                        ? new ArrayList<>()
                                        : products;

                        salesProductAdapter.setProducts(
                                safeProducts
                        );

                        textEmptyProducts.setVisibility(
                                safeProducts.isEmpty()
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        /*
                         * Refresh quantities, stock, prices and offers
                         * using the latest product values from Firebase.
                         */
                        synchroniseCartWithProducts(
                                safeProducts
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        showLoading(false);

                        textEmptyProducts.setVisibility(
                                View.VISIBLE
                        );

                        showMessage(error);
                    }
                }
        );
    }

    /**
     * Creates or updates an offer-aware SaleItem whenever
     * the selected quantity changes.
     */
    private void updateCartItem(
            Product product,
            int quantity
    ) {

        if (product == null
                || product.getId() == null
                || product.getId()
                .trim()
                .isEmpty()) {

            showMessage(
                    "This product has no valid ID."
            );

            return;
        }

        String productId =
                product.getId();

        int safeQuantity =
                Math.max(
                        0,
                        Math.min(
                                quantity,
                                product.getStock()
                        )
                );

        if (safeQuantity <= 0) {

            cartItems.remove(
                    productId
            );

        } else {

            /*
             * Important:
             * This factory stores the selling price, cost price,
             * offer quantity and offer price, then calculates the
             * correct offer-aware subtotal.
             */
            SaleItem saleItem;

            try {

                saleItem =
                        SaleItem.fromProduct(
                                product,
                                safeQuantity
                        );

            } catch (IllegalArgumentException exception) {

                showMessage(
                        exception.getMessage()
                );

                return;
            }

            cartItems.put(
                    productId,
                    saleItem
            );
        }

        updateSaleSummary();
    }

    private void showQuantityDialog(
            Product product,
            int currentQuantity
    ) {

        if (product == null) {
            return;
        }

        if (product.getStock() <= 0) {

            showMessage(
                    getSafeProductName(product)
                            + " is out of stock."
            );

            return;
        }

        EditText quantityInput =
                new EditText(
                        requireContext()
                );

        quantityInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        quantityInput.setSingleLine(true);
        quantityInput.setSelectAllOnFocus(true);

        quantityInput.setText(
                String.valueOf(
                        currentQuantity
                )
        );

        int horizontalPadding =
                Math.round(
                        24
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        int verticalPadding =
                Math.round(
                        8
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        quantityInput.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding
        );

        String dialogMessage =
                buildQuantityDialogMessage(
                        product
                );

        AlertDialog dialog =
                new AlertDialog.Builder(
                        requireContext()
                )
                        .setTitle(
                                getSafeProductName(
                                        product
                                )
                        )
                        .setMessage(
                                dialogMessage
                        )
                        .setView(
                                quantityInput
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Apply",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                unused -> {

                    dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            )
                            .setOnClickListener(
                                    view -> {

                                        String enteredValue =
                                                quantityInput
                                                        .getText()
                                                        .toString()
                                                        .trim();

                                        if (enteredValue.isEmpty()) {

                                            quantityInput.setError(
                                                    "Enter a quantity."
                                            );

                                            return;
                                        }

                                        int quantity;

                                        try {

                                            quantity =
                                                    Integer.parseInt(
                                                            enteredValue
                                                    );

                                        } catch (
                                                NumberFormatException exception
                                        ) {

                                            quantityInput.setError(
                                                    "Enter a valid whole number."
                                            );

                                            return;
                                        }

                                        if (quantity < 0) {

                                            quantityInput.setError(
                                                    "Quantity cannot be negative."
                                            );

                                            return;
                                        }

                                        if (quantity
                                                > product.getStock()) {

                                            quantityInput.setError(
                                                    "Only "
                                                            + product.getStock()
                                                            + " units are available."
                                            );

                                            return;
                                        }

                                        salesProductAdapter
                                                .setQuantity(
                                                        product.getId(),
                                                        quantity
                                                );

                                        updateCartItem(
                                                product,
                                                quantity
                                        );

                                        dialog.dismiss();
                                    }
                            );

                    quantityInput.requestFocus();
                }
        );

        dialog.show();
    }

    private String buildQuantityDialogMessage(
            Product product
    ) {

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Available stock: "
        );

        message.append(
                product.getStock()
        );

        message.append(
                "\nNormal price: "
        );

        message.append(
                formatMoney(
                        product.getSellingPrice()
                )
        );

        if (product.hasValidQuantityOffer()) {

            message.append(
                    "\nOffer: "
            );

            message.append(
                    product.getOfferQuantity()
            );

            message.append(
                    " for "
            );

            message.append(
                    formatMoney(
                            product.getOfferPrice()
                    )
            );
        }

        return message.toString();
    }

    private void completeSale() {

        if (saleInProgress) {
            return;
        }

        if (cartItems.isEmpty()) {

            showMessage(
                    "Select at least one product."
            );

            return;
        }

        List<SaleItem> saleItems =
                new ArrayList<>();

        for (SaleItem item
                : cartItems.values()) {

            if (item == null
                    || item.getQuantity() <= 0) {

                continue;
            }

            /*
             * Make sure the sale snapshot is current before
             * sending it to the repository.
             */
            item.recalculate();

            saleItems.add(item);
        }

        if (saleItems.isEmpty()) {

            showMessage(
                    "Select at least one valid product."
            );

            return;
        }

        setSaleInProgress(true);

        saleRepository.completeSale(
                saleItems,
                new SaleRepository
                        .OnSaleCompleteListener() {

                    @Override
                    public void onSuccess(
                            String saleId
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        setSaleInProgress(false);

                        clearSale();

                        showMessage(
                                "Sale completed successfully."
                        );

                        /*
                         * Reload to display the reduced Firebase stock.
                         */
                        loadProducts();
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        setSaleInProgress(false);
                        showMessage(error);
                    }
                }
        );
    }

    private void clearSale() {

        cartItems.clear();

        if (salesProductAdapter != null) {

            salesProductAdapter
                    .clearQuantities();
        }

        updateSaleSummary();
    }

    /**
     * Calculates the live, offer-aware sale summary.
     */
    private void updateSaleSummary() {

        int totalQuantity = 0;

        double totalAmount = 0;
        double totalSaving = 0;

        for (SaleItem item
                : cartItems.values()) {

            if (item == null
                    || item.getQuantity() <= 0) {

                continue;
            }

            item.recalculate();

            totalQuantity +=
                    item.getQuantity();

            totalAmount +=
                    item.getSubtotal();

            totalSaving +=
                    item.getCustomerSaving();
        }

        textSelectedItems.setText(
                String.valueOf(
                        totalQuantity
                )
        );

        textSaleTotal.setText(
                formatMoney(
                        totalAmount
                )
        );

        if (totalSaving > 0) {

            textCustomerSaving.setVisibility(
                    View.VISIBLE
            );

            textCustomerSaving.setText(
                    String.format(
                            Locale.getDefault(),
                            "Customer saves %s",
                            formatMoney(totalSaving)
                    )
            );

        } else {

            textCustomerSaving.setVisibility(
                    View.GONE
            );

            textCustomerSaving.setText(
                    ""
            );
        }

        boolean hasItems =
                totalQuantity > 0;

        buttonClearSale.setEnabled(
                hasItems && !saleInProgress
        );

        buttonCompleteSale.setEnabled(
                hasItems && !saleInProgress
        );
    }

    /**
     * Refreshes cart entries using the latest stock and
     * pricing information loaded from Firebase.
     */
    private void synchroniseCartWithProducts(
            List<Product> products
    ) {

        if (products == null) {
            return;
        }

        Map<String, Product> productMap =
                new LinkedHashMap<>();

        for (Product product : products) {

            if (product != null
                    && product.getId() != null
                    && !product.getId()
                    .trim()
                    .isEmpty()) {

                productMap.put(
                        product.getId(),
                        product
                );
            }
        }

        List<String> selectedProductIds =
                new ArrayList<>(
                        cartItems.keySet()
                );

        for (String productId
                : selectedProductIds) {

            Product latestProduct =
                    productMap.get(
                            productId
                    );

            SaleItem currentItem =
                    cartItems.get(
                            productId
                    );

            if (latestProduct == null
                    || currentItem == null
                    || latestProduct.getStock() <= 0) {

                cartItems.remove(
                        productId
                );

                salesProductAdapter.setQuantity(
                        productId,
                        0
                );

                continue;
            }

            int correctedQuantity =
                    Math.min(
                            currentItem.getQuantity(),
                            latestProduct.getStock()
                    );

            if (correctedQuantity <= 0) {

                cartItems.remove(
                        productId
                );

                salesProductAdapter.setQuantity(
                        productId,
                        0
                );

                continue;
            }

            /*
             * Recreate the item even when the quantity is unchanged.
             * This updates selling price, cost price and offer details
             * if they were edited in Firebase.
             */
            SaleItem refreshedItem =
                    SaleItem.fromProduct(
                            latestProduct,
                            correctedQuantity
                    );

            cartItems.put(
                    productId,
                    refreshedItem
            );

            salesProductAdapter.setQuantity(
                    productId,
                    correctedQuantity
            );
        }

        updateSaleSummary();
    }

    private void setSaleInProgress(
            boolean inProgress
    ) {

        saleInProgress =
                inProgress;

        buttonCompleteSale.setText(
                inProgress
                        ? "Completing..."
                        : getString(
                        R.string.complete_sale
                )
        );

        searchProducts.setEnabled(
                !inProgress
        );

        recyclerSalesProducts.setEnabled(
                !inProgress
        );

        updateSaleSummary();
    }

    private void showLoading(
            boolean loading
    ) {

        progressProducts.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        recyclerSalesProducts.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );

        searchProducts.setEnabled(
                !loading && !saleInProgress
        );

        if (loading) {

            textEmptyProducts.setVisibility(
                    View.GONE
            );
        }
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

    private String getSafeProductName(
            Product product
    ) {

        if (product == null
                || product.getName() == null
                || product.getName()
                .trim()
                .isEmpty()) {

            return "Product";
        }

        return product.getName();
    }

    private void showMessage(
            String message
    ) {

        if (!isAdded()) {
            return;
        }

        if (message == null
                || message.trim().isEmpty()) {

            message =
                    "Something went wrong.";
        }

        Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}