package com.smartpizza.core.repository;

import com.smartpizza.core.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByOrderTimeDesc(Long userId);

    List<Order> findAllByOrderByOrderTimeDesc();
}