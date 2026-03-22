package dev.dolkisss.paymentservice.api;

import dev.dolkisss.api.http.payment.CreatePaymentRequestDto;
import dev.dolkisss.api.http.payment.CreatePaymentResponseDto;
import dev.dolkisss.paymentservice.domain.db.PaymentEntityMapper;
import dev.dolkisss.paymentservice.domain.db.PaymentEntityRepository;
import dev.dolkisss.paymentservice.domain.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@AllArgsConstructor
public class PaymentController {

    private final PaymentEntityRepository repository;
    private final PaymentEntityMapper mapper;
    private final PaymentService paymentService;

    @PostMapping
    public CreatePaymentResponseDto createPayment (
            @RequestBody CreatePaymentRequestDto request
    ) {
        log.info("Received request: paymentRequest={}", request);

        return paymentService.makePayment(request);
    }

}
