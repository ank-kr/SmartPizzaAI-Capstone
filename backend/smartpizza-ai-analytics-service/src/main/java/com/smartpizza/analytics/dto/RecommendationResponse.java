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
public class RecommendationResponse {

    private String recommendationType;

    private Long itemId;

    private String itemName;

    private String categoryName;

    private BigDecimal price;

    private String reason;

    private Double score;
}