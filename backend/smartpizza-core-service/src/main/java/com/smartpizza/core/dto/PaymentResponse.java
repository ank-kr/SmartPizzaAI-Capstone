package com.smartpizza.core.dto;

import com.smartpizza.core.enums.PaymentGateway;
import com.smartpizza.core.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;

    private Long orderId;

    private Long userId;

    private PaymentGateway paymentGateway;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    private BigDecimal amount;

    private String currency;

    private TransactionStatus transactionStatus;

    private String paymentMethod;

    private LocalDateTime paidAt;

    private String message;
}