package com.smartpizza.core.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;

    private Long userId;

    private BigDecimal finalAmount;

    private String orderStatus;

    private String paymentStatus;

    private LocalDateTime orderTime;
}