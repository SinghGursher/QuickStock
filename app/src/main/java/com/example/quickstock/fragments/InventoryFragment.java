package com.example.quickstock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Comparator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.activities.AddProductActivity;
import com.example.quickstock.activities.ProductDetailsActivity;
import com.example.quickstock.adapters.ProductAdapter;
import com.example.quickstock.models.Product;
import com.example.quickstock.repositories.ProductRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryFragment extends Fragment {

    private static final String TAG =
            "InventoryFragment";

    /*
     * Receives the result from the Add Product screen.
     *
     * RESULT_OK is returned only after Firebase confirms
     * that the product was added successfully.
     */
    private final ActivityResultLauncher<Intent>
            addProductLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    this::handleAddProductResult
            );

    /*
     * Receives update and delete results from the Product
     * Details screen.
     */
    private final ActivityResultLauncher<Intent>
            productDetailsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    this::handleProductDetailsResult
            );

    private RecyclerView recyclerProducts;
    private FloatingActionButton fabAddProduct;
    private TextInputEditText etSearch;

    private final ArrayList<Product> allProducts =
            new ArrayList<>();

    private final ArrayList<Product> displayedProducts =
            new ArrayList<>();

    private ProductAdapter adapter;
    private ProductRepository productRepository;

    /*
     * This remains unchanged when products are refreshed,
     * allowing the current search to be reapplied.
     */
    private String currentSearchQuery = "";

    public InventoryFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_inventory,
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

        setupRecyclerView();
        setupSearch();
        setupClickListeners();

        /*
         * Initial load only.
         *
         * Subsequent loads happen only after receiving
         * a successful activity result.
         */
        loadProducts();
    }

    private void initialiseViews(
            View view
    ) {
        recyclerProducts =
                view.findViewById(
                        R.id.recyclerProducts
                );

        fabAddProduct =
                view.findViewById(
                        R.id.fabAddProduct
                );

        etSearch =
                view.findViewById(
                        R.id.etSearch
                );
    }

    private void setupRecyclerView() {
        recyclerProducts.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        recyclerProducts.setHasFixedSize(true);

        adapter =
                new ProductAdapter(
                        displayedProducts,
                        this::openProductDetails
                );

        recyclerProducts.setAdapter(
                adapter
        );
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        // No action required.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {
                        currentSearchQuery =
                                text == null
                                        ? ""
                                        : text.toString();

                        filterProducts(
                                currentSearchQuery
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // No action required.
                    }
                }
        );
    }

    private void setupClickListeners() {
        fabAddProduct.setOnClickListener(
                view -> {
                    Intent intent =
                            new Intent(
                                    requireContext(),
                                    AddProductActivity.class
                            );

                    addProductLauncher.launch(
                            intent
                    );
                }
        );
    }

    private void handleAddProductResult(
            ActivityResult result
    ) {
        if (result.getResultCode()
                != Activity.RESULT_OK) {
            return;
        }

        loadProducts(
                "Product added successfully."
        );
    }

    private void handleProductDetailsResult(
            ActivityResult result
    ) {
        if (result.getResultCode()
                != Activity.RESULT_OK) {
            return;
        }

        Intent resultData =
                result.getData();

        String changeType =
                resultData == null
                        ? null
                        : resultData.getStringExtra(
                        ProductDetailsActivity
                        .EXTRA_CHANGE_TYPE
                );

        String successMessage;

        if (ProductDetailsActivity
                .CHANGE_DELETED
                .equals(changeType)) {

            successMessage =
                    "Product deleted successfully.";

        } else if (ProductDetailsActivity
                .CHANGE_UPDATED
                .equals(changeType)) {

            successMessage =
                    "Product updated successfully.";

        } else {
            successMessage =
                    "Inventory updated successfully.";
        }

        loadProducts(
                successMessage
        );
    }

    private void loadProducts() {
        loadProducts(null);
    }

    private void loadProducts(
            @Nullable String successMessage
    ) {
        if (productRepository == null) {
            return;
        }

        productRepository.getProducts(
                new ProductRepository
                        .OnProductsLoadedListener() {

                    @Override
                    public void onProductsLoaded(
                            List<Product> products
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        allProducts.clear();

                        if (products != null) {
                            allProducts.addAll(
                                    products
                            );
                        }

                        allProducts.sort(
                                Comparator.comparing(
                                        product ->
                                                safeLowercase(
                                                        product == null
                                                                ? null
                                                                : product.getName()
                                                )
                                )
                        );

                        /*
                         * Reapply the existing search after receiving
                         * the refreshed Firebase data.
                         */
                        filterProducts(
                                currentSearchQuery
                        );

                        Log.d(
                                TAG,
                                "Inventory updated with "
                                        + allProducts.size()
                                        + " products."
                        );

                        /*
                         * Success is shown only after the updated
                         * inventory has been received and displayed.
                         */
                        if (successMessage != null
                                && !successMessage
                                .trim()
                                .isEmpty()) {

                            showMessage(
                                    successMessage
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        String safeError =
                                error == null
                                        || error.trim().isEmpty()
                                        ? "The inventory could not be loaded."
                                        : error.trim();

                        Log.e(
                                TAG,
                                "Failed to load products: "
                                        + safeError
                        );

                        if (successMessage != null) {
                            showMessage(
                                    successMessage
                                            + " However, the inventory could not be refreshed."
                            );

                        } else {
                            showMessage(
                                    safeError
                            );
                        }
                    }
                }
        );
    }

    private void filterProducts(
            String searchText
    ) {
        displayedProducts.clear();

        if (searchText == null
                || searchText.trim().isEmpty()) {

            displayedProducts.addAll(
                    allProducts
            );

        } else {
            String query =
                    searchText
                            .trim()
                            .toLowerCase(
                                    Locale.getDefault()
                            );

            for (Product product
                    : allProducts) {

                if (productMatchesSearch(
                        product,
                        query
                )) {
                    displayedProducts.add(
                            product
                    );
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Log.d(
                TAG,
                "Search query: "
                        + searchText
                        + ". Showing "
                        + displayedProducts.size()
                        + " products."
        );
    }

    private boolean productMatchesSearch(
            Product product,
            String query
    ) {
        if (product == null) {
            return false;
        }

        String name =
                safeLowercase(
                        product.getName()
                );

        String category =
                safeLowercase(
                        product.getCategory()
                );

        return name.contains(query)
                || category.contains(query);
    }

    private String safeLowercase(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.getDefault()
                );
    }

    private void openProductDetails(
            Product product
    ) {
        if (product == null
                || product.getId() == null
                || product.getId()
                .trim()
                .isEmpty()) {

            showMessage(
                    "Unable to open this product."
            );

            return;
        }

        Intent intent =
                new Intent(
                        requireContext(),
                        ProductDetailsActivity.class
                );

        intent.putExtra(
                ProductDetailsActivity
                        .EXTRA_PRODUCT_ID,
                product.getId()
        );

        productDetailsLauncher.launch(
                intent
        );
    }

    private void showMessage(
            String message
    ) {
        View fragmentView =
                getView();

        if (!isAdded()
                || fragmentView == null) {
            return;
        }

        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar snackbar =
                Snackbar.make(
                        fragmentView,
                        safeMessage,
                        Snackbar.LENGTH_LONG
                );

        if (fabAddProduct != null
                && fabAddProduct.getVisibility()
                == View.VISIBLE) {

            snackbar.setAnchorView(
                    fabAddProduct
            );
        }

        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        if (recyclerProducts != null) {
            recyclerProducts.setAdapter(
                    null
            );
        }

        super.onDestroyView();

        recyclerProducts = null;
        fabAddProduct = null;
        etSearch = null;
        adapter = null;
    }
}