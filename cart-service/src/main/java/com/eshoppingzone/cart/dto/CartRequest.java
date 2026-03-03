package com.eshoppingzone.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CartRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemDto> items;

    public CartRequest() {
    }

    public CartRequest(Long userId, List<CartItemDto> items) {
        this.userId = userId;
        this.items = items;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "CartRequest{" +
                "userId=" + userId +
                ", items=" + items +
                '}';
    }
}
