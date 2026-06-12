package com.smartpizza.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    private Long userId;

    private String couponCode;

    private String deliveryAddress;

    private Double deliveryLatitude;

    private Double deliveryLongitude;
}
