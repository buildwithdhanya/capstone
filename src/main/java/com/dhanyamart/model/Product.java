package com.dhanyamart.model;

/**
 * Plain Java Bean (POJO) that represents one product row from products.xlsx.
 */
public class Product {

    private int productId;
    private int sellerId;
    private String name;
    private String category;
    private String description;
    private double price;
    private int stock;
    private String image;
    private String createdAt;

    public Product() {
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isInStock() {
        return stock > 0;
    }

    /** Formats the price like 149.5 -> "149.50" for display in JSPs. */
    public String getPriceText() {
        return String.format("%,.2f", price);
    }

    @Override
    public String toString() {
        return "Product [productId=" + productId + ", name=" + name
                + ", category=" + category + ", price=" + price + ", stock=" + stock + "]";
    }
}