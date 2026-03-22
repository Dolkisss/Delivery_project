package dev.dolkisss.deliveryservice.kafka;

import dev.dolkisss.api.kafka.OrderPaidEvent;
import dev.dolkisss.deliveryservice.domain.DeliveryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@EnableKafka
@Configuration
@AllArgsConstructor
public class OrderPaidKafkaConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
            topics = "${order-paid-topic}",
            containerFactory = "orderPaidEventListenerFactory"
    )
    public void listen(OrderPaidEvent event) {
        log.info("Received order paid event: {}", event);
        deliveryService.proccessOrderPaid(event);
    }


}
