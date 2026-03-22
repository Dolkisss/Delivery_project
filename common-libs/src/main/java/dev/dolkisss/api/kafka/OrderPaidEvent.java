package dev.dolkisss.api.kafka;

import dev.dolkisss.api.http.payment.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderPaidEvent(
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        PaymentMethod paymentMethod
) {

}
