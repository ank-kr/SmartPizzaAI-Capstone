package com.smartpizza.core.dto;

import com.smartpizza.core.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {

    private Long deliveryId;

    private Long orderId;

    private Long deliveryPartnerId;

    private String partnerName;

    private String phone;

    private String vehicleNumber;

    private DeliveryStatus deliveryStatus;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double dropLatitude;

    private Double dropLongitude;

    private Double distanceKm;

    private Integer estimatedTimeMinutes;

    private LocalDateTime assignedAt;

    private LocalDateTime pickedUpAt;

    private LocalDateTime outForDeliveryAt;

    private LocalDateTime deliveredAt;
}