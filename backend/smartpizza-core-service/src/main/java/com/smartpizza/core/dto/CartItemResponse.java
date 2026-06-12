package com.smartpizza.core.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long cartItemId;

    private Long menuItemId;

    private String itemName;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}