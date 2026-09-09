package dev.dolkisss.paymentservice;

import dev.dolkisss.api.http.payment.CreatePaymentRequestDto;
import dev.dolkisss.api.http.payment.CreatePaymentResponseDto;
import dev.dolkisss.api.http.payment.PaymentMethod;
import dev.dolkisss.api.http.payment.PaymentStatus;
import dev.dolkisss.paymentservice.domain.PaymentService;
import dev.dolkisss.paymentservice.domain.db.PaymentEntity;
import dev.dolkisss.paymentservice.domain.db.PaymentEntityMapper;
import dev.dolkisss.paymentservice.domain.db.PaymentEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceApplicationTests {

    @Mock
    private PaymentEntityRepository repository;
    @Mock
    private PaymentEntityMapper mapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void makePaymentShouldReturnExistingPaymentWithoutCreatingAnotherOne() {
        var request = paymentRequest(PaymentMethod.CARD);
        var existingPayment = paymentEntity(
                77L,
                PaymentMethod.CARD,
                PaymentStatus.PAYMENT_SUCCEEDED
        );
        var expectedResponse = paymentResponse(
                77L,
                PaymentMethod.CARD,
                PaymentStatus.PAYMENT_SUCCEEDED
        );

        when(repository.findByOrderId(request.orderId()))
                .thenReturn(Optional.of(existingPayment));
        when(mapper.toResponseDto(existingPayment)).thenReturn(expectedResponse);

        var result = paymentService.makePayment(request);

        assertSame(expectedResponse, result);
        verify(mapper).toResponseDto(existingPayment);
        verify(mapper, never()).toEntity(request);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"CARD", "SPLIT"})
    void makePaymentShouldCreateSuccessfulPaymentForSupportedMethod(
            PaymentMethod paymentMethod
    ) {
        var request = paymentRequest(paymentMethod);
        var mappedEntity = paymentEntity(null, paymentMethod, null);
        var savedEntity = paymentEntity(
                77L,
                paymentMethod,
                PaymentStatus.PAYMENT_SUCCEEDED
        );
        var expectedResponse = paymentResponse(
                77L,
                paymentMethod,
                PaymentStatus.PAYMENT_SUCCEEDED
        );

        when(repository.findByOrderId(request.orderId())).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(repository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.toResponseDto(savedEntity)).thenReturn(expectedResponse);

        var result = paymentService.makePayment(request);

        assertSame(expectedResponse, result);
        assertEquals(PaymentStatus.PAYMENT_SUCCEEDED, mappedEntity.getPaymentStatus());
        verify(repository).save(mappedEntity);
        verify(mapper).toResponseDto(savedEntity);
    }

    @Test
    void makePaymentShouldCreateFailedPaymentForQrMethod() {
        var request = paymentRequest(PaymentMethod.QR);
        var mappedEntity = paymentEntity(null, PaymentMethod.QR, null);
        var savedEntity = paymentEntity(
                77L,
                PaymentMethod.QR,
                PaymentStatus.PAYMENT_FAILED
        );
        var expectedResponse = paymentResponse(
                77L,
                PaymentMethod.QR,
                PaymentStatus.PAYMENT_FAILED
        );

        when(repository.findByOrderId(request.orderId())).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(repository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.toResponseDto(savedEntity)).thenReturn(expectedResponse);

        var result = paymentService.makePayment(request);

        assertSame(expectedResponse, result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, mappedEntity.getPaymentStatus());
        verify(repository).save(mappedEntity);
        verify(mapper).toResponseDto(savedEntity);
    }

    private static CreatePaymentRequestDto paymentRequest(PaymentMethod paymentMethod) {
        return new CreatePaymentRequestDto(
                42L,
                paymentMethod,
                new BigDecimal("1250.50")
        );
    }

    private static PaymentEntity paymentEntity(
            Long id,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus
    ) {
        return new PaymentEntity(
                id,
                42L,
                new BigDecimal("1250.50"),
                paymentStatus,
                paymentMethod
        );
    }

    private static CreatePaymentResponseDto paymentResponse(
            Long paymentId,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus
    ) {
        return new CreatePaymentResponseDto(
                paymentId,
                paymentStatus,
                42L,
                paymentMethod,
                new BigDecimal("1250.50")
        );
    }
}
