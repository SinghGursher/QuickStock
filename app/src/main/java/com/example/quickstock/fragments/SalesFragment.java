package com.example.quickstock.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.activities.MainActivity;
import com.example.quickstock.adapters.SalesProductAdapter;
import com.example.quickstock.models.Product;
import com.example.quickstock.models.SaleItem;
import com.example.quickstock.repositories.ProductRepository;
import com.example.quickstock.repositories.SaleRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesFragment extends Fragment {

    private View salesRoot;
    private View cardSaleSummary;

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
     * Product ID -> selected sale item.
     *
     * LinkedHashMap preserves the order in which products
     * were added to the current sale.
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
        initialiseKeyboardHandling();
        initialiseSaleConfirmationResult();

        updateSaleSummary();
        loadProducts();
    }

    private void initialiseViews(View view) {
        salesRoot =
                view.findViewById(
                        R.id.salesRoot
                );

        cardSaleSummary =
                view.findViewById(
                        R.id.cardSaleSummary
                );

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
                            @Override
                            public void onMessage(
                                    String message
                            ) {
                                showMessage(message);
                            }
                        }
                );

        recyclerSalesProducts.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        recyclerSalesProducts.setHasFixedSize(false);
        recyclerSalesProducts.setAdapter(
                salesProductAdapter
        );

        searchProducts.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        salesProductAdapter.filter(query);
                        hideSearchKeyboard();

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        salesProductAdapter.filter(newText);

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
                view -> showSaleConfirmationSheet()
        );
    }

    private void initialiseKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(
                salesRoot,
                (view, windowInsets) -> {
                    boolean keyboardVisible =
                            windowInsets.isVisible(
                                    WindowInsetsCompat
                                            .Type
                                            .ime()
                            );

                    updateKeyboardUi(
                            keyboardVisible
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(
                salesRoot
        );
    }

    private void initialiseSaleConfirmationResult() {
        getParentFragmentManager()
                .setFragmentResultListener(
                        SaleConfirmationBottomSheet
                                .REQUEST_KEY,
                        getViewLifecycleOwner(),
                        (requestKey, result) -> {
                            boolean confirmed =
                                    result.getBoolean(
                                            SaleConfirmationBottomSheet
                                                    .KEY_CONFIRMED,
                                            false
                                    );

                            if (confirmed) {
                                completeSale();
                            }
                        }
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
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        showLoading(false);

                        List<Product> safeProducts =
                                products == null
                                        ? new ArrayList<>()
                                        : new ArrayList<>(
                                        products
                                );

                        /*
                         * Arrange products alphabetically by name.
                         */
                        safeProducts.sort(
                                Comparator.comparing(
                                        product ->
                                                getSafeProductName(
                                                        product
                                                ),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        );

                        salesProductAdapter.setProducts(
                                safeProducts
                        );

                        /*
                         * Preserve the current search after refreshing
                         * product data.
                         */
                        CharSequence currentQuery =
                                searchProducts.getQuery();

                        if (currentQuery != null
                                && currentQuery.length() > 0) {
                            salesProductAdapter.filter(
                                    currentQuery.toString()
                            );
                        }

                        textEmptyProducts.setVisibility(
                                safeProducts.isEmpty()
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        /*
                         * Update selected products using the latest
                         * stock, price and offer information.
                         */
                        synchroniseCartWithProducts(
                                safeProducts
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (!isAdded()
                                || getView() == null) {
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
            cartItems.remove(productId);

        } else {
            try {
                SaleItem saleItem =
                        SaleItem.fromProduct(
                                product,
                                safeQuantity
                        );

                cartItems.put(
                        productId,
                        saleItem
                );

            } catch (IllegalArgumentException exception) {
                salesProductAdapter.setQuantity(
                        productId,
                        0
                );

                showMessage(
                        exception.getMessage()
                );

                return;
            }
        }

        updateSaleSummary();
    }

    private void updateKeyboardUi(
            boolean keyboardVisible
    ) {
        if (cardSaleSummary != null) {
            cardSaleSummary.setVisibility(
                    keyboardVisible
                            ? View.GONE
                            : View.VISIBLE
            );
        }

        if (getActivity()
                instanceof MainActivity) {

            MainActivity mainActivity =
                    (MainActivity) getActivity();

            mainActivity.setBottomNavigationVisible(
                    !keyboardVisible
            );
        }
    }

    private void hideSearchKeyboard() {
        if (searchProducts == null) {
            return;
        }

        searchProducts.clearFocus();

        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(
                        searchProducts
                );

        if (controller != null) {
            controller.hide(
                    WindowInsetsCompat
                            .Type
                            .ime()
            );
        }
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

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(
                        requireContext()
                )
                        .setTitle(
                                getSafeProductName(
                                        product
                                )
                        )
                        .setMessage(
                                buildQuantityDialogMessage(
                                        product
                                )
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

    private void showSaleConfirmationSheet() {
        if (!isAdded()
                || saleInProgress
                || cartItems.isEmpty()
                || getParentFragmentManager()
                .isStateSaved()) {

            return;
        }

        if (getParentFragmentManager()
                .findFragmentByTag(
                        SaleConfirmationBottomSheet.TAG
                ) != null) {

            return;
        }

        List<SaleItem> validItems =
                getValidSaleItems();

        if (validItems.isEmpty()) {
            showMessage(
                    "Select at least one valid product."
            );

            return;
        }

        ArrayList<String> productNames =
                new ArrayList<>();

        ArrayList<Integer> quantities =
                new ArrayList<>();

        double[] subtotals =
                new double[validItems.size()];

        int totalUnits = 0;
        double totalAmount = 0;
        double totalSaving = 0;

        for (int index = 0;
             index < validItems.size();
             index++) {

            SaleItem item =
                    validItems.get(index);

            productNames.add(
                    getSafeSaleItemName(item)
            );

            quantities.add(
                    item.getQuantity()
            );

            subtotals[index] =
                    item.getSubtotal();

            totalUnits +=
                    item.getQuantity();

            totalAmount +=
                    item.getSubtotal();

            totalSaving +=
                    item.getCustomerSaving();
        }

        hideSearchKeyboard();

        SaleConfirmationBottomSheet sheet =
                SaleConfirmationBottomSheet
                        .newInstance(
                                productNames,
                                quantities,
                                subtotals,
                                totalUnits,
                                totalAmount,
                                totalSaving
                        );

        sheet.show(
                getParentFragmentManager(),
                SaleConfirmationBottomSheet.TAG
        );
    }

    private List<SaleItem> getValidSaleItems() {
        List<SaleItem> validItems =
                new ArrayList<>();

        for (SaleItem item
                : cartItems.values()) {

            if (item == null
                    || item.getQuantity() <= 0) {
                continue;
            }

            item.recalculate();
            validItems.add(item);
        }

        return validItems;
    }

    private void completeSale() {
        if (saleInProgress) {
            return;
        }

        List<SaleItem> saleItems =
                getValidSaleItems();

        if (saleItems.isEmpty()) {
            showSaleError(
                    "Select at least one valid product."
            );

            return;
        }

        final double completedTotal =
                calculateSaleTotal(
                        saleItems
                );

        setSaleInProgress(true);

        saleRepository.completeSale(
                saleItems,
                new SaleRepository
                        .OnSaleCompleteListener() {

                    @Override
                    public void onSuccess(
                            String saleId
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        setSaleInProgress(false);
                        clearSale();

                        SaleConfirmationBottomSheet sheet =
                                findSaleConfirmationSheet();

                        if (sheet != null) {
                            sheet.showSuccess(
                                    completedTotal,
                                    saleId
                            );

                        } else {
                            showMessage(
                                    formatMoney(
                                            completedTotal
                                    )
                                            + " was recorded successfully."
                            );
                        }

                        /*
                         * Display the reduced stock quantities.
                         */
                        loadProducts();
                    }

                    @Override
                    public void onQueuedForSync(
                            String saleId
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        setSaleInProgress(false);
                        clearSale();

                        SaleConfirmationBottomSheet sheet =
                                findSaleConfirmationSheet();

                        if (sheet != null) {
                            sheet.showQueued(
                                    completedTotal,
                                    saleId
                            );

                        } else {
                            showMessage(
                                    formatMoney(
                                            completedTotal
                                    )
                                            + " was saved offline and will sync automatically."
                            );
                        }

                        /*
                         * The atomic write has already reduced
                         * stock in Firebase's local cache.
                         */
                        loadProducts();
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        setSaleInProgress(false);
                        showSaleError(error);
                    }
                }
        );
    }

    private double calculateSaleTotal(
            List<SaleItem> saleItems
    ) {
        double total = 0;

        if (saleItems == null) {
            return total;
        }

        for (SaleItem item : saleItems) {
            if (item == null) {
                continue;
            }

            item.recalculate();

            total +=
                    item.getSubtotal();
        }

        return total;
    }

    @Nullable
    private SaleConfirmationBottomSheet
    findSaleConfirmationSheet() {
        Fragment fragment =
                getParentFragmentManager()
                        .findFragmentByTag(
                                SaleConfirmationBottomSheet.TAG
                        );

        if (fragment
                instanceof SaleConfirmationBottomSheet) {

            return (SaleConfirmationBottomSheet) fragment;
        }

        return null;
    }

    private void showSaleError(
            String error
    ) {
        String message =
                error == null
                        || error.trim().isEmpty()
                        ? "The sale could not be completed."
                        : error.trim();

        SaleConfirmationBottomSheet sheet =
                findSaleConfirmationSheet();

        if (sheet != null) {
            sheet.showError(message);

        } else {
            showMessage(message);
        }
    }

    private void clearSale() {
        cartItems.clear();

        if (salesProductAdapter != null) {
            salesProductAdapter
                    .clearQuantities();
        }

        updateSaleSummary();
    }

    private void updateSaleSummary() {
        if (textSelectedItems == null
                || textSaleTotal == null
                || textCustomerSaving == null
                || buttonClearSale == null
                || buttonCompleteSale == null) {

            return;
        }

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
                            formatMoney(
                                    totalSaving
                            )
                    )
            );

        } else {
            textCustomerSaving.setVisibility(
                    View.GONE
            );

            textCustomerSaving.setText("");
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

    private void synchroniseCartWithProducts(
            List<Product> products
    ) {
        if (products == null
                || salesProductAdapter == null) {
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

        boolean selectionAdjusted = false;

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

                selectionAdjusted = true;
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

                selectionAdjusted = true;
                continue;
            }

            if (correctedQuantity
                    != currentItem.getQuantity()) {
                selectionAdjusted = true;
            }

            try {
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

            } catch (IllegalArgumentException exception) {
                cartItems.remove(
                        productId
                );

                salesProductAdapter.setQuantity(
                        productId,
                        0
                );

                selectionAdjusted = true;
            }
        }

        updateSaleSummary();

        if (selectionAdjusted
                && !cartItems.isEmpty()) {

            showMessage(
                    "Some selected quantities were adjusted to match the latest inventory."
            );
        }
    }

    private void setSaleInProgress(
            boolean inProgress
    ) {
        saleInProgress = inProgress;

        if (buttonCompleteSale == null) {
            return;
        }

        buttonCompleteSale.setText(
                inProgress
                        ? "Completing..."
                        : getString(
                        R.string.complete_sale
                )
        );

        if (searchProducts != null) {
            searchProducts.setEnabled(
                    !inProgress
            );
        }

        if (recyclerSalesProducts != null) {
            recyclerSalesProducts.setEnabled(
                    !inProgress
            );
        }

        updateSaleSummary();
    }

    private void showLoading(
            boolean loading
    ) {
        if (progressProducts == null
                || recyclerSalesProducts == null
                || searchProducts == null) {
            return;
        }

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

        if (loading
                && textEmptyProducts != null) {

            textEmptyProducts.setVisibility(
                    View.GONE
            );
        }
    }

    private String formatMoney(double amount) {
        return String.format(
                Locale.US,
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

        return product.getName().trim();
    }

    private String getSafeSaleItemName(
            SaleItem item
    ) {
        if (item == null
                || item.getProductName() == null
                || item.getProductName()
                .trim()
                .isEmpty()) {

            return "Product";
        }

        return item.getProductName().trim();
    }

    /*
     * Professional in-screen feedback for non-sale messages.
     * Sale success and failure remain inside the bottom sheet.
     */
    private void showMessage(
            String message
    ) {
        if (!isAdded()
                || salesRoot == null) {
            return;
        }

        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar snackbar =
                Snackbar.make(
                        salesRoot,
                        safeMessage,
                        Snackbar.LENGTH_LONG
                );

        if (cardSaleSummary != null
                && cardSaleSummary.getVisibility()
                == View.VISIBLE) {

            snackbar.setAnchorView(
                    cardSaleSummary
            );
        }

        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        if (getActivity()
                instanceof MainActivity) {

            MainActivity mainActivity =
                    (MainActivity) getActivity();

            mainActivity.setBottomNavigationVisible(
                    true
            );
        }

        if (salesRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(
                    salesRoot,
                    null
            );
        }

        if (searchProducts != null) {
            searchProducts.setOnQueryTextListener(
                    null
            );
        }

        if (recyclerSalesProducts != null) {
            recyclerSalesProducts.setAdapter(
                    null
            );
        }

        super.onDestroyView();

        salesRoot = null;
        cardSaleSummary = null;
        recyclerSalesProducts = null;
        searchProducts = null;
        progressProducts = null;
        textEmptyProducts = null;
        textSelectedItems = null;
        textSaleTotal = null;
        textCustomerSaving = null;
        buttonClearSale = null;
        buttonCompleteSale = null;
        salesProductAdapter = null;
    }
}