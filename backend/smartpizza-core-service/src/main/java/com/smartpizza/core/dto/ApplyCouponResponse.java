package com.smartpizza.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponResponse {

    private String couponCode;

    private BigDecimal cartTotal;

    private BigDecimal discountAmount;

    private BigDecimal amountAfterDiscount;

    private Boolean valid;

    private String message;
}