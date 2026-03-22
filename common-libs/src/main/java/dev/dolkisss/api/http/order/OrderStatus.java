package dev.dolkisss.api.http.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PENDING_DELIVERY,
    DELIVERED,
    PAYMENT_FAILED
}
