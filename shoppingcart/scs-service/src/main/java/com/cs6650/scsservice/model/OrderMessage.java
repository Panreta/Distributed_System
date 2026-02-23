package com.cs6650.scsservice.model;

import com.cs6650.scsservice.dto.CartItem;
import java.util.List;

public class OrderMessage {
    private String orderId;
    private String cartId;
    private List<CartItem> items;

    // no-args construct for Jackson
    public OrderMessage() {}

    public OrderMessage(String orderId, String cartId, List<CartItem> items) {
        this.orderId = orderId;
        this.cartId = cartId;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}
