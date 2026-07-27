package com.example.quickstock.models;

import java.util.ArrayList;
import java.util.List;

public class Sale {

    private String id;
    private long timestamp;

    private double totalAmount;
    private double totalCost;
    private double totalProfit;
    private double totalCustomerSaving;

    private int totalItems;

    private List<SaleItem> items;

    /*
     * Required by Firebase.
     */
    public Sale() {
    }

    public Sale(
            String id,
            long timestamp,
            double totalAmount,
            double totalCost,
            double totalProfit,
            double totalCustomerSaving,
            int totalItems,
            List<SaleItem> items
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.totalAmount = totalAmount;
        this.totalCost = totalCost;
        this.totalProfit = totalProfit;
        this.totalCustomerSaving =
                totalCustomerSaving;
        this.totalItems = totalItems;
        this.items = items;
    }

    /*
     * Convenient constructor that calculates all totals.
     */
    public Sale(
            String id,
            long timestamp,
            List<SaleItem> items
    ) {
        this.id = id;
        this.timestamp = timestamp;

        this.items =
                items == null
                        ? new ArrayList<>()
                        : items;

        recalculateTotals();
    }

    public void recalculateTotals() {

        totalAmount = 0;
        totalCost = 0;
        totalProfit = 0;
        totalCustomerSaving = 0;
        totalItems = 0;

        if (items == null) {
            items = new ArrayList<>();
            return;
        }

        for (SaleItem item : items) {

            if (item == null) {
                continue;
            }

            totalAmount += item.getSubtotal();
            totalCost += item.getTotalCost();
            totalProfit += item.getProfit();

            totalCustomerSaving +=
                    item.getCustomerSaving();

            totalItems += item.getQuantity();
        }
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public double getTotalCustomerSaving() {
        return totalCustomerSaving;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTotalAmount(
            double totalAmount
    ) {
        this.totalAmount = totalAmount;
    }

    public void setTotalCost(
            double totalCost
    ) {
        this.totalCost = totalCost;
    }

    public void setTotalProfit(
            double totalProfit
    ) {
        this.totalProfit = totalProfit;
    }

    public void setTotalCustomerSaving(
            double totalCustomerSaving
    ) {
        this.totalCustomerSaving =
                totalCustomerSaving;
    }

    public void setTotalItems(
            int totalItems
    ) {
        this.totalItems = totalItems;
    }

    public void setItems(
            List<SaleItem> items
    ) {
        this.items = items;
    }
}