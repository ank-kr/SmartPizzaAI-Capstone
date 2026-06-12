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
public class CoreMenuItemResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String imageUrl;

    private String size;

    private String crustType;

    private String spiceLevel;

    private Boolean veg;

    private Boolean available;

    private Double rating;

    private Long categoryId;

    private String categoryName;
}