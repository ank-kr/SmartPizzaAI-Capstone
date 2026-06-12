package com.smartpizza.analytics.dto;

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
public class CoreOrderItemResponse {

    private Long orderItemId;

    private Long menuItemId;

    private String itemName;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}