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
public class AnalyticsSummaryResponse {

    private Long totalOrders;

    private Long paidOrders;

    private Long deliveredOrders;

    private Long pendingOrders;

    private BigDecimal totalRevenue;

    private BigDecimal averageOrderValue;
}