package com.smartpizza.analytics.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignedEvent {

	private Long deliveryId;

	private Long orderId;

	private Long deliveryPartnerId;

	private Long deliveryPartnerUserId;

	private String partnerName;

	private String phone;

	private String vehicleNumber;

	private String deliveryStatus;

	private Double distanceKm;

	private Integer estimatedTimeMinutes;

	private LocalDateTime assignedAt;
}