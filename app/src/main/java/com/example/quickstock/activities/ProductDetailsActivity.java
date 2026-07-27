package com.example.quickstock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickstock.R;
import com.example.quickstock.models.Product;
import com.example.quickstock.repositories.ProductRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class ProductDetailsActivity
        extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID =
            "product_id";

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

            Toast.makeText(
                    this,
                    "Invalid product ID.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
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
                        showLoading(false);

                        if (product == null) {

                            Toast.makeText(
                                    ProductDetailsActivity.this,
                                    "Product could not be found.",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
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
                        showLoading(false);

                        Toast.makeText(
                                ProductDetailsActivity.this,
                                getErrorMessage(
                                        error,
                                        "Product could not be loaded."
                                ),
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );
    }

    /**
     * Allows products saved before the new fields were added
     * to continue loading without crashing.
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
         * older product only contains costPrice.
         */
        if (product.getPurchaseUnitPrice() <= 0
                && product.getCostPrice() > 0) {

            product.setPurchaseUnitPrice(
                    product.getCostPrice()
                            * product.getUnitsPerPurchase()
            );
        }

        /*
         * Clear invalid offer fields for products where
         * quantity offers are disabled.
         */
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

            Toast.makeText(
                    this,
                    "Product is still loading.",
                    Toast.LENGTH_SHORT
            ).show();

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

        startActivity(intent);
    }

    private void showDeleteConfirmation() {

        if (currentProduct == null) {

            Toast.makeText(
                    this,
                    "Product is still loading.",
                    Toast.LENGTH_SHORT
            ).show();

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

            return;
        }

        showLoading(true);

        productRepository.deleteProduct(
                productId,
                new ProductRepository
                        .OnCompleteListener() {

                    @Override
                    public void onSuccess() {

                        showLoading(false);

                        Toast.makeText(
                                ProductDetailsActivity.this,
                                "Product deleted successfully.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        showLoading(false);

                        Toast.makeText(
                                ProductDetailsActivity.this,
                                getErrorMessage(
                                        error,
                                        "Product could not be deleted."
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
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

        return value;
    }

    private String getErrorMessage(
            String error,
            String defaultMessage
    ) {

        if (error == null
                || error.trim().isEmpty()) {

            return defaultMessage;
        }

        return error;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (productRepository != null
                && productId != null
                && !productId.trim().isEmpty()) {

            loadProduct();
        }
    }
}