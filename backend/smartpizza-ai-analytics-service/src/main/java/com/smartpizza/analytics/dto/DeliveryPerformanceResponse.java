package com.smartpizza.analytics.dto;

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
public class DeliveryPerformanceResponse {

    private Long totalPartners;

    private Long availablePartners;

    private Long busyPartners;

    private Long offlinePartners;

    private Integer totalActiveDeliveries;

    private Double averageRating;
}