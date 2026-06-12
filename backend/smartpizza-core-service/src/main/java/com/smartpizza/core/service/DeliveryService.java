package com.smartpizza.core.service;

import com.smartpizza.core.dto.DeliveryPartnerRequest;
import com.smartpizza.core.dto.DeliveryPartnerResponse;
import com.smartpizza.core.dto.DeliveryResponse;
import com.smartpizza.core.dto.DeliveryStatusUpdateRequest;
import com.smartpizza.core.entity.Delivery;
import com.smartpizza.core.entity.DeliveryPartner;
import com.smartpizza.core.entity.Order;
import com.smartpizza.core.enums.DeliveryStatus;
import com.smartpizza.core.enums.OrderStatus;
import com.smartpizza.core.enums.PartnerStatus;
import com.smartpizza.core.enums.PaymentStatus;
import com.smartpizza.core.repository.DeliveryPartnerRepository;
import com.smartpizza.core.repository.DeliveryRepository;
import com.smartpizza.core.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final double RESTAURANT_LATITUDE = 12.9352;
    private static final double RESTAURANT_LONGITUDE = 77.6245;

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    public DeliveryPartnerResponse createDeliveryPartner(DeliveryPartnerRequest request) {
        validateDeliveryPartnerRequest(request);

        if (deliveryPartnerRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Delivery partner already exists for user id: " + request.getUserId());
        }

        DeliveryPartner partner = DeliveryPartner.builder()
                .userId(request.getUserId())
                .partnerName(request.getPartnerName())
                .phone(request.getPhone())
                .vehicleNumber(request.getVehicleNumber())
                .partnerStatus(request.getPartnerStatus() == null ? PartnerStatus.AVAILABLE : request.getPartnerStatus())
                .currentLatitude(request.getCurrentLatitude())
                .currentLongitude(request.getCurrentLongitude())
                .activeDeliveryCount(request.getActiveDeliveryCount() == null ? 0 : request.getActiveDeliveryCount())
                .rating(request.getRating() == null ? 4.5 : request.getRating())
                .build();

        DeliveryPartner savedPartner = deliveryPartnerRepository.save(partner);

        return mapToDeliveryPartnerResponse(savedPartner);
    }

    public List<DeliveryPartnerResponse> getAllDeliveryPartners() {
        return deliveryPartnerRepository.findAll()
                .stream()
                .map(this::mapToDeliveryPartnerResponse)
                .toList();
    }

    @Transactional
    public DeliveryResponse assignDeliveryPartner(Long orderId) {
        validateOrderId(orderId);

        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Delivery already assigned for order id: " + orderId);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        validateOrderForDeliveryAssignment(order);

        DeliveryPartner bestPartner = findBestAvailablePartner();

        double distanceKm = calculateDistanceKm(
                RESTAURANT_LATITUDE,
                RESTAURANT_LONGITUDE,
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude()
        );

        int etaMinutes = calculateEtaMinutes(distanceKm);

        Delivery delivery = Delivery.builder()
                .orderId(order.getId())
                .deliveryPartner(bestPartner)
                .deliveryStatus(DeliveryStatus.ASSIGNED)
                .pickupLatitude(RESTAURANT_LATITUDE)
                .pickupLongitude(RESTAURANT_LONGITUDE)
                .dropLatitude(order.getDeliveryLatitude())
                .dropLongitude(order.getDeliveryLongitude())
                .distanceKm(round(distanceKm))
                .estimatedTimeMinutes(etaMinutes)
                .assignedAt(LocalDateTime.now())
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);

        bestPartner.setPartnerStatus(PartnerStatus.BUSY);
        bestPartner.setActiveDeliveryCount(getSafeActiveDeliveryCount(bestPartner) + 1);
        deliveryPartnerRepository.save(bestPartner);

        order.setOrderStatus(OrderStatus.ASSIGNED_TO_DELIVERY);
        orderRepository.save(order);

        return mapToDeliveryResponse(savedDelivery);
    }

    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long deliveryId, DeliveryStatusUpdateRequest request) {
        validateDeliveryId(deliveryId);
        validateDeliveryStatusUpdateRequest(request);

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + deliveryId));

        Order order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + delivery.getOrderId()));

        DeliveryStatus newStatus = request.getDeliveryStatus();

        delivery.setDeliveryStatus(newStatus);

        if (newStatus == DeliveryStatus.PICKED_UP) {
            delivery.setPickedUpAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        }

        if (newStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            delivery.setOutForDeliveryAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        }

        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.DELIVERED);
            makePartnerAvailable(delivery.getDeliveryPartner());
        }

        if (newStatus == DeliveryStatus.CANCELLED) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            makePartnerAvailable(delivery.getDeliveryPartner());
        }

        orderRepository.save(order);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        return mapToDeliveryResponse(savedDelivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryByOrderId(Long orderId) {
        validateOrderId(orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order id: " + orderId));

        return mapToDeliveryResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(Long deliveryId) {
        validateDeliveryId(deliveryId);

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + deliveryId));

        return mapToDeliveryResponse(delivery);
    }

    private void validateDeliveryPartnerRequest(DeliveryPartnerRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        if (request.getUserId() == null) {
            throw new RuntimeException("User id is required");
        }

        if (request.getPartnerName() == null || request.getPartnerName().trim().isEmpty()) {
            throw new RuntimeException("Partner name is required");
        }

        if (request.getCurrentLatitude() == null) {
            throw new RuntimeException("Current latitude is required");
        }

        if (request.getCurrentLongitude() == null) {
            throw new RuntimeException("Current longitude is required");
        }
    }

    private void validateOrderForDeliveryAssignment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Order must be paid before delivery assignment");
        }

        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed orders can be assigned for delivery");
        }

        if (order.getDeliveryLatitude() == null || order.getDeliveryLongitude() == null) {
            throw new RuntimeException("Order delivery location is missing");
        }
    }

    private DeliveryPartner findBestAvailablePartner() {
        List<DeliveryPartner> availablePartners = deliveryPartnerRepository
                .findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(PartnerStatus.AVAILABLE);

        if (availablePartners.isEmpty()) {
            throw new RuntimeException("No available delivery partner found");
        }

        return availablePartners.stream()
                .min(Comparator
                        .comparingInt(this::getSafeActiveDeliveryCount)
                        .thenComparingDouble(this::calculatePartnerDistanceFromRestaurant)
                        .thenComparing(Comparator.comparingDouble(this::getSafeRating).reversed()))
                .orElseThrow(() -> new RuntimeException("No available delivery partner found"));
    }

    private double calculatePartnerDistanceFromRestaurant(DeliveryPartner partner) {
        if (partner.getCurrentLatitude() == null || partner.getCurrentLongitude() == null) {
            return Double.MAX_VALUE;
        }

        return calculateDistanceKm(
                RESTAURANT_LATITUDE,
                RESTAURANT_LONGITUDE,
                partner.getCurrentLatitude(),
                partner.getCurrentLongitude()
        );
    }

    private void makePartnerAvailable(DeliveryPartner partner) {
        int updatedActiveDeliveryCount = Math.max(0, getSafeActiveDeliveryCount(partner) - 1);

        partner.setActiveDeliveryCount(updatedActiveDeliveryCount);
        partner.setPartnerStatus(PartnerStatus.AVAILABLE);

        deliveryPartnerRepository.save(partner);
    }

    private int getSafeActiveDeliveryCount(DeliveryPartner partner) {
        return partner.getActiveDeliveryCount() == null ? 0 : partner.getActiveDeliveryCount();
    }

    private double getSafeRating(DeliveryPartner partner) {
        return partner.getRating() == null ? 0.0 : partner.getRating();
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("Order id is required");
        }
    }

    private void validateDeliveryId(Long deliveryId) {
        if (deliveryId == null) {
            throw new RuntimeException("Delivery id is required");
        }
    }

    private void validateDeliveryStatusUpdateRequest(DeliveryStatusUpdateRequest request) {
        if (request == null || request.getDeliveryStatus() == null) {
            throw new RuntimeException("Delivery status is required");
        }
    }

    private double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int earthRadiusKm = 6371;

        double latitudeDifference = Math.toRadians(lat2 - lat1);
        double longitudeDifference = Math.toRadians(lon2 - lon1);

        double firstLatitude = Math.toRadians(lat1);
        double secondLatitude = Math.toRadians(lat2);

        double haversineValue = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);

        double angularDistance = 2 * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));

        return earthRadiusKm * angularDistance;
    }

    private int calculateEtaMinutes(double distanceKm) {
        int etaMinutes = (int) Math.ceil(distanceKm * 4);
        return Math.max(10, etaMinutes);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private DeliveryPartnerResponse mapToDeliveryPartnerResponse(DeliveryPartner partner) {
        return DeliveryPartnerResponse.builder()
                .deliveryPartnerId(partner.getId())
                .userId(partner.getUserId())
                .partnerName(partner.getPartnerName())
                .phone(partner.getPhone())
                .vehicleNumber(partner.getVehicleNumber())
                .partnerStatus(partner.getPartnerStatus())
                .currentLatitude(partner.getCurrentLatitude())
                .currentLongitude(partner.getCurrentLongitude())
                .activeDeliveryCount(partner.getActiveDeliveryCount())
                .rating(partner.getRating())
                .build();
    }

    private DeliveryResponse mapToDeliveryResponse(Delivery delivery) {
        DeliveryPartner partner = delivery.getDeliveryPartner();

        return DeliveryResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .deliveryPartnerId(partner.getId())
                .partnerName(partner.getPartnerName())
                .phone(partner.getPhone())
                .vehicleNumber(partner.getVehicleNumber())
                .deliveryStatus(delivery.getDeliveryStatus())
                .pickupLatitude(delivery.getPickupLatitude())
                .pickupLongitude(delivery.getPickupLongitude())
                .dropLatitude(delivery.getDropLatitude())
                .dropLongitude(delivery.getDropLongitude())
                .distanceKm(delivery.getDistanceKm())
                .estimatedTimeMinutes(delivery.getEstimatedTimeMinutes())
                .assignedAt(delivery.getAssignedAt())
                .pickedUpAt(delivery.getPickedUpAt())
                .outForDeliveryAt(delivery.getOutForDeliveryAt())
                .deliveredAt(delivery.getDeliveredAt())
                .build();
    }
}