package dev.dolkisss.orderservice.domain.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemEntityJpaRepository extends JpaRepository<OrderItemEntity, Long> {

}