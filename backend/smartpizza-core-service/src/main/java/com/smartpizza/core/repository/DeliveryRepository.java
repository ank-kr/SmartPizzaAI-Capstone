package com.smartpizza.core.repository;

import com.smartpizza.core.entity.Delivery;
import com.smartpizza.core.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByDeliveryPartnerIdOrderByAssignedAtDesc(Long deliveryPartnerId);

    List<Delivery> findByDeliveryPartner_IdAndDeliveryStatusInOrderByAssignedAtDesc(
            Long deliveryPartnerId,
            List<DeliveryStatus> deliveryStatuses
    );
}