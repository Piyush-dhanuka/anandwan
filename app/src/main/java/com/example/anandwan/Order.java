package com.example.anandwan;

import com.google.firebase.Timestamp;

public class Order {
    private String orderId;
    private String itemId;
    private String itemName;
    private String buyerName;
    private int quantity;
    private double totalPrice;
    private String sellerId;
    private Timestamp timestamp;

    public Order() {}

    public Order(String orderId, String itemId, String itemName, String buyerName, int quantity, double totalPrice, String sellerId, Timestamp timestamp) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.buyerName = buyerName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.sellerId = sellerId;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getBuyerName() { return buyerName; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public String getSellerId() { return sellerId; }
    public Timestamp getTimestamp() { return timestamp; }
}