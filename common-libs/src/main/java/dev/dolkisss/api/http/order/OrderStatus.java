package dev.dolkisss.api.http.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    DELIVERY_ASSIGNED,
    DELIVERED,
    PAYMENT_FAILED
}
