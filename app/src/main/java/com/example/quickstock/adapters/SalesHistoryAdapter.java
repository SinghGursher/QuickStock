package com.example.quickstock.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickstock.R;
import com.example.quickstock.models.Sale;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SalesHistoryAdapter
        extends ListAdapter<
        Sale,
        SalesHistoryAdapter.SaleViewHolder> {

    public interface OnSaleClickListener {
        void onSaleClick(Sale sale);
    }

    private static final Locale KENYAN_LOCALE =
            new Locale("en", "KE");

    private static final DiffUtil.ItemCallback<Sale>
            SALE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Sale>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Sale oldSale,
                        @NonNull Sale newSale
                ) {
                    String oldId =
                            oldSale.getId();

                    String newId =
                            newSale.getId();

                    if (oldId != null
                            && !oldId.trim().isEmpty()
                            && newId != null
                            && !newId.trim().isEmpty()) {

                        return oldId.equals(
                                newId
                        );
                    }

                    return oldSale == newSale;
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Sale oldSale,
                        @NonNull Sale newSale
                ) {
                    return oldSale.getTimestamp()
                            == newSale.getTimestamp()

                            && Double.compare(
                            oldSale.getTotalAmount(),
                            newSale.getTotalAmount()
                    ) == 0

                            && Double.compare(
                            oldSale.getTotalProfit(),
                            newSale.getTotalProfit()
                    ) == 0

                            && Double.compare(
                            oldSale.getTotalCustomerSaving(),
                            newSale.getTotalCustomerSaving()
                    ) == 0

                            && oldSale.getTotalItems()
                            == newSale.getTotalItems()

                            && getItemCount(
                            oldSale.getItems()
                    ) == getItemCount(
                            newSale.getItems()
                    )

                            && Objects.equals(
                            oldSale.getId(),
                            newSale.getId()
                    );
                }

                private int getItemCount(
                        List<?> items
                ) {
                    return items == null
                            ? 0
                            : items.size();
                }
            };

    private final OnSaleClickListener listener;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "d MMM yyyy, h:mm a",
                    KENYAN_LOCALE
            );

    public SalesHistoryAdapter(
            OnSaleClickListener listener
    ) {
        super(SALE_DIFF_CALLBACK);

        this.listener = listener;
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_sales_history,
                                parent,
                                false
                        );

        return new SaleViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull SaleViewHolder holder,
            int position
    ) {
        Sale sale =
                getItem(position);

        holder.bind(sale);
    }

    private String formatDate(
            long timestamp
    ) {
        if (timestamp <= 0) {
            return "Date unavailable";
        }

        return dateFormat.format(
                new Date(timestamp)
        );
    }

    private String formatMoney(
            double amount
    ) {
        return String.format(
                KENYAN_LOCALE,
                "KSh %,.2f",
                amount
        );
    }

    private String createShortReference(
            Sale sale
    ) {
        if (sale == null
                || sale.getId() == null
                || sale.getId()
                .trim()
                .isEmpty()) {

            return "Sale reference unavailable";
        }

        String cleanId =
                sale.getId()
                        .trim();

        String shortId =
                cleanId.length() > 8
                        ? cleanId.substring(
                        cleanId.length() - 8
                )
                        : cleanId;

        return "Sale #"
                + shortId.toUpperCase(
                Locale.ROOT
        );
    }

    class SaleViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView textSaleReference;
        private final TextView textSaleDate;
        private final TextView textSaleAmount;
        private final TextView textSaleUnits;
        private final TextView textSaleProfit;
        private final TextView textSaleSaving;

        SaleViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textSaleReference =
                    itemView.findViewById(
                            R.id.textSaleReference
                    );

            textSaleDate =
                    itemView.findViewById(
                            R.id.textSaleDate
                    );

            textSaleAmount =
                    itemView.findViewById(
                            R.id.textSaleAmount
                    );

            textSaleUnits =
                    itemView.findViewById(
                            R.id.textSaleUnits
                    );

            textSaleProfit =
                    itemView.findViewById(
                            R.id.textSaleProfit
                    );

            textSaleSaving =
                    itemView.findViewById(
                            R.id.textSaleSaving
                    );
        }

        void bind(
                Sale sale
        ) {
            if (sale == null) {
                return;
            }

            textSaleReference.setText(
                    createShortReference(
                            sale
                    )
            );

            textSaleDate.setText(
                    formatDate(
                            sale.getTimestamp()
                    )
            );

            textSaleAmount.setText(
                    formatMoney(
                            sale.getTotalAmount()
                    )
            );

            int totalUnits =
                    Math.max(
                            sale.getTotalItems(),
                            0
                    );

            textSaleUnits.setText(
                    String.format(
                            KENYAN_LOCALE,
                            totalUnits == 1
                                    ? "%d unit"
                                    : "%d units",
                            totalUnits
                    )
            );

            double totalProfit =
                    sale.getTotalProfit();

            textSaleProfit.setText(
                    String.format(
                            KENYAN_LOCALE,
                            "Profit %s",
                            formatMoney(
                                    totalProfit
                            )
                    )
            );

            int profitColor =
                    totalProfit >= 0
                            ? R.color.primaryGreen
                            : R.color.error;

            textSaleProfit.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            profitColor
                    )
            );

            double customerSaving =
                    sale.getTotalCustomerSaving();

            if (customerSaving > 0) {
                textSaleSaving.setVisibility(
                        View.VISIBLE
                );

                textSaleSaving.setText(
                        String.format(
                                KENYAN_LOCALE,
                                "Customer saved %s",
                                formatMoney(
                                        customerSaving
                                )
                        )
                );

            } else {
                textSaleSaving.setVisibility(
                        View.GONE
                );

                textSaleSaving.setText("");
            }

            itemView.setOnClickListener(
                    view -> {
                        if (listener != null) {
                            listener.onSaleClick(
                                    sale
                            );
                        }
                    }
            );
        }
    }
}