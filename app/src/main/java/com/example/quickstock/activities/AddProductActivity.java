package com.example.quickstock.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickstock.R;
import com.example.quickstock.models.Product;
import com.example.quickstock.repositories.ProductRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class AddProductActivity
        extends AppCompatActivity {

    public static final String EXTRA_EDIT_MODE =
            "edit_mode";

    public static final String EXTRA_PRODUCT_ID =
            "product_id";

    public static final String EXTRA_PRODUCT_NAME =
            "product_name";

    public static final String EXTRA_PRODUCT_CATEGORY =
            "product_category";

    public static final String EXTRA_PRODUCT_AMOUNT =
            "product_amount";

    public static final String EXTRA_PRODUCT_COST_PRICE =
            "product_cost_price";

    public static final String EXTRA_PRODUCT_PRICE =
            "product_price";

    public static final String EXTRA_PURCHASE_UNIT_PRICE =
            "purchase_unit_price";

    public static final String EXTRA_PRODUCT_STOCK =
            "product_stock";

    public static final String EXTRA_PURCHASE_UNIT =
            "purchase_unit";

    public static final String EXTRA_UNITS_PER_PURCHASE =
            "units_per_purchase";

    public static final String EXTRA_QUANTITY_OFFER_ENABLED =
            "quantity_offer_enabled";

    public static final String EXTRA_OFFER_QUANTITY =
            "offer_quantity";

    public static final String EXTRA_OFFER_PRICE =
            "offer_price";

    private TextInputEditText etName;
    private TextInputEditText etCategory;
    private TextInputEditText etAmount;
    private TextInputEditText etPrice;
    private TextInputEditText etCostPrice;
    private TextInputEditText etStock;

    private TextInputEditText etPurchaseUnit;
    private TextInputEditText etUnitsPerPurchase;
    private TextInputEditText etPurchaseUnitPrice;
    private TextInputEditText etCalculatedCostPrice;

    private TextInputEditText etOfferQuantity;
    private TextInputEditText etOfferPrice;

    private TextInputLayout layoutNormalCostPrice;

    private CheckBox checkAdvanced;
    private CheckBox checkQuantityOffer;

    private LinearLayout layoutAdvanced;
    private LinearLayout layoutQuantityOffer;

    private TextView textOfferPreview;
    private MaterialButton btnSave;

    private ProductRepository productRepository;

    private boolean isEditMode;
    private boolean populatingFields;
    private String productId;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_add_product
        );

        initialiseViews();

        productRepository =
                new ProductRepository();

        setupAdvancedPurchaseOption();
        setupQuantityOfferOption();
        setupCalculationWatchers();
        setupSaveButton();

        readEditMode();
    }

    private void initialiseViews() {

        etName = findViewById(R.id.etName);
        etCategory = findViewById(R.id.etCategory);
        etAmount = findViewById(R.id.etAmount);
        etPrice = findViewById(R.id.etPrice);
        etCostPrice = findViewById(R.id.etCostPrice);
        etStock = findViewById(R.id.etStock);

        etPurchaseUnit =
                findViewById(R.id.etPurchaseUnit);

        etUnitsPerPurchase =
                findViewById(
                        R.id.etUnitsPerPurchase
                );

        etPurchaseUnitPrice =
                findViewById(
                        R.id.etPurchaseUnitPrice
                );

        etCalculatedCostPrice =
                findViewById(
                        R.id.etCalculatedCostPrice
                );

        etOfferQuantity =
                findViewById(
                        R.id.etOfferQuantity
                );

        etOfferPrice =
                findViewById(
                        R.id.etOfferPrice
                );

        layoutNormalCostPrice =
                findViewById(
                        R.id.layoutNormalCostPrice
                );

        checkAdvanced =
                findViewById(R.id.checkAdvanced);

        checkQuantityOffer =
                findViewById(
                        R.id.checkQuantityOffer
                );

        layoutAdvanced =
                findViewById(R.id.layoutAdvanced);

        layoutQuantityOffer =
                findViewById(
                        R.id.layoutQuantityOffer
                );

        textOfferPreview =
                findViewById(
                        R.id.textOfferPreview
                );

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupAdvancedPurchaseOption() {

        checkAdvanced.setOnCheckedChangeListener(
                (buttonView, checked) -> {

                    layoutAdvanced.setVisibility(
                            checked
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    layoutNormalCostPrice.setVisibility(
                            checked
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    if (checked) {
                        calculateAndDisplayUnitCost();
                    } else {
                        etCalculatedCostPrice.setText("");
                        clearAdvancedErrors();
                    }
                }
        );
    }

    private void setupQuantityOfferOption() {

        checkQuantityOffer
                .setOnCheckedChangeListener(
                        (buttonView, checked) -> {

                            layoutQuantityOffer
                                    .setVisibility(
                                            checked
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                            if (checked) {
                                updateOfferPreview();
                            } else {
                                textOfferPreview.setText(
                                        "Enter the offer details"
                                );

                                clearOfferErrors();
                            }
                        }
                );
    }

    private void setupCalculationWatchers() {

        TextWatcher watcher =
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence sequence,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence sequence,
                            int start,
                            int before,
                            int count
                    ) {

                        if (populatingFields) {
                            return;
                        }

                        if (checkAdvanced.isChecked()) {
                            calculateAndDisplayUnitCost();
                        }

                        if (checkQuantityOffer.isChecked()) {
                            updateOfferPreview();
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                    }
                };

        etUnitsPerPurchase
                .addTextChangedListener(watcher);

        etPurchaseUnitPrice
                .addTextChangedListener(watcher);

        etPrice.addTextChangedListener(watcher);
        etOfferQuantity.addTextChangedListener(watcher);
        etOfferPrice.addTextChangedListener(watcher);
    }

    private void setupSaveButton() {

        btnSave.setOnClickListener(
                view -> saveProduct()
        );
    }

    private void readEditMode() {

        isEditMode =
                getIntent().getBooleanExtra(
                        EXTRA_EDIT_MODE,
                        false
                );

        if (!isEditMode) {
            btnSave.setText("Save Product");
            return;
        }

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
            return;
        }

        btnSave.setText("Update Product");
        populateProductFields();
    }

    private void populateProductFields() {

        populatingFields = true;

        String name =
                getIntent().getStringExtra(
                        EXTRA_PRODUCT_NAME
                );

        String category =
                getIntent().getStringExtra(
                        EXTRA_PRODUCT_CATEGORY
                );

        String amount =
                getIntent().getStringExtra(
                        EXTRA_PRODUCT_AMOUNT
                );

        double costPrice =
                getIntent().getDoubleExtra(
                        EXTRA_PRODUCT_COST_PRICE,
                        0
                );

        double sellingPrice =
                getIntent().getDoubleExtra(
                        EXTRA_PRODUCT_PRICE,
                        0
                );

        double purchaseUnitPrice =
                getIntent().getDoubleExtra(
                        EXTRA_PURCHASE_UNIT_PRICE,
                        0
                );

        int stock =
                getIntent().getIntExtra(
                        EXTRA_PRODUCT_STOCK,
                        0
                );

        String purchaseUnit =
                getIntent().getStringExtra(
                        EXTRA_PURCHASE_UNIT
                );

        int unitsPerPurchase =
                getIntent().getIntExtra(
                        EXTRA_UNITS_PER_PURCHASE,
                        1
                );

        boolean offerEnabled =
                getIntent().getBooleanExtra(
                        EXTRA_QUANTITY_OFFER_ENABLED,
                        false
                );

        int offerQuantity =
                getIntent().getIntExtra(
                        EXTRA_OFFER_QUANTITY,
                        0
                );

        double offerPrice =
                getIntent().getDoubleExtra(
                        EXTRA_OFFER_PRICE,
                        0
                );

        boolean advanced =
                purchaseUnit != null
                        && !purchaseUnit.trim().isEmpty()
                        && (
                        !purchaseUnit.equalsIgnoreCase(
                                "Unit"
                        )
                                || unitsPerPurchase > 1
                                || purchaseUnitPrice > costPrice
                );

        etName.setText(
                name == null ? "" : name
        );

        etCategory.setText(
                category == null ? "" : category
        );

        etAmount.setText(
                amount == null ? "" : amount
        );

        etPrice.setText(
                sellingPrice > 0
                        ? formatEditableNumber(
                        sellingPrice
                )
                        : ""
        );

        etStock.setText(
                String.valueOf(stock)
        );

        checkAdvanced.setChecked(advanced);

        layoutAdvanced.setVisibility(
                advanced
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutNormalCostPrice.setVisibility(
                advanced
                        ? View.GONE
                        : View.VISIBLE
        );

        if (advanced) {

            etPurchaseUnit.setText(purchaseUnit);

            etUnitsPerPurchase.setText(
                    String.valueOf(
                            Math.max(
                                    unitsPerPurchase,
                                    1
                            )
                    )
            );

            if (purchaseUnitPrice <= 0
                    && costPrice > 0
                    && unitsPerPurchase > 0) {

                purchaseUnitPrice =
                        costPrice
                                * unitsPerPurchase;
            }

            etPurchaseUnitPrice.setText(
                    purchaseUnitPrice > 0
                            ? formatEditableNumber(
                            purchaseUnitPrice
                    )
                            : ""
            );

            etCalculatedCostPrice.setText(
                    costPrice > 0
                            ? formatMoneyValue(
                            costPrice
                    )
                            : ""
            );

        } else {

            etCostPrice.setText(
                    costPrice > 0
                            ? formatEditableNumber(
                            costPrice
                    )
                            : ""
            );
        }

        checkQuantityOffer.setChecked(
                offerEnabled
        );

        layoutQuantityOffer.setVisibility(
                offerEnabled
                        ? View.VISIBLE
                        : View.GONE
        );

        if (offerEnabled) {

            etOfferQuantity.setText(
                    offerQuantity > 0
                            ? String.valueOf(
                            offerQuantity
                    )
                            : ""
            );

            etOfferPrice.setText(
                    offerPrice > 0
                            ? formatEditableNumber(
                            offerPrice
                    )
                            : ""
            );
        }

        populatingFields = false;

        if (advanced) {
            calculateAndDisplayUnitCost();
        }

        if (offerEnabled) {
            updateOfferPreview();
        }
    }

    private void calculateAndDisplayUnitCost() {

        int units =
                parseInteger(
                        getText(
                                etUnitsPerPurchase
                        )
                );

        double purchasePrice =
                parseDouble(
                        getText(
                                etPurchaseUnitPrice
                        )
                );

        if (units <= 0 || purchasePrice <= 0) {
            etCalculatedCostPrice.setText("");
            return;
        }

        double costPerItem =
                purchasePrice / units;

        etCalculatedCostPrice.setText(
                formatMoneyValue(
                        costPerItem
                )
        );
    }

    private void updateOfferPreview() {

        double normalPrice =
                parseDouble(
                        getText(etPrice)
                );

        int quantity =
                parseInteger(
                        getText(etOfferQuantity)
                );

        double offerPrice =
                parseDouble(
                        getText(etOfferPrice)
                );

        if (normalPrice <= 0
                || quantity <= 1
                || offerPrice <= 0) {

            textOfferPreview.setText(
                    "Enter the offer details"
            );

            return;
        }

        double normalTotal =
                normalPrice * quantity;

        double saving =
                normalTotal - offerPrice;

        if (saving <= 0) {

            textOfferPreview.setText(
                    "This offer is not cheaper than the normal price."
            );

            return;
        }

        textOfferPreview.setText(
                String.format(
                        Locale.getDefault(),
                        "%d items for KSh %,.2f • Customer saves KSh %,.2f",
                        quantity,
                        offerPrice,
                        saving
                )
        );
    }

    private void saveProduct() {

        clearErrors();

        String name = getText(etName);
        String category = getText(etCategory);
        String amount = getText(etAmount);

        double sellingPrice =
                parseDouble(
                        getText(etPrice)
                );

        int stock =
                parseInteger(
                        getText(etStock)
                );

        if (name.isEmpty()) {

            etName.setError(
                    "Product name is required."
            );

            etName.requestFocus();
            return;
        }

        if (sellingPrice <= 0) {

            etPrice.setError(
                    "Enter a valid selling price."
            );

            etPrice.requestFocus();
            return;
        }

        if (getText(etStock).isEmpty()
                || stock < 0) {

            etStock.setError(
                    "Enter a valid stock quantity."
            );

            etStock.requestFocus();
            return;
        }

        double costPrice;
        double purchaseUnitPrice;
        String purchaseUnit;
        int unitsPerPurchase;

        if (checkAdvanced.isChecked()) {

            purchaseUnit =
                    getText(etPurchaseUnit);

            unitsPerPurchase =
                    parseInteger(
                            getText(
                                    etUnitsPerPurchase
                            )
                    );

            purchaseUnitPrice =
                    parseDouble(
                            getText(
                                    etPurchaseUnitPrice
                            )
                    );

            if (purchaseUnit.isEmpty()) {

                etPurchaseUnit.setError(
                        "Enter the purchase unit."
                );

                etPurchaseUnit.requestFocus();
                return;
            }

            if (unitsPerPurchase <= 0) {

                etUnitsPerPurchase.setError(
                        "Enter the number of items in the purchase unit."
                );

                etUnitsPerPurchase.requestFocus();
                return;
            }

            if (purchaseUnitPrice <= 0) {

                etPurchaseUnitPrice.setError(
                        "Enter the complete purchase-unit price."
                );

                etPurchaseUnitPrice.requestFocus();
                return;
            }

            costPrice =
                    purchaseUnitPrice
                            / unitsPerPurchase;

        } else {

            costPrice =
                    parseDouble(
                            getText(etCostPrice)
                    );

            if (costPrice <= 0) {

                etCostPrice.setError(
                        "Enter a valid cost price."
                );

                etCostPrice.requestFocus();
                return;
            }

            purchaseUnit = "Unit";
            unitsPerPurchase = 1;
            purchaseUnitPrice = costPrice;
        }

        /*
         * The ordinary single-item price cannot be below cost.
         */
        if (sellingPrice < costPrice) {

            etPrice.setError(
                    String.format(
                            Locale.getDefault(),
                            "Selling price cannot be below the item cost of KSh %,.2f.",
                            costPrice
                    )
            );

            etPrice.requestFocus();
            return;
        }

        boolean offerEnabled =
                checkQuantityOffer.isChecked();

        int offerQuantity = 0;
        double offerPrice = 0;

        if (offerEnabled) {

            offerQuantity =
                    parseInteger(
                            getText(etOfferQuantity)
                    );

            offerPrice =
                    parseDouble(
                            getText(etOfferPrice)
                    );

            if (offerQuantity <= 1) {

                etOfferQuantity.setError(
                        "Offer quantity must be at least 2."
                );

                etOfferQuantity.requestFocus();
                return;
            }

            if (offerPrice <= 0) {

                etOfferPrice.setError(
                        "Enter a valid offer price."
                );

                etOfferPrice.requestFocus();
                return;
            }

            double normalTotal =
                    sellingPrice * offerQuantity;

            if (offerPrice >= normalTotal) {

                etOfferPrice.setError(
                        String.format(
                                Locale.getDefault(),
                                "Offer price must be below the normal total of KSh %,.2f.",
                                normalTotal
                        )
                );

                etOfferPrice.requestFocus();
                return;
            }

            double offerCost =
                    costPrice * offerQuantity;

            /*
             * Prevent offers that sell the group below its cost.
             */
            if (offerPrice < offerCost) {

                etOfferPrice.setError(
                        String.format(
                                Locale.getDefault(),
                                "Offer price cannot be below the total cost of KSh %,.2f.",
                                offerCost
                        )
                );

                etOfferPrice.requestFocus();
                return;
            }
        }

        Product product =
                new Product(
                        isEditMode
                                ? productId
                                : null,
                        name,
                        category,
                        amount,
                        costPrice,
                        sellingPrice,
                        purchaseUnitPrice,
                        stock,
                        purchaseUnit,
                        unitsPerPurchase,
                        offerEnabled,
                        offerQuantity,
                        offerPrice
                );

        btnSave.setEnabled(false);

        if (isEditMode) {
            updateProduct(product);
        } else {
            addProduct(product);
        }
    }

    private void addProduct(Product product) {

        productRepository.addProduct(
                product,
                new ProductRepository
                        .OnCompleteListener() {

                    @Override
                    public void onSuccess() {

                        Toast.makeText(
                                AddProductActivity.this,
                                "Product added successfully.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onFailure(String error) {

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                AddProductActivity.this,
                                getErrorMessage(
                                        error,
                                        "Product could not be added."
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void updateProduct(Product product) {

        productRepository.updateProduct(
                product,
                new ProductRepository
                        .OnCompleteListener() {

                    @Override
                    public void onSuccess() {

                        Toast.makeText(
                                AddProductActivity.this,
                                "Product updated successfully.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onFailure(String error) {

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                AddProductActivity.this,
                                getErrorMessage(
                                        error,
                                        "Product could not be updated."
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private String getText(
            TextInputEditText editText
    ) {

        if (editText.getText() == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private double parseDouble(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return 0;
        }

        try {
            return Double.parseDouble(
                    value.trim()
            );
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private int parseInteger(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return 0;
        }

        try {
            return Integer.parseInt(
                    value.trim()
            );
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void clearErrors() {

        etName.setError(null);
        etCategory.setError(null);
        etAmount.setError(null);
        etPrice.setError(null);
        etCostPrice.setError(null);
        etStock.setError(null);

        clearAdvancedErrors();
        clearOfferErrors();
    }

    private void clearAdvancedErrors() {

        etPurchaseUnit.setError(null);
        etUnitsPerPurchase.setError(null);
        etPurchaseUnitPrice.setError(null);
    }

    private void clearOfferErrors() {

        etOfferQuantity.setError(null);
        etOfferPrice.setError(null);
    }

    private String formatEditableNumber(
            double value
    ) {

        if (value == Math.floor(value)) {

            return String.format(
                    Locale.getDefault(),
                    "%.0f",
                    value
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.2f",
                value
        );
    }

    private String formatMoneyValue(
            double value
    ) {

        return String.format(
                Locale.getDefault(),
                "%.2f",
                value
        );
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
}