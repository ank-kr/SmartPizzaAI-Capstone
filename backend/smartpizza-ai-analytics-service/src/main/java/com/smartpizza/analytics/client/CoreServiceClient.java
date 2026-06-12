package com.smartpizza.analytics.client;

import com.smartpizza.analytics.dto.CoreMenuItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.DeliveryPartnerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "smartpizza-core-service")
public interface CoreServiceClient {

    @GetMapping("/api/menu-items")
    List<CoreMenuItemResponse> getAllMenuItems();

    @GetMapping("/api/orders/user/{userId}")
    List<CoreOrderResponse> getOrdersByUserId(@PathVariable Long userId);

    @GetMapping("/api/orders/admin/all")
    List<CoreOrderResponse> getAllOrders();

    @GetMapping("/api/delivery/partners")
    List<DeliveryPartnerResponse> getAllDeliveryPartners();
}