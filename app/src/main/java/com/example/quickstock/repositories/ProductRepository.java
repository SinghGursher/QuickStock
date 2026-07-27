package com.example.quickstock.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.quickstock.firebase.FirebaseClient;
import com.example.quickstock.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private static final String TAG = "ProductRepository";

    public interface OnCompleteListener {
        void onSuccess();

        void onFailure(String error);
    }

    public interface OnProductsLoadedListener {
        void onProductsLoaded(List<Product> products);

        void onFailure(String error);
    }

    public interface OnProductLoadedListener {
        void onProductLoaded(Product product);

        void onFailure(String error);
    }

    /*
     * CREATE PRODUCT
     */
    public void addProduct(
            Product product,
            OnCompleteListener listener
    ) {

        if (product == null) {

            listener.onFailure(
                    "Product information is missing."
            );

            return;
        }

        String productId =
                FirebaseClient.products.push().getKey();

        if (productId == null
                || productId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Failed to generate product ID."
            );

            listener.onFailure(
                    "Unable to generate product ID."
            );

            return;
        }

        /*
         * The product ID must be the same as its Firebase child key.
         */
        product.setId(productId);

        FirebaseClient.products
                .child(productId)
                .setValue(product)
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Product added successfully. ID: "
                                    + productId
                    );

                    listener.onSuccess();
                })
                .addOnFailureListener(exception -> {

                    Log.e(
                            TAG,
                            "Failed to add product.",
                            exception
                    );

                    listener.onFailure(
                            getErrorMessage(
                                    exception.getMessage(),
                                    "Failed to add product."
                            )
                    );
                });
    }

    /*
     * READ ALL PRODUCTS
     *
     * This listener remains active, so inventory and sales screens
     * receive updates whenever Firebase product data changes.
     */
    public void getProducts(
            OnProductsLoadedListener listener
    ) {

        FirebaseClient.products
                .addValueEventListener(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                List<Product> products =
                                        new ArrayList<>();

                                for (DataSnapshot productSnapshot
                                        : snapshot.getChildren()) {

                                    Product product =
                                            productSnapshot.getValue(
                                                    Product.class
                                            );

                                    if (product == null) {

                                        Log.w(
                                                TAG,
                                                "Skipped unreadable product: "
                                                        + productSnapshot.getKey()
                                        );

                                        continue;
                                    }

                                    String firebaseKey =
                                            productSnapshot.getKey();

                                    if (firebaseKey == null
                                            || firebaseKey.trim().isEmpty()) {

                                        Log.w(
                                                TAG,
                                                "Skipped product with missing Firebase key."
                                        );

                                        continue;
                                    }

                                    /*
                                     * Always use the real Firebase child key.
                                     *
                                     * Do not only set it when product.getId()
                                     * is empty. A stored ID may exist but may
                                     * be old or different from the node key.
                                     */
                                    product.setId(firebaseKey);

                                    Log.d(
                                            TAG,
                                            "Loaded product: "
                                                    + product.getName()
                                                    + ", ID: "
                                                    + firebaseKey
                                    );

                                    products.add(product);
                                }

                                Log.d(
                                        TAG,
                                        "Products loaded: "
                                                + products.size()
                                );

                                listener.onProductsLoaded(
                                        products
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Log.e(
                                        TAG,
                                        "Failed to load products.",
                                        error.toException()
                                );

                                listener.onFailure(
                                        getErrorMessage(
                                                error.getMessage(),
                                                "Failed to load products."
                                        )
                                );
                            }
                        }
                );
    }

    /*
     * READ ONE PRODUCT
     */
    public void getProductById(
            String productId,
            OnProductLoadedListener listener
    ) {

        if (productId == null
                || productId.trim().isEmpty()) {

            listener.onFailure(
                    "Invalid product ID."
            );

            return;
        }

        String cleanProductId =
                productId.trim();

        FirebaseClient.products
                .child(cleanProductId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!snapshot.exists()) {

                                    Log.w(
                                            TAG,
                                            "Product not found. ID: "
                                                    + cleanProductId
                                    );

                                    listener.onFailure(
                                            "Product was not found."
                                    );

                                    return;
                                }

                                Product product =
                                        snapshot.getValue(
                                                Product.class
                                        );

                                if (product == null) {

                                    listener.onFailure(
                                            "Unable to read product data."
                                    );

                                    return;
                                }

                                String firebaseKey =
                                        snapshot.getKey();

                                if (firebaseKey == null
                                        || firebaseKey.trim().isEmpty()) {

                                    listener.onFailure(
                                            "Product Firebase key is missing."
                                    );

                                    return;
                                }

                                /*
                                 * Always replace the stored ID with the real
                                 * Firebase child key.
                                 */
                                product.setId(firebaseKey);

                                Log.d(
                                        TAG,
                                        "Product loaded: "
                                                + product.getName()
                                                + ", ID: "
                                                + firebaseKey
                                );

                                listener.onProductLoaded(
                                        product
                                );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Log.e(
                                        TAG,
                                        "Failed to load product.",
                                        error.toException()
                                );

                                listener.onFailure(
                                        getErrorMessage(
                                                error.getMessage(),
                                                "Failed to load product."
                                        )
                                );
                            }
                        }
                );
    }

    /*
     * UPDATE PRODUCT
     */
    public void updateProduct(
            Product product,
            OnCompleteListener listener
    ) {

        if (product == null) {

            listener.onFailure(
                    "Product information is missing."
            );

            return;
        }

        String productId =
                product.getId();

        if (productId == null
                || productId.trim().isEmpty()) {

            listener.onFailure(
                    "Product ID is missing."
            );

            return;
        }

        String cleanProductId =
                productId.trim();

        /*
         * Keep the model's ID identical to the Firebase child key.
         */
        product.setId(cleanProductId);

        FirebaseClient.products
                .child(cleanProductId)
                .setValue(product)
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Product updated successfully. ID: "
                                    + cleanProductId
                    );

                    listener.onSuccess();
                })
                .addOnFailureListener(exception -> {

                    Log.e(
                            TAG,
                            "Failed to update product.",
                            exception
                    );

                    listener.onFailure(
                            getErrorMessage(
                                    exception.getMessage(),
                                    "Failed to update product."
                            )
                    );
                });
    }

    /*
     * DELETE PRODUCT
     */
    public void deleteProduct(
            String productId,
            OnCompleteListener listener
    ) {

        if (productId == null
                || productId.trim().isEmpty()) {

            listener.onFailure(
                    "Invalid product ID."
            );

            return;
        }

        String cleanProductId =
                productId.trim();

        FirebaseClient.products
                .child(cleanProductId)
                .removeValue()
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Product deleted successfully. ID: "
                                    + cleanProductId
                    );

                    listener.onSuccess();
                })
                .addOnFailureListener(exception -> {

                    Log.e(
                            TAG,
                            "Failed to delete product.",
                            exception
                    );

                    listener.onFailure(
                            getErrorMessage(
                                    exception.getMessage(),
                                    "Failed to delete product."
                            )
                    );
                });
    }

    /*
     * Returns a readable Firebase error message.
     */
    private String getErrorMessage(
            String firebaseMessage,
            String defaultMessage
    ) {

        if (firebaseMessage == null
                || firebaseMessage.trim().isEmpty()) {

            return defaultMessage;
        }

        return firebaseMessage;
    }
}