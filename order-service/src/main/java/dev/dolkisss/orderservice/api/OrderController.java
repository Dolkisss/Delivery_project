package dev.dolkisss.orderservice.api;

import dev.dolkisss.api.http.order.CreateOrderRequestDto;
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

    @PostMapping
    public OrderDto create(
            @RequestBody CreateOrderRequestDto request
    ) {
        log.info("Creating order: request={}", request);
        var saved = orderService.create(request);
        return orderEntityMapper.toOrderDto(saved);
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
