package com.example.quickstock.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.models.SaleItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CartAdapter
        extends RecyclerView.Adapter<
        CartAdapter.CartViewHolder> {

    public interface CartActionListener {

        void onIncrease(
                SaleItem saleItem
        );

        void onDecrease(
                SaleItem saleItem
        );

        void onRemove(
                SaleItem saleItem
        );
    }

    private final List<SaleItem> cartItems;
    private final CartActionListener listener;

    public CartAdapter(
            List<SaleItem> cartItems,
            CartActionListener listener
    ) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_cart,
                                parent,
                                false
                        );

        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CartViewHolder holder,
            int position
    ) {

        SaleItem saleItem =
                cartItems.get(position);

        /*
         * Ensures subtotal, saving and offer groups
         * match the current quantity.
         */
        saleItem.recalculate();

        holder.bind(saleItem);

        holder.buttonIncrease.setOnClickListener(
                view -> {

                    int adapterPosition =
                            holder
                                    .getBindingAdapterPosition();

                    if (adapterPosition
                            == RecyclerView.NO_POSITION) {

                        return;
                    }

                    if (listener != null) {

                        listener.onIncrease(
                                cartItems.get(
                                        adapterPosition
                                )
                        );
                    }
                }
        );

        holder.buttonDecrease.setOnClickListener(
                view -> {

                    int adapterPosition =
                            holder
                                    .getBindingAdapterPosition();

                    if (adapterPosition
                            == RecyclerView.NO_POSITION) {

                        return;
                    }

                    if (listener != null) {

                        listener.onDecrease(
                                cartItems.get(
                                        adapterPosition
                                )
                        );
                    }
                }
        );

        holder.buttonRemove.setOnClickListener(
                view -> {

                    int adapterPosition =
                            holder
                                    .getBindingAdapterPosition();

                    if (adapterPosition
                            == RecyclerView.NO_POSITION) {

                        return;
                    }

                    if (listener != null) {

                        listener.onRemove(
                                cartItems.get(
                                        adapterPosition
                                )
                        );
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void refreshItem(
            SaleItem saleItem
    ) {

        if (saleItem == null) {
            return;
        }

        int position =
                cartItems.indexOf(
                        saleItem
                );

        if (position >= 0) {

            saleItem.recalculate();
            notifyItemChanged(position);
        }
    }

    public void refreshAll() {

        for (SaleItem item : cartItems) {

            if (item != null) {
                item.recalculate();
            }
        }

        notifyDataSetChanged();
    }

    static class CartViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView
                textCartProductName;

        private final TextView
                textCartUnitPrice;

        private final TextView
                textCartOffer;

        private final TextView
                textCartQuantity;

        private final TextView
                textCartSubtotal;

        private final TextView
                textCartSaving;

        private final MaterialButton
                buttonDecrease;

        private final MaterialButton
                buttonIncrease;

        private final MaterialButton
                buttonRemove;

        CartViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textCartProductName =
                    itemView.findViewById(
                            R.id.textCartProductName
                    );

            textCartUnitPrice =
                    itemView.findViewById(
                            R.id.textCartUnitPrice
                    );

            textCartOffer =
                    itemView.findViewById(
                            R.id.textCartOffer
                    );

            textCartQuantity =
                    itemView.findViewById(
                            R.id.textCartQuantity
                    );

            textCartSubtotal =
                    itemView.findViewById(
                            R.id.textCartSubtotal
                    );

            textCartSaving =
                    itemView.findViewById(
                            R.id.textCartSaving
                    );

            buttonDecrease =
                    itemView.findViewById(
                            R.id.buttonDecrease
                    );

            buttonIncrease =
                    itemView.findViewById(
                            R.id.buttonIncrease
                    );

            buttonRemove =
                    itemView.findViewById(
                            R.id.buttonRemoveCartItem
                    );
        }

        private void bind(
                SaleItem saleItem
        ) {

            textCartProductName.setText(
                    getSafeText(
                            saleItem.getProductName(),
                            "Unnamed product"
                    )
            );

            textCartUnitPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "KSh %,.2f each",
                            saleItem.getSellingPrice()
                    )
            );

            textCartQuantity.setText(
                    String.valueOf(
                            saleItem.getQuantity()
                    )
            );

            textCartSubtotal.setText(
                    String.format(
                            Locale.getDefault(),
                            "Subtotal: KSh %,.2f",
                            saleItem.getSubtotal()
                    )
            );

            displayOfferInformation(
                    saleItem
            );

            displaySaving(
                    saleItem
            );
        }

        private void displayOfferInformation(
                SaleItem saleItem
        ) {

            if (!saleItem
                    .isQuantityOfferAvailable()) {

                textCartOffer.setVisibility(
                        View.GONE
                );

                return;
            }

            textCartOffer.setVisibility(
                    View.VISIBLE
            );

            if (!saleItem
                    .isQuantityOfferApplied()) {

                int remaining =
                        saleItem.getOfferQuantity()
                                - saleItem.getQuantity();

                if (remaining > 0) {

                    textCartOffer.setText(
                            String.format(
                                    Locale.getDefault(),
                                    "Offer: %d for KSh %,.2f • Add %d more",
                                    saleItem.getOfferQuantity(),
                                    saleItem.getOfferPrice(),
                                    remaining
                            )
                    );

                } else {

                    textCartOffer.setText(
                            String.format(
                                    Locale.getDefault(),
                                    "Offer: %d for KSh %,.2f",
                                    saleItem.getOfferQuantity(),
                                    saleItem.getOfferPrice()
                            )
                    );
                }

                return;
            }

            String offerText =
                    saleItem.getOfferGroupsApplied()
                            + (
                            saleItem
                                    .getOfferGroupsApplied()
                                    == 1
                                    ? " offer applied"
                                    : " offers applied"
                    );

            if (saleItem.getNormalPriceItems()
                    > 0) {

                offerText += ", "
                        + saleItem
                        .getNormalPriceItems()
                        + (
                        saleItem
                                .getNormalPriceItems()
                                == 1
                                ? " item at normal price"
                                : " items at normal price"
                );
            }

            textCartOffer.setText(
                    offerText
            );
        }

        private void displaySaving(
                SaleItem saleItem
        ) {

            if (saleItem.getCustomerSaving()
                    <= 0) {

                textCartSaving.setVisibility(
                        View.GONE
                );

                return;
            }

            textCartSaving.setVisibility(
                    View.VISIBLE
            );

            textCartSaving.setText(
                    String.format(
                            Locale.getDefault(),
                            "Customer saves KSh %,.2f",
                            saleItem.getCustomerSaving()
                    )
            );
        }

        private String getSafeText(
                String value,
                String fallback
        ) {

            if (value == null
                    || value.trim().isEmpty()) {

                return fallback;
            }

            return value;
        }
    }
}