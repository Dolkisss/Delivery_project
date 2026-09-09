package dev.dolkisss.orderservice;

import dev.dolkisss.api.http.order.CreateOrderRequestDto;
import dev.dolkisss.api.http.order.OrderStatus;
import dev.dolkisss.api.http.payment.CreatePaymentRequestDto;
import dev.dolkisss.api.http.payment.CreatePaymentResponseDto;
import dev.dolkisss.api.http.payment.PaymentMethod;
import dev.dolkisss.api.http.payment.PaymentStatus;
import dev.dolkisss.api.kafka.OrderPaidEvent;
import dev.dolkisss.orderservice.api.OrderPaymentRequest;
import dev.dolkisss.orderservice.domain.OrderService;
import dev.dolkisss.orderservice.domain.db.OrderEntity;
import dev.dolkisss.orderservice.domain.db.OrderEntityMapper;
import dev.dolkisss.orderservice.domain.db.OrderItemEntity;
import dev.dolkisss.orderservice.domain.db.OrderJpaRepository;
import dev.dolkisss.orderservice.external.PaymentHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceApplicationTests {

    private static final String ORDER_PAID_TOPIC = "order-paid-test-topic";

    @Mock
    private OrderJpaRepository repository;
    @Mock
    private OrderEntityMapper orderEntityMapper;
    @Mock
    private PaymentHttpClient paymentHttpClient;
    @Mock
    private KafkaTemplate<Long, OrderPaidEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderPaidTopic", ORDER_PAID_TOPIC);
    }

    @Test
    void createShouldCalculatePricesSetPendingStatusAndSaveOrder() {
        var request = new CreateOrderRequestDto(10L, "Test address", Set.of());
        var firstItem = orderItem(101L, 2);
        var secondItem = orderItem(102L, 3);
        var entity = order(1L, null, BigDecimal.ZERO);
        entity.setItems(new LinkedHashSet<>(Set.of(firstItem, secondItem)));

        when(orderEntityMapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        var result = orderService.create(request);

        var expectedTotal = firstItem.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(firstItem.getQuantity()))
                .add(secondItem.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(secondItem.getQuantity())));

        assertAll(
                () -> assertSame(entity, result),
                () -> assertEquals(OrderStatus.PENDING_PAYMENT, entity.getOrderStatus()),
                () -> assertPriceIsInGeneratedRange(firstItem.getPriceAtPurchase()),
                () -> assertPriceIsInGeneratedRange(secondItem.getPriceAtPurchase()),
                () -> assertEquals(0, expectedTotal.compareTo(entity.getTotalAmount()))
        );
        verify(orderEntityMapper).toEntity(request);
        verify(repository).save(entity);
    }

    @Test
    void createShouldSaveZeroTotalForOrderWithoutItems() {
        var request = new CreateOrderRequestDto(10L, "Test address", Set.of());
        var entity = order(1L, null, null);

        when(orderEntityMapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        var result = orderService.create(request);

        assertAll(
                () -> assertSame(entity, result),
                () -> assertEquals(OrderStatus.PENDING_PAYMENT, entity.getOrderStatus()),
                () -> assertEquals(BigDecimal.ZERO, entity.getTotalAmount())
        );
        verify(repository).save(entity);
    }

    @Test
    void getOrderOrThrowShouldReturnExistingOrder() {
        var entity = order(42L, OrderStatus.PENDING_PAYMENT, BigDecimal.TEN);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));

        var result = orderService.getOrderOrThrow(42L);

        assertSame(entity, result);
        verify(repository).findById(42L);
    }

    @Test
    void getOrderOrThrowShouldThrowNotFoundForMissingOrder() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.getOrderOrThrow(42L)
        );

        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode()),
                () -> assertEquals("Entity with id `42` not found", exception.getReason())
        );
    }

    @Test
    void processPaymentShouldMarkOrderPaidSaveAndPublishEventWhenPaymentSucceeds() {
        var entity = order(42L, OrderStatus.PENDING_PAYMENT, new BigDecimal("1250.50"));
        var request = new OrderPaymentRequest(PaymentMethod.CARD);
        var paymentResponse = new CreatePaymentResponseDto(
                777L,
                PaymentStatus.PAYMENT_SUCCEEDED,
                42L,
                PaymentMethod.CARD,
                entity.getTotalAmount()
        );

        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(paymentHttpClient.createPayment(any(CreatePaymentRequestDto.class)))
                .thenReturn(paymentResponse);
        when(kafkaTemplate.send(eq(ORDER_PAID_TOPIC), eq(42L), any(OrderPaidEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(repository.save(entity)).thenReturn(entity);

        var result = orderService.processPayment(42L, request);

        var paymentRequestCaptor = ArgumentCaptor.forClass(CreatePaymentRequestDto.class);
        verify(paymentHttpClient).createPayment(paymentRequestCaptor.capture());
        var sentPaymentRequest = paymentRequestCaptor.getValue();

        var eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(kafkaTemplate).send(eq(ORDER_PAID_TOPIC), eq(42L), eventCaptor.capture());
        var sentEvent = eventCaptor.getValue();

        assertAll(
                () -> assertSame(entity, result),
                () -> assertEquals(OrderStatus.PAID, entity.getOrderStatus()),
                () -> assertEquals(42L, sentPaymentRequest.orderId()),
                () -> assertEquals(PaymentMethod.CARD, sentPaymentRequest.paymentMethod()),
                () -> assertEquals(entity.getTotalAmount(), sentPaymentRequest.amount()),
                () -> assertEquals(42L, sentEvent.orderId()),
                () -> assertEquals(777L, sentEvent.paymentId()),
                () -> assertEquals(PaymentMethod.CARD, sentEvent.paymentMethod()),
                () -> assertEquals(entity.getTotalAmount(), sentEvent.amount())
        );
        verify(repository).save(entity);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PAYMENT_FAILED", "REFUNDED"})
    void processPaymentShouldMarkOrderPaymentFailedAndNotPublishEvent(
            PaymentStatus paymentStatus
    ) {
        var entity = order(42L, OrderStatus.PENDING_PAYMENT, new BigDecimal("1250.50"));
        var request = new OrderPaymentRequest(PaymentMethod.QR);
        var paymentResponse = new CreatePaymentResponseDto(
                777L,
                paymentStatus,
                42L,
                PaymentMethod.QR,
                entity.getTotalAmount()
        );

        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(paymentHttpClient.createPayment(any(CreatePaymentRequestDto.class)))
                .thenReturn(paymentResponse);
        when(repository.save(entity)).thenReturn(entity);

        var result = orderService.processPayment(42L, request);

        assertAll(
                () -> assertSame(entity, result),
                () -> assertEquals(OrderStatus.PAYMENT_FAILED, entity.getOrderStatus())
        );
        verify(repository).save(entity);
        verifyNoInteractions(kafkaTemplate);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "PENDING_PAYMENT"
    )
    void processPaymentShouldRejectOrderThatIsNotPendingPayment(OrderStatus currentStatus) {
        var entity = order(42L, currentStatus, new BigDecimal("1250.50"));
        when(repository.findById(42L)).thenReturn(Optional.of(entity));

        var exception = assertThrows(
                RuntimeException.class,
                () -> orderService.processPayment(
                        42L,
                        new OrderPaymentRequest(PaymentMethod.SPLIT)
                )
        );

        assertEquals("Order must be in status PENDING_PAYMENT", exception.getMessage());
        verifyNoInteractions(paymentHttpClient, kafkaTemplate);
        verify(repository, never()).save(any());
    }

    @Test
    void processPaymentShouldStopWhenOrderDoesNotExist() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.processPayment(
                        42L,
                        new OrderPaymentRequest(PaymentMethod.CARD)
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(paymentHttpClient, kafkaTemplate);
        verify(repository, never()).save(any());
    }

    private static OrderEntity order(
            Long id,
            OrderStatus status,
            BigDecimal totalAmount
    ) {
        var entity = new OrderEntity();
        entity.setId(id);
        entity.setOrderStatus(status);
        entity.setTotalAmount(totalAmount);
        return entity;
    }

    private static OrderItemEntity orderItem(Long itemId, int quantity) {
        var item = new OrderItemEntity();
        item.setItemId(itemId);
        item.setQuantity(quantity);
        return item;
    }

    private static void assertPriceIsInGeneratedRange(BigDecimal price) {
        assertAll(
                () -> assertNotNull(price),
                () -> assertTrue(price.compareTo(BigDecimal.valueOf(100)) >= 0),
                () -> assertTrue(price.compareTo(BigDecimal.valueOf(5000)) < 0)
        );
    }
}
