package com.smartpizza.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartpizza.core.dto.DeliveryPartnerRequest;
import com.smartpizza.core.dto.DeliveryPartnerResponse;
import com.smartpizza.core.dto.DeliveryResponse;
import com.smartpizza.core.dto.DeliveryStatusUpdateRequest;
import com.smartpizza.core.service.DeliveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/partners")
    public ResponseEntity<DeliveryPartnerResponse> createDeliveryPartner(
            @RequestBody DeliveryPartnerRequest request
    ) {
        DeliveryPartnerResponse response = deliveryService.createDeliveryPartner(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/partners")
    public ResponseEntity<List<DeliveryPartnerResponse>> getAllDeliveryPartners() {
        List<DeliveryPartnerResponse> response = deliveryService.getAllDeliveryPartners();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign/{orderId}")
    public ResponseEntity<DeliveryResponse> assignDeliveryPartner(@PathVariable Long orderId) {
        DeliveryResponse response = deliveryService.assignDeliveryPartner(orderId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status/{deliveryId}")
    public ResponseEntity<DeliveryResponse> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestBody DeliveryStatusUpdateRequest request
    ) {
        DeliveryResponse response = deliveryService.updateDeliveryStatus(deliveryId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(@PathVariable Long deliveryId) {
        DeliveryResponse response = deliveryService.getDeliveryById(deliveryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrderId(@PathVariable Long orderId) {
        DeliveryResponse response = deliveryService.getDeliveryByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}