package com.example.quickstock.repositories;

import androidx.annotation.NonNull;

import com.example.quickstock.firebase.FirebaseClient;
import com.example.quickstock.models.Product;
import com.example.quickstock.models.Sale;
import com.example.quickstock.models.SaleItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaleRepository {

    public interface OnSaleCompleteListener {

        void onSuccess(String saleId);

        void onFailure(String error);
    }

    public interface OnSalesLoadedListener {

        void onSalesLoaded(List<Sale> sales);

        void onFailure(String error);
    }

    public SaleRepository() {
    }

    public void completeSale(
            List<SaleItem> cartItems,
            OnSaleCompleteListener listener
    ) {

        if (listener == null) {
            return;
        }

        if (cartItems == null
                || cartItems.isEmpty()) {

            listener.onFailure(
                    "Add at least one product to the sale."
            );

            return;
        }

        String validationError =
                validateCart(cartItems);

        if (validationError != null) {

            listener.onFailure(validationError);
            return;
        }

        String saleId =
                FirebaseClient.sales
                        .push()
                        .getKey();

        if (saleId == null
                || saleId.trim().isEmpty()) {

            listener.onFailure(
                    "Could not generate a sale ID."
            );

            return;
        }

        /*
         * Read all current products immediately before checkout.
         *
         * This ensures that the sale uses the latest:
         * - stock
         * - cost price
         * - normal selling price
         * - quantity offer
         */
        FirebaseClient.products
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                prepareAndSaveSale(
                                        saleId,
                                        cartItems,
                                        snapshot,
                                        listener
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                listener.onFailure(
                                        getErrorMessage(
                                                error.getMessage(),
                                                "Products could not be loaded."
                                        )
                                );
                            }
                        }
                );
    }

    private void prepareAndSaveSale(
            String saleId,
            List<SaleItem> cartItems,
            DataSnapshot productsSnapshot,
            OnSaleCompleteListener listener
    ) {

        List<SaleItem> savedItems =
                new ArrayList<>();

        Map<String, Object> updates =
                new HashMap<>();

        for (SaleItem cartItem : cartItems) {

            String productId =
                    cartItem.getProductId();

            DataSnapshot productSnapshot =
                    productsSnapshot.child(
                            productId
                    );

            if (!productSnapshot.exists()) {

                listener.onFailure(
                        cartItem.getProductName()
                                + " could not be found."
                );

                return;
            }

            Product product =
                    productSnapshot.getValue(
                            Product.class
                    );

            if (product == null) {

                listener.onFailure(
                        "Could not read product information for "
                                + cartItem.getProductName()
                                + "."
                );

                return;
            }

            product.setId(
                    productSnapshot.getKey()
            );

            normaliseOlderProduct(product);

            int requestedQuantity =
                    cartItem.getQuantity();

            if (requestedQuantity <= 0) {

                listener.onFailure(
                        "Invalid quantity for "
                                + product.getName()
                                + "."
                );

                return;
            }

            if (product.getStock() <= 0) {

                listener.onFailure(
                        product.getName()
                                + " is out of stock."
                );

                return;
            }

            if (requestedQuantity
                    > product.getStock()) {

                listener.onFailure(
                        "Only "
                                + product.getStock()
                                + " units of "
                                + product.getName()
                                + " are available."
                );

                return;
            }

            if (product.getCostPrice() <= 0) {

                listener.onFailure(
                        product.getName()
                                + " does not have a valid cost price."
                );

                return;
            }

            if (product.getSellingPrice() <= 0) {

                listener.onFailure(
                        product.getName()
                                + " does not have a valid selling price."
                );

                return;
            }

            /*
             * Ensure the normal price is not below cost.
             */
            if (product.getSellingPrice()
                    < product.getCostPrice()) {

                listener.onFailure(
                        product.getName()
                                + " has a selling price below its cost price."
                );

                return;
            }

            /*
             * Ensure an enabled offer does not sell below cost.
             */
            if (product.hasValidQuantityOffer()) {

                double offerCost =
                        product.getCostPrice()
                                * product.getOfferQuantity();

                if (product.getOfferPrice()
                        < offerCost) {

                    listener.onFailure(
                            product.getName()
                                    + " has an offer price below its total cost."
                    );

                    return;
                }
            }

            /*
             * This automatically applies the saved quantity offer.
             */
            SaleItem savedItem =
                    SaleItem.fromProduct(
                            product,
                            requestedQuantity
                    );

            savedItems.add(savedItem);

            int remainingStock =
                    product.getStock()
                            - requestedQuantity;

            /*
             * Update only the stock field.
             */
            updates.put(
                    "products/"
                            + productId
                            + "/stock",
                    remainingStock
            );
        }

        /*
         * Sale calculates:
         * - totalAmount
         * - totalCost
         * - totalProfit
         * - totalCustomerSaving
         * - totalItems
         */
        Sale sale =
                new Sale(
                        saleId,
                        System.currentTimeMillis(),
                        savedItems
                );

        updates.put(
                "sales/" + saleId,
                sale
        );

        DatabaseReference rootReference =
                FirebaseClient.products
                        .getRoot();

        /*
         * Stock reductions and sale saving happen together.
         */
        rootReference
                .updateChildren(updates)
                .addOnSuccessListener(
                        unused ->
                                listener.onSuccess(
                                        saleId
                                )
                )
                .addOnFailureListener(
                        exception ->
                                listener.onFailure(
                                        getErrorMessage(
                                                exception.getMessage(),
                                                "The sale could not be saved."
                                        )
                                )
                );
    }

    /*
     * Allows old Firebase products to be read safely.
     */
    private void normaliseOlderProduct(
            Product product
    ) {

        if (product.getPurchaseUnit() == null
                || product.getPurchaseUnit()
                .trim()
                .isEmpty()) {

            product.setPurchaseUnit("Unit");
        }

        if (product.getUnitsPerPurchase() <= 0) {

            product.setUnitsPerPurchase(1);
        }

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

    private String validateCart(
            List<SaleItem> cartItems
    ) {

        for (SaleItem item : cartItems) {

            if (item == null) {

                return "The cart contains an invalid item.";
            }

            if (item.getProductId() == null
                    || item.getProductId()
                    .trim()
                    .isEmpty()) {

                return "A cart item has no product ID.";
            }

            if (item.getQuantity() <= 0) {

                return "Invalid quantity for "
                        + safeProductName(item)
                        + ".";
            }
        }

        return null;
    }

    private String safeProductName(
            SaleItem item
    ) {

        if (item.getProductName() == null
                || item.getProductName()
                .trim()
                .isEmpty()) {

            return "product";
        }

        return item.getProductName();
    }

    public void getSales(
            OnSalesLoadedListener listener
    ) {

        if (listener == null) {
            return;
        }

        FirebaseClient.sales
                .addValueEventListener(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                List<Sale> sales =
                                        new ArrayList<>();

                                for (DataSnapshot saleSnapshot
                                        : snapshot.getChildren()) {

                                    Sale sale =
                                            saleSnapshot.getValue(
                                                    Sale.class
                                            );

                                    if (sale == null) {
                                        continue;
                                    }

                                    if (sale.getId() == null
                                            || sale.getId()
                                            .trim()
                                            .isEmpty()) {

                                        sale.setId(
                                                saleSnapshot.getKey()
                                        );
                                    }

                                    if (sale.getItems() == null) {

                                        sale.setItems(
                                                new ArrayList<>()
                                        );
                                    }

                                    sales.add(sale);
                                }

                                listener.onSalesLoaded(sales);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                listener.onFailure(
                                        getErrorMessage(
                                                error.getMessage(),
                                                "Sales could not be loaded."
                                        )
                                );
                            }
                        }
                );
    }

    private String getErrorMessage(
            String message,
            String defaultMessage
    ) {

        if (message == null
                || message.trim().isEmpty()) {

            return defaultMessage;
        }

        return message;
    }
}