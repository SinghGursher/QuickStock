package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quickstock.R;
import com.example.quickstock.models.Product;
import com.example.quickstock.repositories.ProductRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class ProductDetailsActivity
        extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID =
            "product_id";

    public static final String EXTRA_CHANGE_TYPE =
            "product_change_type";

    public static final String EXTRA_PENDING_SYNC =
            "product_pending_sync";

    public static final String CHANGE_UPDATED =
            "product_updated";

    public static final String CHANGE_DELETED =
            "product_deleted";

    /*
     * Receives the result only when AddProductActivity
     * successfully updates the product in Firebase.
     */
    private final ActivityResultLauncher<Intent>
            editProductLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    result -> {
                        if (result.getResultCode()
                                != RESULT_OK) {
                            return;
                        }

                        Intent resultData =
                                result.getData();

                        boolean pendingSync =
                                resultData != null
                                        && resultData.getBooleanExtra(
                                        AddProductActivity
                                                .EXTRA_PENDING_SYNC,
                                        false
                                );

                        returnChangeToInventory(
                                CHANGE_UPDATED,
                                pendingSync
                        );
                    }
            );

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;

    private TextView textProductName;
    private TextView textProductCategory;
    private TextView textProductAmount;
    private TextView textProductPrice;
    private TextView textCostPrice;
    private TextView textProfitPerUnit;
    private TextView textProductStock;
    private TextView textStockStatus;
    private TextView textProductId;

    private LinearLayout layoutPurchaseUnitDetails;
    private TextView textPurchaseUnit;
    private TextView textUnitsPerPurchase;
    private TextView textPurchaseUnitPrice;
    private TextView textCalculatedCostPerItem;

    private LinearLayout layoutQuantityOfferDetails;
    private TextView textQuantityOffer;
    private TextView textOfferNormalTotal;
    private TextView textOfferSaving;
    private TextView textOfferProfit;

    private MaterialButton buttonEditProduct;
    private MaterialButton buttonDeleteProduct;

    private ProductRepository productRepository;

    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_product_details
        );

        initialiseViews();
        setupToolbar();
        setupClickListeners();

        productRepository =
                new ProductRepository();

        productId =
                getIntent().getStringExtra(
                        EXTRA_PRODUCT_ID
                );

        if (productId == null
                || productId.trim().isEmpty()) {

            showBlockingError(
                    "Product unavailable",
                    "A valid product could not be opened."
            );

            return;
        }

        /*
         * Initial load only.
         *
         * There is deliberately no onResume() Firebase
         * reload in this activity.
         */
        loadProduct();
    }

    private void initialiseViews() {
        toolbar =
                findViewById(
                        R.id.toolbarProductDetails
                );

        progressBar =
                findViewById(
                        R.id.progressProductDetails
                );

        textProductName =
                findViewById(
                        R.id.textProductName
                );

        textProductCategory =
                findViewById(
                        R.id.textProductCategory
                );

        textProductAmount =
                findViewById(
                        R.id.textProductAmount
                );

        textProductPrice =
                findViewById(
                        R.id.textProductPrice
                );

        textCostPrice =
                findViewById(
                        R.id.textCostPrice
                );

        textProfitPerUnit =
                findViewById(
                        R.id.textProfitPerUnit
                );

        textProductStock =
                findViewById(
                        R.id.textProductStock
                );

        textStockStatus =
                findViewById(
                        R.id.textStockStatus
                );

        textProductId =
                findViewById(
                        R.id.textProductId
                );

        layoutPurchaseUnitDetails =
                findViewById(
                        R.id.layoutPurchaseUnitDetails
                );

        textPurchaseUnit =
                findViewById(
                        R.id.textPurchaseUnit
                );

        textUnitsPerPurchase =
                findViewById(
                        R.id.textUnitsPerPurchase
                );

        textPurchaseUnitPrice =
                findViewById(
                        R.id.textPurchaseUnitPrice
                );

        textCalculatedCostPerItem =
                findViewById(
                        R.id.textCalculatedCostPerItem
                );

        layoutQuantityOfferDetails =
                findViewById(
                        R.id.layoutQuantityOfferDetails
                );

        textQuantityOffer =
                findViewById(
                        R.id.textQuantityOffer
                );

        textOfferNormalTotal =
                findViewById(
                        R.id.textOfferNormalTotal
                );

        textOfferSaving =
                findViewById(
                        R.id.textOfferSaving
                );

        textOfferProfit =
                findViewById(
                        R.id.textOfferProfit
                );

        buttonEditProduct =
                findViewById(
                        R.id.buttonEditProduct
                );

        buttonDeleteProduct =
                findViewById(
                        R.id.buttonDeleteProduct
                );
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setTitle(
                            "Product Details"
                    );

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(
                            true
                    );
        }

        toolbar.setNavigationOnClickListener(
                view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
        );
    }

    private void setupClickListeners() {
        buttonEditProduct.setOnClickListener(
                view -> openEditProduct()
        );

        buttonDeleteProduct.setOnClickListener(
                view -> showDeleteConfirmation()
        );
    }

    private void loadProduct() {
        if (productId == null
                || productId.trim().isEmpty()) {
            return;
        }

        showLoading(true);

        productRepository.getProductById(
                productId,
                new ProductRepository
                        .OnProductLoadedListener() {

                    @Override
                    public void onProductLoaded(
                            Product product
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoading(false);

                        if (product == null) {
                            showBlockingError(
                                    "Product unavailable",
                                    "This product could not be found."
                            );

                            return;
                        }

                        currentProduct = product;

                        if (currentProduct.getId() == null
                                || currentProduct
                                .getId()
                                .trim()
                                .isEmpty()) {

                            currentProduct.setId(
                                    productId
                            );
                        }

                        normaliseOlderProductData(
                                currentProduct
                        );

                        displayProduct(
                                currentProduct
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoading(false);

                        showBlockingError(
                                "Unable to load product",
                                getErrorMessage(
                                        error,
                                        "Product could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    /**
     * Allows products saved before the newer product fields
     * were introduced to continue loading safely.
     */
    private void normaliseOlderProductData(
            Product product
    ) {
        if (product.getPurchaseUnit() == null
                || product.getPurchaseUnit()
                .trim()
                .isEmpty()) {

            product.setPurchaseUnit(
                    "Unit"
            );
        }

        if (product.getUnitsPerPurchase() <= 0) {
            product.setUnitsPerPurchase(
                    1
            );
        }

        /*
         * Recreate the complete purchase-unit price when an
         * older product contains only costPrice.
         */
        if (product.getPurchaseUnitPrice() <= 0
                && product.getCostPrice() > 0) {

            product.setPurchaseUnitPrice(
                    product.getCostPrice()
                            * product.getUnitsPerPurchase()
            );
        }

        if (!product.isQuantityOfferEnabled()) {
            product.setOfferQuantity(0);
            product.setOfferPrice(0);
        }
    }

    private void displayProduct(
            Product product
    ) {
        textProductName.setText(
                getDisplayText(
                        product.getName(),
                        "Unnamed product"
                )
        );

        textProductCategory.setText(
                getDisplayText(
                        product.getCategory(),
                        "Not specified"
                )
        );

        textProductAmount.setText(
                getDisplayText(
                        product.getAmount(),
                        "Not specified"
                )
        );

        textProductPrice.setText(
                formatMoney(
                        product.getSellingPrice()
                )
        );

        textCostPrice.setText(
                formatMoney(
                        product.getCostPrice()
                )
        );

        textProfitPerUnit.setText(
                formatMoney(
                        product.getProfitPerUnit()
                )
        );

        textProductStock.setText(
                String.format(
                        Locale.getDefault(),
                        "%d units",
                        product.getStock()
                )
        );

        textProductId.setText(
                getDisplayText(
                        product.getId(),
                        productId
                )
        );

        displayStockStatus(
                product.getStock()
        );

        displayPurchaseUnitInformation(
                product
        );

        displayQuantityOffer(
                product
        );
    }

    private void displayPurchaseUnitInformation(
            Product product
    ) {
        if (!product.usesAdvancedPurchaseUnit()) {
            layoutPurchaseUnitDetails.setVisibility(
                    View.GONE
            );

            return;
        }

        layoutPurchaseUnitDetails.setVisibility(
                View.VISIBLE
        );

        textPurchaseUnit.setText(
                getDisplayText(
                        product.getPurchaseUnit(),
                        "Not specified"
                )
        );

        textUnitsPerPurchase.setText(
                String.valueOf(
                        Math.max(
                                product.getUnitsPerPurchase(),
                                1
                        )
                )
        );

        textPurchaseUnitPrice.setText(
                formatMoney(
                        product.getPurchaseUnitPrice()
                )
        );

        textCalculatedCostPerItem.setText(
                formatMoney(
                        product.getCostPrice()
                )
        );
    }

    private void displayQuantityOffer(
            Product product
    ) {
        if (!product.hasValidQuantityOffer()) {
            layoutQuantityOfferDetails.setVisibility(
                    View.GONE
            );

            return;
        }

        layoutQuantityOfferDetails.setVisibility(
                View.VISIBLE
        );

        int offerQuantity =
                product.getOfferQuantity();

        double offerPrice =
                product.getOfferPrice();

        double normalTotal =
                product.getSellingPrice()
                        * offerQuantity;

        double saving =
                normalTotal - offerPrice;

        double offerProfit =
                offerPrice
                        - (
                        product.getCostPrice()
                                * offerQuantity
                );

        textQuantityOffer.setText(
                String.format(
                        Locale.getDefault(),
                        "%d items for KSh %,.2f",
                        offerQuantity,
                        offerPrice
                )
        );

        textOfferNormalTotal.setText(
                formatMoney(
                        normalTotal
                )
        );

        textOfferSaving.setText(
                formatMoney(
                        Math.max(
                                saving,
                                0
                        )
                )
        );

        textOfferProfit.setText(
                formatMoney(
                        offerProfit
                )
        );
    }

    private void displayStockStatus(
            int stock
    ) {
        if (stock <= 0) {
            textStockStatus.setText(
                    "Out of stock"
            );

            textStockStatus.setTextColor(
                    getColor(
                            android.R.color.holo_red_dark
                    )
            );

            return;
        }

        if (stock <= 5) {
            textStockStatus.setText(
                    "Low stock"
            );

            textStockStatus.setTextColor(
                    getColor(
                            android.R.color.holo_orange_dark
                    )
            );

            return;
        }

        textStockStatus.setText(
                "In stock"
        );

        textStockStatus.setTextColor(
                getColor(
                        R.color.primaryGreen
                )
        );
    }

    private void openEditProduct() {
        if (currentProduct == null) {
            showMessage(
                    "Product information is still loading."
            );

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        AddProductActivity.class
                );

        intent.putExtra(
                AddProductActivity.EXTRA_EDIT_MODE,
                true
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_ID,
                currentProduct.getId()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_NAME,
                currentProduct.getName()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_CATEGORY,
                currentProduct.getCategory()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_AMOUNT,
                currentProduct.getAmount()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_COST_PRICE,
                currentProduct.getCostPrice()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_PRICE,
                currentProduct.getSellingPrice()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PURCHASE_UNIT_PRICE,
                currentProduct.getPurchaseUnitPrice()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PRODUCT_STOCK,
                currentProduct.getStock()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_PURCHASE_UNIT,
                currentProduct.getPurchaseUnit()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_UNITS_PER_PURCHASE,
                currentProduct.getUnitsPerPurchase()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_QUANTITY_OFFER_ENABLED,
                currentProduct.isQuantityOfferEnabled()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_OFFER_QUANTITY,
                currentProduct.getOfferQuantity()
        );

        intent.putExtra(
                AddProductActivity.EXTRA_OFFER_PRICE,
                currentProduct.getOfferPrice()
        );

        editProductLauncher.launch(
                intent
        );
    }

    private void showDeleteConfirmation() {
        if (currentProduct == null) {
            showMessage(
                    "Product information is still loading."
            );

            return;
        }

        String productName =
                getDisplayText(
                        currentProduct.getName(),
                        "this product"
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        "Delete product?"
                )
                .setMessage(
                        "This will permanently delete "
                                + productName
                                + " from your inventory."
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) ->
                                dialog.dismiss()
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) ->
                                deleteProduct()
                )
                .show();
    }

    private void deleteProduct() {
        if (productId == null
                || productId.trim().isEmpty()) {

            showMessage(
                    "This product has no valid ID."
            );

            return;
        }

        showLoading(true);

        productRepository.deleteProduct(
                productId,
                new ProductRepository
                        .OnCompleteListener() {

                    @Override
                    public void onSuccess() {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoading(false);

                        /*
                         * Inventory reloads its products and shows
                         * the deletion success message.
                         */
                        returnChangeToInventory(
                                CHANGE_DELETED,
                                false
                        );
                    }

                    @Override
                    public void onQueuedForSync() {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoading(false);

                        returnChangeToInventory(
                                CHANGE_DELETED,
                                true
                        );
                    }
                    @Override
                    public void onFailure(
                            String error
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoading(false);

                        showMessage(
                                getErrorMessage(
                                        error,
                                        "Product could not be deleted."
                                )
                        );
                    }
                }
        );
    }

    private void returnChangeToInventory(
            String changeType,
            boolean pendingSync
    ) {
        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                EXTRA_CHANGE_TYPE,
                changeType
        );

        resultIntent.putExtra(
                EXTRA_PENDING_SYNC,
                pendingSync
        );

        setResult(
                RESULT_OK,
                resultIntent
        );

        finish();
    }

    /*
     * Recoverable feedback stays on the current screen.
     */
    private void showMessage(
            String message
    ) {
        if (isFinishing()
                || isDestroyed()) {
            return;
        }

        String safeMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Something went wrong."
                        : message.trim();

        Snackbar.make(
                findViewById(
                        android.R.id.content
                ),
                safeMessage,
                Snackbar.LENGTH_LONG
        ).show();
    }

    /*
     * Used when this screen cannot continue without valid
     * product information.
     */
    private void showBlockingError(
            String title,
            String message
    ) {
        if (isFinishing()
                || isDestroyed()) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        "Return to inventory",
                        (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        }
                )
                .show();
    }

    private void showLoading(
            boolean loading
    ) {
        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        buttonEditProduct.setEnabled(
                !loading
        );

        buttonDeleteProduct.setEnabled(
                !loading
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

    private String getDisplayText(
            String value,
            String defaultValue
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            return defaultValue;
        }

        return value.trim();
    }

    private String getErrorMessage(
            String error,
            String defaultMessage
    ) {
        if (error == null
                || error.trim().isEmpty()) {

            return defaultMessage;
        }

        return error.trim();
    }
}