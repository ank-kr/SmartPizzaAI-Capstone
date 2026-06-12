package com.smartpizza.core.dto;

import com.smartpizza.core.enums.CrustType;
import com.smartpizza.core.enums.ItemSize;
import com.smartpizza.core.enums.SpiceLevel;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String imageUrl;

    private ItemSize size;

    private CrustType crustType;

    private SpiceLevel spiceLevel;

    private Boolean veg;

    private Boolean available;

    private Double rating;

    private Long categoryId;

    private String categoryName;
}