package dev.dolkisss.orderservice.external;

import dev.dolkisss.api.http.payment.CreatePaymentRequestDto;
import dev.dolkisss.api.http.payment.CreatePaymentResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange (
        accept = "application/json",
        contentType = "application/json",
        url = "/api/payments"
)
public interface PaymentHttpClient {

    @PostExchange
    CreatePaymentResponseDto createPayment(@RequestBody CreatePaymentRequestDto request);
}
