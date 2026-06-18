package com.smartpizza.analytics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartpizza.analytics.dto.AnalyticsSummaryResponse;
import com.smartpizza.analytics.dto.DeliveryPerformanceResponse;
import com.smartpizza.analytics.dto.TopItemResponse;
import com.smartpizza.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary() {
        AnalyticsSummaryResponse response = analyticsService.getSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-items")
    public ResponseEntity<List<TopItemResponse>> getTopItems() {
        List<TopItemResponse> response = analyticsService.getTopItems();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/delivery-performance")
    public ResponseEntity<DeliveryPerformanceResponse> getDeliveryPerformance() {
        DeliveryPerformanceResponse response = analyticsService.getDeliveryPerformance();
        return ResponseEntity.ok(response);
    }
}