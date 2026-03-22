package dev.dolkisss.orderservice.api;

import dev.dolkisss.api.http.payment.PaymentMethod;

public record OrderPaymentRequest (
        PaymentMethod paymentMethod
) {

}
