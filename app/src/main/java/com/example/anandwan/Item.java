package com.example.anandwan;

public class Item {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageEncoded; // Base64 String
    private String sellerId;
    private int stock;
    private String category;

    public Item() {
        // Required for Firestore
    }

    public Item(String id, String name, String description, double price, String imageEncoded, String sellerId, int stock, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageEncoded = imageEncoded;
        this.sellerId = sellerId;
        this.stock = stock;
        this.category = category;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getImageEncoded() { return imageEncoded; }
    public void setImageEncoded(String imageEncoded) { this.imageEncoded = imageEncoded; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}