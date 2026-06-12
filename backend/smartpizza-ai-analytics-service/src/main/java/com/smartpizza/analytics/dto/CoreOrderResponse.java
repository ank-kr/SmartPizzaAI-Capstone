package com.smartpizza.analytics.dto;

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
public class CoreOrderResponse {

    private Long orderId;

    private Long userId;

    private String couponCode;

    private String orderStatus;

    private String paymentStatus;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal deliveryCharge;

    private BigDecimal finalAmount;

    private String deliveryAddress;

    private Double deliveryLatitude;

    private Double deliveryLongitude;

    private LocalDateTime orderTime;

    private List<CoreOrderItemResponse> items;
}