package dev.dolkisss.orderservice.api;

import dev.dolkisss.orderservice.domain.OrderItemEntity;

import java.math.BigDecimal;

/**
 * DTO for {@link OrderItemEntity}
 */
public record OrderItemDto(
        Long id,
        Long itemId,
        Integer quantity,
        BigDecimal priceAtPurchase) {
}