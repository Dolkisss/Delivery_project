package dev.dolkisss.deliveryservice;

import dev.dolkisss.api.http.payment.PaymentMethod;
import dev.dolkisss.api.kafka.DeliveryAssignedEvent;
import dev.dolkisss.api.kafka.OrderPaidEvent;
import dev.dolkisss.deliveryservice.domain.DeliveryEntity;
import dev.dolkisss.deliveryservice.domain.DeliveryEntityRepository;
import dev.dolkisss.deliveryservice.domain.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceApplicationTests {

    private static final String DELIVERY_ASSIGNED_TOPIC = "delivery-assigned-test-topic";

    @Mock
    private DeliveryEntityRepository repository;
    @Mock
    private KafkaTemplate<Long, DeliveryAssignedEvent> kafkaTemplate;

    @InjectMocks
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                deliveryService,
                "deliveryAssignedEventTopic",
                DELIVERY_ASSIGNED_TOPIC
        );
    }

    @Test
    void proccessOrderPaidShouldNotAssignDeliveryTwice() {
        var event = orderPaidEvent();
        var existingDelivery = new DeliveryEntity(
                77L,
                event.orderId(),
                "courier-10",
                20
        );
        when(repository.findByOrderId(event.orderId()))
                .thenReturn(Optional.of(existingDelivery));

        deliveryService.proccessOrderPaid(event);

        verify(repository).findByOrderId(event.orderId());
        verify(repository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void proccessOrderPaidShouldAssignDeliverySaveAndPublishEvent() {
        var event = orderPaidEvent();
        when(repository.findByOrderId(event.orderId())).thenReturn(Optional.empty());
        when(repository.save(any(DeliveryEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, DeliveryEntity.class);
            entity.setId(77L);
            return entity;
        });
        when(kafkaTemplate.send(
                eq(DELIVERY_ASSIGNED_TOPIC),
                eq(event.orderId()),
                any(DeliveryAssignedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));

        deliveryService.proccessOrderPaid(event);

        var deliveryCaptor = ArgumentCaptor.forClass(DeliveryEntity.class);
        verify(repository).save(deliveryCaptor.capture());
        var savedDelivery = deliveryCaptor.getValue();

        var assignedEventCaptor = ArgumentCaptor.forClass(DeliveryAssignedEvent.class);
        verify(kafkaTemplate).send(
                eq(DELIVERY_ASSIGNED_TOPIC),
                eq(event.orderId()),
                assignedEventCaptor.capture()
        );
        var assignedEvent = assignedEventCaptor.getValue();

        assertAll(
                () -> assertEquals(77L, savedDelivery.getId()),
                () -> assertEquals(event.orderId(), savedDelivery.getOrderId()),
                () -> assertTrue(savedDelivery.getCourierName().matches("courier-\\d{1,2}")),
                () -> assertCourierNumberIsInRange(savedDelivery.getCourierName()),
                () -> assertTrue(savedDelivery.getEtaMinutes() >= 10),
                () -> assertTrue(savedDelivery.getEtaMinutes() < 45),
                () -> assertEquals(savedDelivery.getOrderId(), assignedEvent.orderId()),
                () -> assertEquals(savedDelivery.getCourierName(), assignedEvent.courierName()),
                () -> assertEquals(savedDelivery.getEtaMinutes(), assignedEvent.etaMinutes())
        );
    }

    private static OrderPaidEvent orderPaidEvent() {
        return new OrderPaidEvent(
                42L,
                777L,
                new BigDecimal("1250.50"),
                PaymentMethod.CARD
        );
    }

    private static void assertCourierNumberIsInRange(String courierName) {
        var courierNumber = Integer.parseInt(courierName.substring("courier-".length()));
        assertTrue(courierNumber >= 0 && courierNumber < 100);
    }
}
