package com.smartpizza.core.dto;

import com.smartpizza.core.enums.OrderStatus;
import com.smartpizza.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;

    private Long userId;

    private String couponCode;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal deliveryCharge;

    private BigDecimal finalAmount;

    private String deliveryAddress;

    private Double deliveryLatitude;

    private Double deliveryLongitude;

    private LocalDateTime orderTime;

    private List<OrderItemResponse> items;
}