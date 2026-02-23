package com.cs6650.scsservice.dto;

import java.util.List;

public class CheckoutRequest {
    private String cartId;
    private String creditCard;   // 传给 CCA 的 cardNumber
    private List<CartItem> items;

    public CheckoutRequest() {}

    // Getter and Setter
    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }

    public String getCreditCard() { return creditCard; }
    public void setCreditCard(String creditCard) { this.creditCard = creditCard; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}
