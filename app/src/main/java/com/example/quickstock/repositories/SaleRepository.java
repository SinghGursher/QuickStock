package com.example.quickstock.repositories;

import android.util.Log;

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

    private static final String TAG =
            "SaleRepository";

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

        String userId =
                FirebaseClient.getCurrentUserId();

        DatabaseReference productsReference =
                FirebaseClient.getProductsReference();

        DatabaseReference salesReference =
                FirebaseClient.getSalesReference();

        if (userId == null
                || userId.trim().isEmpty()
                || productsReference == null
                || salesReference == null) {

            listener.onFailure(
                    "User is not logged in."
            );
            return;
        }

        String saleId =
                salesReference.push().getKey();

        if (saleId == null
                || saleId.trim().isEmpty()) {
            listener.onFailure(
                    "Could not generate a sale ID."
            );
            return;
        }

        productsReference
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {
                                prepareAndSaveSale(
                                        userId,
                                        saleId,
                                        cartItems,
                                        snapshot,
                                        productsReference,
                                        listener
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                Log.e(
                                        TAG,
                                        "Products could not be loaded.",
                                        error.toException()
                                );

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
            String userId,
            String saleId,
            List<SaleItem> cartItems,
            DataSnapshot productsSnapshot,
            DatabaseReference productsReference,
            OnSaleCompleteListener listener
    ) {
        List<SaleItem> savedItems =
                new ArrayList<>();

        Map<String, Object> updates =
                new HashMap<>();

        for (SaleItem cartItem : cartItems) {
            if (cartItem == null) {
                listener.onFailure(
                        "The cart contains an invalid item."
                );
                return;
            }

            String productId =
                    cartItem.getProductId();

            if (productId == null
                    || productId.trim().isEmpty()) {
                listener.onFailure(
                        "A cart item has no product ID."
                );
                return;
            }

            String cleanProductId =
                    productId.trim();

            DataSnapshot productSnapshot =
                    productsSnapshot.child(
                            cleanProductId
                    );

            if (!productSnapshot.exists()) {
                listener.onFailure(
                        safeProductName(cartItem)
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
                                + safeProductName(cartItem)
                                + "."
                );
                return;
            }

            product.setId(cleanProductId);

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

            if (product.getSellingPrice()
                    < product.getCostPrice()) {
                listener.onFailure(
                        product.getName()
                                + " has a selling price below its cost price."
                );
                return;
            }

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
             * Full atomic path:
             * products/{uid}/{productId}/stock
             */
            updates.put(
                    "products/"
                            + userId
                            + "/"
                            + cleanProductId
                            + "/stock",
                    remainingStock
            );
        }

        Sale sale =
                new Sale(
                        saleId,
                        System.currentTimeMillis(),
                        savedItems
                );

        /*
         * Full atomic path:
         * sales/{uid}/{saleId}
         */
        updates.put(
                "sales/"
                        + userId
                        + "/"
                        + saleId,
                sale
        );

        DatabaseReference rootReference =
                productsReference.getRoot();

        rootReference
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(
                            TAG,
                            "Sale completed successfully. ID: "
                                    + saleId
                    );

                    listener.onSuccess(saleId);
                })
                .addOnFailureListener(exception -> {
                    Log.e(
                            TAG,
                            "The sale could not be saved.",
                            exception
                    );

                    listener.onFailure(
                            getErrorMessage(
                                    exception.getMessage(),
                                    "The sale could not be saved."
                            )
                    );
                });
    }

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
        if (item == null
                || item.getProductName() == null
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

        DatabaseReference salesReference =
                FirebaseClient.getSalesReference();

        if (salesReference == null) {
            listener.onFailure(
                    "User is not logged in."
            );
            return;
        }

        /*
         * One-time read prevents repeated active listeners
         * each time the dashboard resumes.
         */
        salesReference
                .addListenerForSingleValueEvent(
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

                                    String firebaseKey =
                                            saleSnapshot.getKey();

                                    if (firebaseKey != null
                                            && !firebaseKey
                                            .trim()
                                            .isEmpty()) {
                                        sale.setId(firebaseKey);
                                    }

                                    if (sale.getItems() == null) {
                                        sale.setItems(
                                                new ArrayList<>()
                                        );
                                    }

                                    sales.add(sale);
                                }

                                Log.d(
                                        TAG,
                                        "Sales loaded: "
                                                + sales.size()
                                );

                                listener.onSalesLoaded(sales);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                Log.e(
                                        TAG,
                                        "Sales could not be loaded.",
                                        error.toException()
                                );

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