package dev.dolkisss.orderservice.api;

import dev.dolkisss.api.http.order.OrderDto;
import dev.dolkisss.orderservice.domain.db.OrderEntityMapper;
import dev.dolkisss.orderservice.domain.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    private final OrderEntityMapper orderEntityMapper;

    @PostMapping("/{id}/pay")
    public OrderDto payOrder(
            @PathVariable Long id,
            @RequestBody OrderPaymentRequest request
    ) {
        log.info("Paying order with id={}, request={}", id, request);
        var entity = orderService.processPayment(id, request);
        return orderEntityMapper.toOrderDto(entity);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(
            @PathVariable Long id
    ) {
        log.info("Retrieving order with id {}", id);
        var found = orderService.getOrderOrThrow(id);
        return orderEntityMapper.toOrderDto(found);
    }
}
