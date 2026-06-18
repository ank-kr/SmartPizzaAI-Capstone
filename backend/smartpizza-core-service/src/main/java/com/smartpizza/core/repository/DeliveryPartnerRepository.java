package com.smartpizza.core.repository;

import com.smartpizza.core.entity.DeliveryPartner;
import com.smartpizza.core.enums.PartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {

    boolean existsByUserId(Long userId);

    Optional<DeliveryPartner> findByUserId(Long userId);

    List<DeliveryPartner> findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(
            PartnerStatus partnerStatus
    );
}