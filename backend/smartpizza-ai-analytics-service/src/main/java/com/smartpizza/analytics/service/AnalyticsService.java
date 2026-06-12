package com.smartpizza.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smartpizza.analytics.client.CoreServiceClient;
import com.smartpizza.analytics.dto.AnalyticsSummaryResponse;
import com.smartpizza.analytics.dto.CoreOrderItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.DeliveryPartnerResponse;
import com.smartpizza.analytics.dto.DeliveryPerformanceResponse;
import com.smartpizza.analytics.dto.TopItemResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CoreServiceClient coreServiceClient;

    public AnalyticsSummaryResponse getSummary() {
        List<CoreOrderResponse> orders = coreServiceClient.getAllOrders();

        long totalOrders = orders.size();

        long paidOrders = orders.stream()
                .filter(order -> "PAID".equalsIgnoreCase(order.getPaymentStatus()))
                .count();

        long deliveredOrders = orders.stream()
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getOrderStatus()))
                .count();

        long pendingOrders = orders.stream()
                .filter(order -> !"DELIVERED".equalsIgnoreCase(order.getOrderStatus())
                        && !"CANCELLED".equalsIgnoreCase(order.getOrderStatus()))
                .count();

        BigDecimal totalRevenue = orders.stream()
                .filter(order -> "PAID".equalsIgnoreCase(order.getPaymentStatus()))
                .map(CoreOrderResponse::getFinalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = BigDecimal.ZERO;

        if (paidOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);
        }

        return AnalyticsSummaryResponse.builder()
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .deliveredOrders(deliveredOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    public List<TopItemResponse> getTopItems() {
        List<CoreOrderResponse> orders = coreServiceClient.getAllOrders();

        Map<Long, TopItemAccumulator> accumulatorMap = new HashMap<>();

        for (CoreOrderResponse order : orders) {
            if (order.getItems() == null) {
                continue;
            }

            for (CoreOrderItemResponse item : order.getItems()) {
                TopItemAccumulator accumulator = accumulatorMap.getOrDefault(
                        item.getMenuItemId(),
                        new TopItemAccumulator(item.getMenuItemId(), item.getItemName())
                );

                accumulator.totalQuantitySold += item.getQuantity();
                accumulator.totalRevenue = accumulator.totalRevenue.add(item.getSubtotal());

                accumulatorMap.put(item.getMenuItemId(), accumulator);
            }
        }

        return accumulatorMap.values()
                .stream()
                .sorted((first, second) -> Long.compare(second.totalQuantitySold, first.totalQuantitySold))
                .limit(5)
                .map(accumulator -> TopItemResponse.builder()
                        .menuItemId(accumulator.menuItemId)
                        .itemName(accumulator.itemName)
                        .totalQuantitySold(accumulator.totalQuantitySold)
                        .totalRevenue(accumulator.totalRevenue)
                        .build())
                .toList();
    }

    public DeliveryPerformanceResponse getDeliveryPerformance() {
        List<DeliveryPartnerResponse> partners = coreServiceClient.getAllDeliveryPartners();

        long totalPartners = partners.size();

        long availablePartners = partners.stream()
                .filter(partner -> "AVAILABLE".equalsIgnoreCase(partner.getPartnerStatus()))
                .count();

        long busyPartners = partners.stream()
                .filter(partner -> "BUSY".equalsIgnoreCase(partner.getPartnerStatus()))
                .count();

        long offlinePartners = partners.stream()
                .filter(partner -> "OFFLINE".equalsIgnoreCase(partner.getPartnerStatus()))
                .count();

        int totalActiveDeliveries = partners.stream()
                .map(DeliveryPartnerResponse::getActiveDeliveryCount)
                .filter(count -> count != null)
                .reduce(0, Integer::sum);

        double averageRating = partners.stream()
                .map(DeliveryPartnerResponse::getRating)
                .filter(rating -> rating != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return DeliveryPerformanceResponse.builder()
                .totalPartners(totalPartners)
                .availablePartners(availablePartners)
                .busyPartners(busyPartners)
                .offlinePartners(offlinePartners)
                .totalActiveDeliveries(totalActiveDeliveries)
                .averageRating(Math.round(averageRating * 100.0) / 100.0)
                .build();
    }

    private static class TopItemAccumulator {

        private final Long menuItemId;

        private final String itemName;

        private Long totalQuantitySold;

        private BigDecimal totalRevenue;

        private TopItemAccumulator(Long menuItemId, String itemName) {
            this.menuItemId = menuItemId;
            this.itemName = itemName;
            this.totalQuantitySold = 0L;
            this.totalRevenue = BigDecimal.ZERO;
        }
    }
}
