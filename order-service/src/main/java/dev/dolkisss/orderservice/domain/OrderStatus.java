package dev.dolkisss.orderservice.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PENDING_DELIVERY,
    DELIVERED,
    PAYMENT_FAILED
}
