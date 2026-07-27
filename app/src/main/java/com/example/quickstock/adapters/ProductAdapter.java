package com.example.quickstock.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.interfaces.OnProductClickListener;
import com.example.quickstock.models.Product;

import java.util.List;
import java.util.Locale;

public class ProductAdapter
        extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList;
    private final OnProductClickListener listener;

    public ProductAdapter(
            List<Product> productList,
            OnProductClickListener listener
    ) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_product,
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

        Product product = productList.get(position);

        holder.bind(product);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtProductName;
        private final TextView txtCategory;
        private final TextView txtAmount;
        private final TextView txtStock;
        private final TextView txtPrice;

        public ProductViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            txtProductName =
                    itemView.findViewById(R.id.txtProductName);

            txtCategory =
                    itemView.findViewById(R.id.txtCategory);

            txtAmount =
                    itemView.findViewById(R.id.txtAmount);

            txtStock =
                    itemView.findViewById(R.id.txtStock);

            txtPrice =
                    itemView.findViewById(R.id.txtPrice);
        }

        private void bind(Product product) {

            String name = product.getName();

            if (name == null || name.trim().isEmpty()) {
                name = "Unnamed product";
            }

            String category = product.getCategory();

            if (category == null
                    || category.trim().isEmpty()) {
                category = "No category";
            }

            String amount = product.getAmount();

            if (amount == null
                    || amount.trim().isEmpty()) {
                amount = "Amount not specified";
            }

            txtProductName.setText(name);
            txtCategory.setText(category);
            txtAmount.setText(amount);

            txtStock.setText(
                    String.format(
                            Locale.getDefault(),
                            "Stock: %d",
                            product.getStock()
                    )
            );

            txtPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "KSh %.2f",
                            product.getSellingPrice()
                    )
            );
        }
    }
}