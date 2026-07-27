package com.example.quickstock.models;

public class Product {

    private String id;
    private String name;
    private String category;
    private String amount;

    /*
     * Cost of one individual selling unit.
     */
    private double costPrice;

    /*
     * Normal selling price of one individual item.
     */
    private double sellingPrice;

    /*
     * Price paid for the complete carton, box, crate, etc.
     */
    private double purchaseUnitPrice;

    /*
     * Stock is stored as individual selling units.
     */
    private int stock;

    private String purchaseUnit;
    private int unitsPerPurchase;

    /*
     * Quantity offer information.
     *
     * Example:
     * 3 items for KSh 10.
     */
    private boolean quantityOfferEnabled;
    private int offerQuantity;
    private double offerPrice;

    /*
     * Required by Firebase.
     */
    public Product() {
    }

    public Product(
            String id,
            String name,
            String category,
            String amount,
            double costPrice,
            double sellingPrice,
            double purchaseUnitPrice,
            int stock,
            String purchaseUnit,
            int unitsPerPurchase,
            boolean quantityOfferEnabled,
            int offerQuantity,
            double offerPrice
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.purchaseUnitPrice = purchaseUnitPrice;
        this.stock = stock;
        this.purchaseUnit = purchaseUnit;
        this.unitsPerPurchase = unitsPerPurchase;
        this.quantityOfferEnabled = quantityOfferEnabled;
        this.offerQuantity = offerQuantity;
        this.offerPrice = offerPrice;
    }

    /*
     * Compatibility constructor without a quantity offer.
     */
    public Product(
            String id,
            String name,
            String category,
            String amount,
            double costPrice,
            double sellingPrice,
            double purchaseUnitPrice,
            int stock,
            String purchaseUnit,
            int unitsPerPurchase
    ) {
        this(
                id,
                name,
                category,
                amount,
                costPrice,
                sellingPrice,
                purchaseUnitPrice,
                stock,
                purchaseUnit,
                unitsPerPurchase,
                false,
                0,
                0
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAmount() {
        return amount;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public double getPurchaseUnitPrice() {
        return purchaseUnitPrice;
    }

    public int getStock() {
        return stock;
    }

    public String getPurchaseUnit() {
        return purchaseUnit;
    }

    public int getUnitsPerPurchase() {
        return unitsPerPurchase;
    }

    public boolean isQuantityOfferEnabled() {
        return quantityOfferEnabled;
    }

    public int getOfferQuantity() {
        return offerQuantity;
    }

    public double getOfferPrice() {
        return offerPrice;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setPurchaseUnitPrice(
            double purchaseUnitPrice
    ) {
        this.purchaseUnitPrice = purchaseUnitPrice;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setPurchaseUnit(String purchaseUnit) {
        this.purchaseUnit = purchaseUnit;
    }

    public void setUnitsPerPurchase(
            int unitsPerPurchase
    ) {
        this.unitsPerPurchase = unitsPerPurchase;
    }

    public void setQuantityOfferEnabled(
            boolean quantityOfferEnabled
    ) {
        this.quantityOfferEnabled =
                quantityOfferEnabled;
    }

    public void setOfferQuantity(int offerQuantity) {
        this.offerQuantity = offerQuantity;
    }

    public void setOfferPrice(double offerPrice) {
        this.offerPrice = offerPrice;
    }

    public void addStock(int quantity) {

        if (quantity > 0) {
            stock += quantity;
        }
    }

    public boolean reduceStock(int quantity) {

        if (quantity <= 0 || quantity > stock) {
            return false;
        }

        stock -= quantity;
        return true;
    }

    public boolean isLowStock() {
        return stock <= 5;
    }

    public boolean usesAdvancedPurchaseUnit() {

        return purchaseUnit != null
                && !purchaseUnit.trim().isEmpty()
                && (
                !purchaseUnit.equalsIgnoreCase("Unit")
                        || unitsPerPurchase > 1
        );
    }

    public boolean hasValidQuantityOffer() {

        if (!quantityOfferEnabled) {
            return false;
        }

        if (offerQuantity <= 1
                || offerPrice <= 0
                || sellingPrice <= 0) {

            return false;
        }

        return offerPrice
                < sellingPrice * offerQuantity;
    }

    public double calculateSaleTotal(int quantity) {

        if (quantity <= 0) {
            return 0;
        }

        if (!hasValidQuantityOffer()) {
            return sellingPrice * quantity;
        }

        int offerGroups =
                quantity / offerQuantity;

        int remainingItems =
                quantity % offerQuantity;

        return (offerGroups * offerPrice)
                + (remainingItems * sellingPrice);
    }

    public int calculateOfferGroups(int quantity) {

        if (!hasValidQuantityOffer()
                || quantity < offerQuantity) {

            return 0;
        }

        return quantity / offerQuantity;
    }

    public int calculateRemainingNormalItems(
            int quantity
    ) {

        if (quantity <= 0) {
            return 0;
        }

        if (!hasValidQuantityOffer()) {
            return quantity;
        }

        return quantity % offerQuantity;
    }

    public double calculateNormalTotal(
            int quantity
    ) {

        if (quantity <= 0) {
            return 0;
        }

        return sellingPrice * quantity;
    }

    public double calculateCustomerSaving(
            int quantity
    ) {

        return Math.max(
                calculateNormalTotal(quantity)
                        - calculateSaleTotal(quantity),
                0
        );
    }

    public double calculateTotalCost(
            int quantity
    ) {

        if (quantity <= 0) {
            return 0;
        }

        return costPrice * quantity;
    }

    public double calculateProfit(int quantity) {

        return calculateSaleTotal(quantity)
                - calculateTotalCost(quantity);
    }

    public double getProfitPerUnit() {
        return sellingPrice - costPrice;
    }

    public double calculateOfferProfit() {

        if (!hasValidQuantityOffer()) {
            return 0;
        }

        return offerPrice
                - (costPrice * offerQuantity);
    }

    public double calculateCostPerUnit() {

        if (purchaseUnitPrice <= 0
                || unitsPerPurchase <= 0) {

            return costPrice;
        }

        return purchaseUnitPrice
                / unitsPerPurchase;
    }

    @Override
    public String toString() {

        if (amount == null
                || amount.trim().isEmpty()) {

            return name;
        }

        return name + " (" + amount + ")";
    }
}