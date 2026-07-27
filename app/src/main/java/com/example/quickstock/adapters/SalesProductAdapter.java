package com.example.quickstock.adapters;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.models.Product;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesProductAdapter
        extends RecyclerView.Adapter<
        SalesProductAdapter.ProductViewHolder> {

    public interface OnQuantityChangedListener {

        void onQuantityChanged(
                Product product,
                int quantity
        );

        void onQuantityClicked(
                Product product,
                int currentQuantity
        );
    }

    private final List<Product> allProducts =
            new ArrayList<>();

    private final List<Product> displayedProducts =
            new ArrayList<>();

    private final Map<String, Integer>
            selectedQuantities =
            new HashMap<>();

    private final OnQuantityChangedListener listener;

    public SalesProductAdapter(
            OnQuantityChangedListener listener
    ) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_sales_product,
                                parent,
                                false
                        );

        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position
    ) {

        Product product =
                displayedProducts.get(position);

        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return displayedProducts.size();
    }

    public void setProducts(
            List<Product> products
    ) {

        allProducts.clear();

        if (products != null) {
            allProducts.addAll(products);
        }

        displayedProducts.clear();
        displayedProducts.addAll(allProducts);

        removeQuantitiesForDeletedProducts();

        notifyDataSetChanged();
    }

    public void filter(
            String searchText
    ) {

        displayedProducts.clear();

        if (searchText == null
                || searchText.trim().isEmpty()) {

            displayedProducts.addAll(
                    allProducts
            );

        } else {

            String cleanSearchText =
                    searchText
                            .trim()
                            .toLowerCase(
                                    Locale.getDefault()
                            );

            for (Product product : allProducts) {

                String productName =
                        product.getName() == null
                                ? ""
                                : product.getName()
                                .toLowerCase(
                                        Locale.getDefault()
                                );

                String category =
                        product.getCategory() == null
                                ? ""
                                : product.getCategory()
                                .toLowerCase(
                                        Locale.getDefault()
                                );

                if (productName.contains(
                        cleanSearchText
                ) || category.contains(
                        cleanSearchText
                )) {

                    displayedProducts.add(
                            product
                    );
                }
            }
        }

        notifyDataSetChanged();
    }

    public void setQuantity(
            String productId,
            int quantity
    ) {

        if (productId == null
                || productId.trim().isEmpty()) {

            return;
        }

        Product product =
                findProductById(productId);

        int safeQuantity = quantity;

        if (product != null) {

            safeQuantity =
                    Math.max(
                            0,
                            Math.min(
                                    quantity,
                                    product.getStock()
                            )
                    );
        }

        if (safeQuantity <= 0) {

            selectedQuantities.remove(
                    productId
            );

        } else {

            selectedQuantities.put(
                    productId,
                    safeQuantity
            );
        }

        int displayedPosition =
                findDisplayedProductPosition(
                        productId
                );

        if (displayedPosition >= 0) {

            notifyItemChanged(
                    displayedPosition
            );
        }
    }

    public int getQuantity(
            String productId
    ) {

        if (productId == null) {
            return 0;
        }

        Integer quantity =
                selectedQuantities.get(
                        productId
                );

        return quantity == null
                ? 0
                : quantity;
    }

    public void clearQuantities() {

        selectedQuantities.clear();
        notifyDataSetChanged();
    }

    public Map<String, Integer>
    getSelectedQuantities() {

        return new HashMap<>(
                selectedQuantities
        );
    }

    private Product findProductById(
            String productId
    ) {

        if (productId == null) {
            return null;
        }

        for (Product product : allProducts) {

            if (productId.equals(
                    product.getId()
            )) {

                return product;
            }
        }

        return null;
    }

    private int findDisplayedProductPosition(
            String productId
    ) {

        if (productId == null) {
            return -1;
        }

        for (int position = 0;
             position < displayedProducts.size();
             position++) {

            Product product =
                    displayedProducts.get(position);

            if (productId.equals(
                    product.getId()
            )) {

                return position;
            }
        }

        return -1;
    }

    private void removeQuantitiesForDeletedProducts() {

        List<String> existingProductIds =
                new ArrayList<>();

        for (Product product : allProducts) {

            if (product.getId() != null) {

                existingProductIds.add(
                        product.getId()
                );
            }
        }

        List<String> storedProductIds =
                new ArrayList<>(
                        selectedQuantities.keySet()
                );

        for (String storedProductId
                : storedProductIds) {

            if (!existingProductIds.contains(
                    storedProductId
            )) {

                selectedQuantities.remove(
                        storedProductId
                );
            }
        }
    }

    class ProductViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView textProductName;
        private final TextView textProductCategory;
        private final TextView textProductPrice;
        private final TextView textProductOffer;
        private final TextView textProductStock;
        private final TextView textSelectedSubtotal;
        private final TextView textQuantity;

        private final MaterialButton buttonMinus;
        private final MaterialButton buttonPlus;

        private final QuantityHoldController
                plusHoldController;

        private final QuantityHoldController
                minusHoldController;

        private Product boundProduct;

        ProductViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textProductName =
                    itemView.findViewById(
                            R.id.textProductName
                    );

            textProductCategory =
                    itemView.findViewById(
                            R.id.textProductCategory
                    );

            textProductPrice =
                    itemView.findViewById(
                            R.id.textProductPrice
                    );

            textProductOffer =
                    itemView.findViewById(
                            R.id.textProductOffer
                    );

            textProductStock =
                    itemView.findViewById(
                            R.id.textProductStock
                    );

            textSelectedSubtotal =
                    itemView.findViewById(
                            R.id.textSelectedSubtotal
                    );

            textQuantity =
                    itemView.findViewById(
                            R.id.textQuantity
                    );

            buttonMinus =
                    itemView.findViewById(
                            R.id.buttonMinus
                    );

            buttonPlus =
                    itemView.findViewById(
                            R.id.buttonPlus
                    );

            plusHoldController =
                    new QuantityHoldController(
                            this::increaseQuantity
                    );

            minusHoldController =
                    new QuantityHoldController(
                            this::decreaseQuantity
                    );

            configureQuantityControls();
        }

        void bind(
                Product product
        ) {

            boundProduct = product;

            textProductName.setText(
                    getSafeText(
                            product.getName(),
                            "Unnamed product"
                    )
            );

            textProductCategory.setText(
                    getSafeText(
                            product.getCategory(),
                            "Uncategorised"
                    )
            );

            textProductPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "KSh %,.2f each",
                            product.getSellingPrice()
                    )
            );

            displayProductOffer(product);

            textProductStock.setText(
                    itemView.getContext()
                            .getString(
                                    R.string.stock_available,
                                    product.getStock()
                            )
            );

            int currentQuantity =
                    getQuantity(
                            product.getId()
                    );

            if (currentQuantity
                    > product.getStock()) {

                currentQuantity =
                        Math.max(
                                product.getStock(),
                                0
                        );

                saveQuantity(
                        product,
                        currentQuantity
                );
            }

            updateQuantityDisplay(
                    currentQuantity
            );

            updateSelectedSubtotal(
                    product,
                    currentQuantity
            );

            boolean hasStock =
                    product.getStock() > 0;

            buttonPlus.setEnabled(
                    hasStock
                            && currentQuantity
                            < product.getStock()
            );

            buttonMinus.setEnabled(
                    currentQuantity > 0
            );

            textQuantity.setEnabled(
                    hasStock
            );
        }

        private void displayProductOffer(
                Product product
        ) {

            if (!product.hasValidQuantityOffer()) {

                textProductOffer.setVisibility(
                        View.GONE
                );

                return;
            }

            textProductOffer.setVisibility(
                    View.VISIBLE
            );

            textProductOffer.setText(
                    String.format(
                            Locale.getDefault(),
                            "Offer: %d for KSh %,.2f",
                            product.getOfferQuantity(),
                            product.getOfferPrice()
                    )
            );
        }

        @SuppressLint("ClickableViewAccessibility")
        private void configureQuantityControls() {

            buttonPlus.setOnTouchListener(
                    (view, motionEvent) ->
                            handleHoldTouch(
                                    view,
                                    motionEvent,
                                    plusHoldController
                            )
            );

            buttonMinus.setOnTouchListener(
                    (view, motionEvent) ->
                            handleHoldTouch(
                                    view,
                                    motionEvent,
                                    minusHoldController
                            )
            );

            textQuantity.setOnClickListener(
                    view -> {

                        if (boundProduct == null
                                || listener == null) {

                            return;
                        }

                        listener.onQuantityClicked(
                                boundProduct,
                                getQuantity(
                                        boundProduct.getId()
                                )
                        );
                    }
            );
        }

        private boolean handleHoldTouch(
                View view,
                MotionEvent motionEvent,
                QuantityHoldController controller
        ) {

            if (!view.isEnabled()) {
                return false;
            }

            switch (motionEvent.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:

                    view.setPressed(true);
                    controller.start();

                    return true;

                case MotionEvent.ACTION_UP:

                    controller.stop();
                    view.setPressed(false);
                    view.performClick();

                    return true;

                case MotionEvent.ACTION_CANCEL:

                    controller.stop();
                    view.setPressed(false);

                    return true;

                default:
                    return true;
            }
        }

        private void increaseQuantity() {

            if (boundProduct == null
                    || boundProduct.getId() == null) {

                plusHoldController.stop();
                return;
            }

            int currentQuantity =
                    getQuantity(
                            boundProduct.getId()
                    );

            int availableStock =
                    boundProduct.getStock();

            if (availableStock <= 0) {

                plusHoldController.stop();

                showToast(
                        boundProduct.getName()
                                + " is out of stock."
                );

                return;
            }

            if (currentQuantity
                    >= availableStock) {

                plusHoldController.stop();

                showToast(
                        "Only "
                                + availableStock
                                + " units are available."
                );

                return;
            }

            updateQuantity(
                    currentQuantity + 1
            );
        }

        private void decreaseQuantity() {

            if (boundProduct == null
                    || boundProduct.getId() == null) {

                minusHoldController.stop();
                return;
            }

            int currentQuantity =
                    getQuantity(
                            boundProduct.getId()
                    );

            if (currentQuantity <= 0) {

                minusHoldController.stop();
                return;
            }

            updateQuantity(
                    currentQuantity - 1
            );
        }

        private void updateQuantity(
                int requestedQuantity
        ) {

            if (boundProduct == null
                    || boundProduct.getId() == null) {

                return;
            }

            int safeQuantity =
                    Math.max(
                            0,
                            Math.min(
                                    requestedQuantity,
                                    boundProduct.getStock()
                            )
                    );

            saveQuantity(
                    boundProduct,
                    safeQuantity
            );

            updateQuantityDisplay(
                    safeQuantity
            );

            updateSelectedSubtotal(
                    boundProduct,
                    safeQuantity
            );

            buttonMinus.setEnabled(
                    safeQuantity > 0
            );

            buttonPlus.setEnabled(
                    safeQuantity
                            < boundProduct.getStock()
            );

            if (listener != null) {

                listener.onQuantityChanged(
                        boundProduct,
                        safeQuantity
                );
            }
        }

        private void saveQuantity(
                Product product,
                int quantity
        ) {

            if (product == null
                    || product.getId() == null) {

                return;
            }

            if (quantity <= 0) {

                selectedQuantities.remove(
                        product.getId()
                );

            } else {

                selectedQuantities.put(
                        product.getId(),
                        quantity
                );
            }
        }

        private void updateQuantityDisplay(
                int quantity
        ) {

            textQuantity.setText(
                    String.valueOf(quantity)
            );

            textQuantity.setActivated(
                    quantity > 0
            );
        }

        private void updateSelectedSubtotal(
                Product product,
                int quantity
        ) {

            if (quantity <= 0) {

                textSelectedSubtotal.setVisibility(
                        View.GONE
                );

                return;
            }

            textSelectedSubtotal.setVisibility(
                    View.VISIBLE
            );

            double subtotal =
                    product.calculateSaleTotal(
                            quantity
                    );

            double saving =
                    product.calculateCustomerSaving(
                            quantity
                    );

            if (saving > 0) {

                textSelectedSubtotal.setText(
                        String.format(
                                Locale.getDefault(),
                                "Subtotal: KSh %,.2f • Save KSh %,.2f",
                                subtotal,
                                saving
                        )
                );

            } else {

                textSelectedSubtotal.setText(
                        String.format(
                                Locale.getDefault(),
                                "Subtotal: KSh %,.2f",
                                subtotal
                        )
                );
            }
        }

        private String getSafeText(
                String text,
                String fallback
        ) {

            if (text == null
                    || text.trim().isEmpty()) {

                return fallback;
            }

            return text;
        }

        private void showToast(
                String message
        ) {

            Toast.makeText(
                    itemView.getContext(),
                    message,
                    Toast.LENGTH_SHORT
            ).show();
        }

        void stopHoldActions() {

            plusHoldController.stop();
            minusHoldController.stop();
        }
    }

    @Override
    public void onViewRecycled(
            @NonNull ProductViewHolder holder
    ) {

        holder.stopHoldActions();
        super.onViewRecycled(holder);
    }

    private static class QuantityHoldController {

        private static final long
                INITIAL_HOLD_DELAY_MS = 450L;

        private static final long
                START_REPEAT_DELAY_MS = 220L;

        private static final long
                MINIMUM_REPEAT_DELAY_MS = 65L;

        private static final long
                ACCELERATION_STEP_MS = 18L;

        private final Handler handler =
                new Handler(
                        Looper.getMainLooper()
                );

        private final Runnable quantityAction;

        private boolean holding = false;

        private long repeatDelay =
                START_REPEAT_DELAY_MS;

        QuantityHoldController(
                Runnable quantityAction
        ) {

            this.quantityAction =
                    quantityAction;
        }

        private final Runnable repeatRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (!holding) {
                            return;
                        }

                        quantityAction.run();

                        repeatDelay =
                                Math.max(
                                        MINIMUM_REPEAT_DELAY_MS,
                                        repeatDelay
                                                - ACCELERATION_STEP_MS
                                );

                        handler.postDelayed(
                                this,
                                repeatDelay
                        );
                    }
                };

        void start() {

            stop();

            holding = true;

            repeatDelay =
                    START_REPEAT_DELAY_MS;

            quantityAction.run();

            handler.postDelayed(
                    repeatRunnable,
                    INITIAL_HOLD_DELAY_MS
            );
        }

        void stop() {

            holding = false;

            handler.removeCallbacks(
                    repeatRunnable
            );
        }
    }
}