package com.dhanyamart.model;

/**
 * A product together with the quantity in the cart (session-level only,
 * never persisted). Used to render the cart page.
 */
public class CartItem {

    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    public String getSubtotalText() {
        return String.format("%,.2f", getSubtotal());
    }
}