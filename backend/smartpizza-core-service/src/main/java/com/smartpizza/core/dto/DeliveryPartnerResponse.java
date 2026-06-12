package com.smartpizza.core.dto;

import com.smartpizza.core.enums.PartnerStatus;
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
public class DeliveryPartnerResponse {

    private Long deliveryPartnerId;

    private Long userId;

    private String partnerName;

    private String phone;

    private String vehicleNumber;

    private PartnerStatus partnerStatus;

    private Double currentLatitude;

    private Double currentLongitude;

    private Integer activeDeliveryCount;

    private Double rating;
}