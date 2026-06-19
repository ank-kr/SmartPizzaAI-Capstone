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
public class DeliveryStatusUpdatedEvent {

    private Long deliveryId;

    private Long orderId;

    private Long deliveryPartnerId;

    private String partnerName;

    private String previousStatus;

    private String newStatus;

    private String orderStatus;

    private LocalDateTime updatedAt;
}