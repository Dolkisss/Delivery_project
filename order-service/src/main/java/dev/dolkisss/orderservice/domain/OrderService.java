package dev.dolkisss.orderservice.domain;

import dev.dolkisss.api.http.order.CreateOrderRequestDto;
import dev.dolkisss.api.http.order.OrderStatus;
import dev.dolkisss.api.http.payment.CreatePaymentRequestDto;
import dev.dolkisss.api.http.payment.CreatePaymentResponseDto;
import dev.dolkisss.api.http.payment.PaymentStatus;
import dev.dolkisss.api.kafka.DeliveryAssignedEvent;
import dev.dolkisss.api.kafka.OrderPaidEvent;
import dev.dolkisss.orderservice.api.OrderPaymentRequest;
import dev.dolkisss.orderservice.domain.db.OrderEntity;
import dev.dolkisss.orderservice.domain.db.OrderEntityMapper;
import dev.dolkisss.orderservice.domain.db.OrderItemEntity;
import dev.dolkisss.orderservice.domain.db.OrderJpaRepository;
import dev.dolkisss.orderservice.external.PaymentHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderService {

    private final OrderJpaRepository repository;
    private final OrderEntityMapper orderEntityMapper;
    private final PaymentHttpClient paymentHttpClient;
    private final KafkaTemplate<Long, OrderPaidEvent> kafkaTemplate;

    @Value("${order-paid-topic}")
    private String orderPaidTopic;

    public OrderEntity create(CreateOrderRequestDto request) {
        var entity = orderEntityMapper.toEntity(request);
        calculatePricingForOrder(entity);
        entity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        return repository.save(entity);
    }

    private void calculatePricingForOrder(OrderEntity entity) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemEntity item : entity.getItems()) {
            var randomPrice = ThreadLocalRandom.current().nextDouble(100, 5000);
            item.setPriceAtPurchase(BigDecimal.valueOf(randomPrice));

            totalPrice = item.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .add(totalPrice);
        }
        entity.setTotalAmount(totalPrice);
    }

    public OrderEntity getOrderOrThrow(
            Long id
      ) {
        var orderItemEntityOptional = repository.findById(id);
        return orderItemEntityOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id)));
    }

    public OrderEntity processPayment(
            Long id,
            OrderPaymentRequest request
    ) {
        var entity = getOrderOrThrow(id);
        if (!entity.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT)) {
            throw new RuntimeException("Order must be in status PENDING_PAYMENT");
        }

        var response = paymentHttpClient
                .createPayment(CreatePaymentRequestDto
                        .builder()
                        .orderId(id)
                        .paymentMethod(request.paymentMethod())
                        .amount(entity.getTotalAmount())
                        .build()
                );

        var status = response.paymentStatus().equals(PaymentStatus.PAYMENT_SUCCEEDED)
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        entity.setOrderStatus(status);
        if (status.equals(OrderStatus.PAID)) {
            sendOrderPaidEvent(entity, response);
        }
        return repository.save(entity);
    }

    private void sendOrderPaidEvent(
            OrderEntity entity,
            CreatePaymentResponseDto paymentResponseDto
    ) {
        kafkaTemplate.send(
                orderPaidTopic,
                entity.getId(),
                OrderPaidEvent.builder()
                        .orderId(entity.getId())
                        .amount(entity.getTotalAmount())
                        .paymentMethod(paymentResponseDto.paymentMethod())
                        .paymentId(paymentResponseDto.paymentId())
                        .build()
        ).thenAccept(result -> {
            log.info("Order Paid event sent: id={}", entity.getId());
        });
    }


    public void proccessDeliveryAssigned(
            DeliveryAssignedEvent event
    ) {
        var order = getOrderOrThrow(event.orderId());

        if (order.getOrderStatus().equals(OrderStatus.DELIVERY_ASSIGNED)) {
            log.info("Order delivery already processed: orderId={}", order.getId());
        } else if (
                !order.getOrderStatus().equals(OrderStatus.PAID)
        ) {
            log.error("Order have incorrect state: state={}", order.getId());
            return;
        }

        order.setOrderStatus(OrderStatus.DELIVERY_ASSIGNED);
        order.setCourierName(event.courierName());
        order.setEtaMinutes(event.etaMinutes());
        repository.save(order);
        log.info("Order delivery assigned processed: orderId={}", order.getId());
    }
}
