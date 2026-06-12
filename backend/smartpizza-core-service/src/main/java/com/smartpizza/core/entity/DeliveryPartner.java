package com.smartpizza.core.entity;

import com.smartpizza.core.enums.PartnerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Delivery user's id from auth-service.
     * We store only userId because User entity belongs to auth-service.
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String partnerName;

    private String phone;

    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus partnerStatus;

    private Double currentLatitude;

    private Double currentLongitude;

    private Integer activeDeliveryCount;

    private Double rating;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.partnerStatus == null) {
            this.partnerStatus = PartnerStatus.AVAILABLE;
        }

        if (this.activeDeliveryCount == null) {
            this.activeDeliveryCount = 0;
        }

        if (this.rating == null) {
            this.rating = 4.5;
        }

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}