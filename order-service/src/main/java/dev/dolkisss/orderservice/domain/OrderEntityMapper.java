package dev.dolkisss.orderservice.domain;

import dev.dolkisss.orderservice.api.CreateOrderRequestDto;
import dev.dolkisss.orderservice.api.OrderDto;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface OrderEntityMapper {
    OrderEntity toEntity(CreateOrderRequestDto requestDto);

    @AfterMapping
    default void linkOrderItemEntities(@MappingTarget OrderEntity orderEntity) {
        orderEntity.getItems()
                .forEach(orderItemEntity -> orderItemEntity.setOrder(orderEntity));
    }

    OrderDto toOrderDto(OrderEntity orderEntity);
}