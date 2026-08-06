package com.example.quickstock.models;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class SaleItem {

    private String productId;
    private String productName;

    private int quantity;

    /*
     * Normal single-item selling price.
     */
    private double sellingPrice;

    /*
     * Cost of one item.
     */
    private double costPrice;

    /*
     * True when this product has an available quantity offer,
     * even if the current quantity has not reached it yet.
     */
    private boolean quantityOfferAvailable;

    /*
     * True when at least one offer group is currently applied.
     */
    private boolean quantityOfferApplied;

    private int offerQuantity;
    private double offerPrice;

    private int offerGroupsApplied;
    private int normalPriceItems;

    private double subtotal;
    private double normalSubtotal;
    private double customerSaving;
    private double totalCost;
    private double profit;

    /*
     * Required by Firebase.
     */
    public SaleItem() {
    }

    /*
     * Temporary cart constructor.
     *
     * Prefer SaleItem.fromProduct() when adding to the cart.
     */
    public SaleItem(
            String productId,
            String productName,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }

    /*
     * Compatibility constructor.
     */
    public SaleItem(
            String productId,
            String productName,
            int quantity,
            double sellingPrice
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;

        recalculate();
    }

    public SaleItem(
            String productId,
            String productName,
            int quantity,
            double sellingPrice,
            double costPrice,
            boolean quantityOfferAvailable,
            boolean quantityOfferApplied,
            int offerQuantity,
            double offerPrice,
            int offerGroupsApplied,
            int normalPriceItems,
            double subtotal,
            double normalSubtotal,
            double customerSaving,
            double totalCost,
            double profit
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.quantityOfferAvailable =
                quantityOfferAvailable;
        this.quantityOfferApplied =
                quantityOfferApplied;
        this.offerQuantity = offerQuantity;
        this.offerPrice = offerPrice;
        this.offerGroupsApplied =
                offerGroupsApplied;
        this.normalPriceItems = normalPriceItems;
        this.subtotal = subtotal;
        this.normalSubtotal = normalSubtotal;
        this.customerSaving = customerSaving;
        this.totalCost = totalCost;
        this.profit = profit;
    }

    public static SaleItem fromProduct(
            Product product,
            int quantity
    ) {

        if (product == null) {

            throw new IllegalArgumentException(
                    "Product is required."
            );
        }

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        SaleItem item = new SaleItem();

        item.productId = product.getId();
        item.productName = product.getName();
        item.quantity = quantity;

        item.sellingPrice =
                product.getSellingPrice();

        item.costPrice =
                product.getCostPrice();

        item.quantityOfferAvailable =
                product.hasValidQuantityOffer();

        if (item.quantityOfferAvailable) {

            item.offerQuantity =
                    product.getOfferQuantity();

            item.offerPrice =
                    product.getOfferPrice();

        } else {

            item.offerQuantity = 0;
            item.offerPrice = 0;
        }

        item.recalculate();

        return item;
    }

    /**
     * Updates quantity and immediately recalculates:
     *
     * subtotal
     * offers applied
     * saving
     * cost
     * profit
     */
    public void updateQuantityAndRecalculate(
            int newQuantity
    ) {

        if (newQuantity <= 0) {
            return;
        }

        quantity = newQuantity;
        recalculate();
    }

    /**
     * Recalculates all cart financial values using the
     * pricing snapshot stored inside this SaleItem.
     */
    public void recalculate() {

        if (quantity <= 0) {

            quantityOfferApplied = false;
            offerGroupsApplied = 0;
            normalPriceItems = 0;
            subtotal = 0;
            normalSubtotal = 0;
            customerSaving = 0;
            totalCost = 0;
            profit = 0;

            return;
        }

        normalSubtotal =
                sellingPrice * quantity;

        boolean validOffer =
                quantityOfferAvailable
                        && offerQuantity > 1
                        && offerPrice > 0
                        && offerPrice
                        < sellingPrice * offerQuantity;

        if (validOffer) {

            offerGroupsApplied =
                    quantity / offerQuantity;

            normalPriceItems =
                    quantity % offerQuantity;

            quantityOfferApplied =
                    offerGroupsApplied > 0;

            subtotal =
                    (offerGroupsApplied * offerPrice)
                            + (normalPriceItems
                            * sellingPrice);

        } else {

            quantityOfferApplied = false;
            offerGroupsApplied = 0;
            normalPriceItems = quantity;
            subtotal = normalSubtotal;
        }

        customerSaving =
                Math.max(
                        normalSubtotal - subtotal,
                        0
                );

        totalCost =
                costPrice * quantity;

        profit =
                subtotal - totalCost;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    /*
     * Compatibility method for older adapter code.
     */
    @Exclude
    public double getUnitPrice() {
        return sellingPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public boolean isQuantityOfferAvailable() {
        return quantityOfferAvailable;
    }

    public boolean isQuantityOfferApplied() {
        return quantityOfferApplied;
    }

    public int getOfferQuantity() {
        return offerQuantity;
    }

    public double getOfferPrice() {
        return offerPrice;
    }

    public int getOfferGroupsApplied() {
        return offerGroupsApplied;
    }

    public int getNormalPriceItems() {
        return normalPriceItems;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getNormalSubtotal() {
        return normalSubtotal;
    }

    public double getCustomerSaving() {
        return customerSaving;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getProfit() {
        return profit;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setProductName(
            String productName
    ) {
        this.productName = productName;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculate();
    }

    public void setSellingPrice(
            double sellingPrice
    ) {
        this.sellingPrice = sellingPrice;
        recalculate();
    }

    public void setCostPrice(
            double costPrice
    ) {
        this.costPrice = costPrice;
        recalculate();
    }

    public void setQuantityOfferAvailable(
            boolean quantityOfferAvailable
    ) {
        this.quantityOfferAvailable =
                quantityOfferAvailable;

        recalculate();
    }

    public void setQuantityOfferApplied(
            boolean quantityOfferApplied
    ) {
        this.quantityOfferApplied =
                quantityOfferApplied;
    }

    public void setOfferQuantity(
            int offerQuantity
    ) {
        this.offerQuantity = offerQuantity;
        recalculate();
    }

    public void setOfferPrice(
            double offerPrice
    ) {
        this.offerPrice = offerPrice;
        recalculate();
    }

    public void setOfferGroupsApplied(
            int offerGroupsApplied
    ) {
        this.offerGroupsApplied =
                offerGroupsApplied;
    }

    public void setNormalPriceItems(
            int normalPriceItems
    ) {
        this.normalPriceItems = normalPriceItems;
    }

    public void setSubtotal(
            double subtotal
    ) {
        this.subtotal = subtotal;
    }

    public void setNormalSubtotal(
            double normalSubtotal
    ) {
        this.normalSubtotal = normalSubtotal;
    }

    public void setCustomerSaving(
            double customerSaving
    ) {
        this.customerSaving = customerSaving;
    }

    public void setTotalCost(
            double totalCost
    ) {
        this.totalCost = totalCost;
    }

    public void setProfit(
            double profit
    ) {
        this.profit = profit;
    }
}