package com.smartpizza.core.repository;

import com.smartpizza.core.entity.Payment;
import com.smartpizza.core.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndTransactionStatus(Long orderId, TransactionStatus transactionStatus);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}