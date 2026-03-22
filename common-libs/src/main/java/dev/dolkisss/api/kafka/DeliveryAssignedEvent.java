package dev.dolkisss.api.kafka;

import lombok.Builder;

@Builder
public record DeliveryAssignedEvent(
        Long orderId,
        String courierName,
        Integer etaMinutes
) {

}
