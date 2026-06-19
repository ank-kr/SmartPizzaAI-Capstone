package com.smartpizza.analytics.event;

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
public class PaymentCompletedEvent {

	private Long paymentId;

	private Long orderId;

	private Long userId;

	private BigDecimal amount;

	private String currency;

	private String paymentGateway;

	private String paymentMethod;

	private String transactionStatus;

	private String orderStatus;

	private String paymentStatus;

	private LocalDateTime paidAt;
}