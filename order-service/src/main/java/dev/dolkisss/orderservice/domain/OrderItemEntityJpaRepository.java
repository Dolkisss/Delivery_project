package dev.dolkisss.orderservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemEntityJpaRepository extends JpaRepository<OrderItemEntity, Long> {

}