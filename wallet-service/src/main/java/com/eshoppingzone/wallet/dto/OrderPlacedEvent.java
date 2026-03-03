package com.eshoppingzone.wallet.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event received from RabbitMQ when an order is successfully placed.
 * Must match the OrderPlacedEvent structure from order-service.
 */
public class OrderPlacedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;

    public OrderPlacedEvent() {
    }

    public OrderPlacedEvent(Long orderId, Long userId, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "OrderPlacedEvent{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
