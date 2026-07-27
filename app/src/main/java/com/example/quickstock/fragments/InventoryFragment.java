package com.example.quickstock.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryFragment extends Fragment {

    private static final String TAG = "InventoryFragment";

    private RecyclerView recyclerProducts;
    private FloatingActionButton fabAddProduct;
    private TextInputEditText etSearch;

    /*
     * Contains every product loaded from Firebase.
     */
    private final ArrayList<Product> allProducts =
            new ArrayList<>();

    /*
     * Contains only products currently displayed.
     *
     * ProductAdapter uses this list.
     */
    private final ArrayList<Product> displayedProducts =
            new ArrayList<>();

    private ProductAdapter adapter;
    private ProductRepository productRepository;

    /*
     * Stores the current search query so that Firebase updates
     * do not automatically clear the filtered results.
     */
    private String currentSearchQuery = "";

    public InventoryFragment() {
        // Required empty public constructor
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
        super.onViewCreated(view, savedInstanceState);

        initialiseViews(view);

        productRepository =
                new ProductRepository();

        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        loadProducts();
    }

    private void initialiseViews(View view) {

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

        adapter = new ProductAdapter(
                displayedProducts,
                this::openProductDetails
        );

        recyclerProducts.setAdapter(adapter);
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
                        // No action required
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
                        // No action required
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

                    startActivity(intent);
                }
        );
    }

    private void loadProducts() {

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

                        allProducts.clear();

                        if (products != null) {
                            allProducts.addAll(products);
                        }

                        /*
                         * Reapply the current search after Firebase
                         * sends an updated product list.
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
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                TAG,
                                "Failed to load products: "
                                        + error
                        );

                        Toast.makeText(
                                requireContext(),
                                "Failed to load products: "
                                        + error,
                                Toast.LENGTH_LONG
                        ).show();
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

            for (Product product : allProducts) {

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

        /*
         * Search by product name or category.
         */
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
                || product.getId() == null) {

            Toast.makeText(
                    requireContext(),
                    "Unable to open this product.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        requireContext(),
                        ProductDetailsActivity.class
                );

        intent.putExtra(
                ProductDetailsActivity.EXTRA_PRODUCT_ID,
                product.getId()
        );

        startActivity(intent);
    }
}