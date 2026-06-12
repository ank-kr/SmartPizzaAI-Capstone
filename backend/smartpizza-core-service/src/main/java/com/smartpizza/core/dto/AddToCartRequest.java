package com.smartpizza.core.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    private Long userId;

    private Long menuItemId;

    private Integer quantity;
}