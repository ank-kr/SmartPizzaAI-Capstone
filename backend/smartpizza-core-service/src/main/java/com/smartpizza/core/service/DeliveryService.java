package com.smartpizza.core.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.smartpizza.core.event.DeliveryAssignedEvent;
import com.smartpizza.core.event.DeliveryStatusUpdatedEvent;
import com.smartpizza.core.kafka.KafkaEventProducer;
import com.smartpizza.core.repository.DeliveryPartnerRepository;
import com.smartpizza.core.repository.DeliveryRepository;
import com.smartpizza.core.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    // static restaurant location
    private static final double RESTAURANT_LATITUDE = 12.9352;
    private static final double RESTAURANT_LONGITUDE = 77.6245;

    //// Handles database operations for delivery partner,delivery records, fetch
    //// and update delivery assignment
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final KafkaEventProducer kafkaEventProducer;

    // admin will create delivery partner
    public DeliveryPartnerResponse createDeliveryPartner(DeliveryPartnerRequest request) {
        validateDeliveryPartnerRequest(request);

        log.info("Creating delivery partner profile for userId={}", request.getUserId());

        if (deliveryPartnerRepository.existsByUserId(request.getUserId())) {
            log.warn("Delivery partner profile already exists for userId={}", request.getUserId());
            throw new RuntimeException("Delivery partner already exists for user id: " + request.getUserId());
        }

        DeliveryPartner partner = DeliveryPartner.builder().userId(request.getUserId()) //// Link delivery partner
                                                                                        //// profile with auth-service
                                                                                        //// user id
                .partnerName(request.getPartnerName()).phone(request.getPhone())// Store partner profile details.
                .vehicleNumber(request.getVehicleNumber())
                .partnerStatus(
                        request.getPartnerStatus() == null ? PartnerStatus.AVAILABLE : request.getPartnerStatus()) // If
                                                                                                                    // status
                                                                                                                    // is
                                                                                                                    // not
                                                                                                                    // provided,
                                                                                                                    // make
                                                                                                                    // partner
                                                                                                                    // available
                                                                                                                    // by
                                                                                                                    // default.
                .currentLatitude(request.getCurrentLatitude()).currentLongitude(request.getCurrentLongitude())
                .activeDeliveryCount(request.getActiveDeliveryCount() == null ? 0 : request.getActiveDeliveryCount())
                .rating(request.getRating() == null ? 4.5 : request.getRating()).build();

        DeliveryPartner savedPartner = deliveryPartnerRepository.save(partner);

        log.info("Delivery partner profile created successfully. partnerId={}, userId={}, status={}",
                savedPartner.getId(), savedPartner.getUserId(), savedPartner.getPartnerStatus());

        return mapToDeliveryPartnerResponse(savedPartner);
    }

    // service to get all delivery partner
    public List<DeliveryPartnerResponse> getAllDeliveryPartners() {
        log.info("Fetching all delivery partners");

        List<DeliveryPartnerResponse> partners = deliveryPartnerRepository.findAll().stream()
                .map(this::mapToDeliveryPartnerResponse).toList();

        log.info("Delivery partners fetched successfully. count={}", partners.size());

        return partners;
    }

    // All database operations inside this method should be treated as one single
    // unit of work.(means no half completed db state.
    @Transactional
    public DeliveryResponse assignDeliveryPartner(Long orderId) {
        validateOrderId(orderId); // delivery will only be assigned if there is valid order id

        log.info("Starting delivery assignment for orderId={}", orderId);

        // check for duplicate delivery assignment for the same order
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Delivery assignment skipped because delivery already exists. orderId={}", orderId);
            throw new RuntimeException("Delivery already assigned for order id: " + orderId);
        }

        // Fetch order before assigning delivery partner.
        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
            log.warn("Delivery assignment failed because order was not found. orderId={}", orderId);
            return new RuntimeException("Order not found with id: " + orderId);
        });

        // Order must be paid, confirmed and must have delivery location.
        validateOrderForDeliveryAssignment(order);

        // Select best available partner based on workload, distance and rating.
        DeliveryPartner bestPartner = findBestAvailablePartner();

        log.info("Best delivery partner selected. orderId={}, partnerId={}, activeDeliveryCount={}, rating={}", orderId,
                bestPartner.getId(), getSafeActiveDeliveryCount(bestPartner), getSafeRating(bestPartner));

        // Calculate restaurant-to-customer distance.
        double distanceKm = calculateDistanceKm(RESTAURANT_LATITUDE, RESTAURANT_LONGITUDE, order.getDeliveryLatitude(),
                order.getDeliveryLongitude());

        // ETA calculations
        int etaMinutes = calculateEtaMinutes(distanceKm);

        // Create delivery record linked to order and selected partner.
        Delivery delivery = Delivery.builder().orderId(order.getId()).deliveryPartner(bestPartner)
                .deliveryStatus(DeliveryStatus.ASSIGNED).pickupLatitude(RESTAURANT_LATITUDE)
                .pickupLongitude(RESTAURANT_LONGITUDE).dropLatitude(order.getDeliveryLatitude())
                .dropLongitude(order.getDeliveryLongitude()).distanceKm(round(distanceKm))
                .estimatedTimeMinutes(etaMinutes).assignedAt(LocalDateTime.now()).build();

        Delivery savedDelivery = deliveryRepository.save(delivery);

        DeliveryAssignedEvent deliveryAssignedEvent = DeliveryAssignedEvent.builder().deliveryId(savedDelivery.getId())
                .orderId(savedDelivery.getOrderId()).deliveryPartnerId(bestPartner.getId())
                .deliveryPartnerUserId(bestPartner.getUserId()).partnerName(bestPartner.getPartnerName())
                .phone(bestPartner.getPhone()).vehicleNumber(bestPartner.getVehicleNumber())
                .deliveryStatus(savedDelivery.getDeliveryStatus().name()).distanceKm(savedDelivery.getDistanceKm())
                .estimatedTimeMinutes(savedDelivery.getEstimatedTimeMinutes()).assignedAt(savedDelivery.getAssignedAt())
                .build();

        kafkaEventProducer.publishDeliveryAssignedEvent(deliveryAssignedEvent);

        // Mark partner busy so same partner is not selected again immediately.
        bestPartner.setPartnerStatus(PartnerStatus.BUSY);
        bestPartner.setActiveDeliveryCount(getSafeActiveDeliveryCount(bestPartner) + 1);
        deliveryPartnerRepository.save(bestPartner);

        // Update order lifecycle after successful delivery assignment.
        order.setOrderStatus(OrderStatus.ASSIGNED_TO_DELIVERY);
        orderRepository.save(order);

        log.info(
                "Delivery assigned successfully. deliveryId={}, orderId={}, partnerId={}, distanceKm={}, etaMinutes={}",
                savedDelivery.getId(), orderId, bestPartner.getId(), savedDelivery.getDistanceKm(),
                savedDelivery.getEstimatedTimeMinutes());

        // Return delivery details in DTO format for frontend/customer tracking.
        return mapToDeliveryResponse(savedDelivery);
    }

    // this method update delivery lifecycle method(ASSIGNED → PICKED_UP →
    // OUT_FOR_DELIVERY → DELIVERED)
    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long deliveryId, DeliveryStatusUpdateRequest request) {
        validateDeliveryId(deliveryId);
        validateDeliveryStatusUpdateRequest(request);

        log.info("Updating delivery status. deliveryId={}, newStatus={}", deliveryId, request.getDeliveryStatus());

        // Fetch delivery record that needs status update.
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(() -> {
            log.warn("Delivery status update failed because delivery was not found. deliveryId={}", deliveryId);
            return new RuntimeException("Delivery not found with id: " + deliveryId);
        });

        // fetch linked order as delivery status change also effect order status
        Order order = orderRepository.findById(delivery.getOrderId()).orElseThrow(() -> {
            log.warn("Delivery status update failed because order was not found. orderId={}, deliveryId={}",
                    delivery.getOrderId(), deliveryId);
            return new RuntimeException("Order not found with id: " + delivery.getOrderId());
        });

        DeliveryStatus previousStatus = delivery.getDeliveryStatus();

        DeliveryStatus newStatus = request.getDeliveryStatus();

        // update delivery status first
        delivery.setDeliveryStatus(newStatus);

        if (newStatus == DeliveryStatus.PICKED_UP) {
            delivery.setPickedUpAt(LocalDateTime.now()); //// Store pickup timestamp and move order to out-for-delivery
                                                            //// stage.
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);

            log.info("Delivery marked as PICKED_UP. deliveryId={}, orderId={}", deliveryId, order.getId());
        }

        if (newStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            delivery.setOutForDeliveryAt(LocalDateTime.now());//// Store out-for-delivery timestamp and keep order
                                                                //// status in delivery stage.
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);

            log.info("Delivery marked as OUT_FOR_DELIVERY. deliveryId={}, orderId={}", deliveryId, order.getId());
        }

        // Complete delivery, mark order delivered and free the delivery partner.
        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.DELIVERED);
            makePartnerAvailable(delivery.getDeliveryPartner());

            log.info("Delivery completed successfully. deliveryId={}, orderId={}, partnerId={}", deliveryId,
                    order.getId(), delivery.getDeliveryPartner().getId());
        }

        if (newStatus == DeliveryStatus.CANCELLED) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            makePartnerAvailable(delivery.getDeliveryPartner());

            log.warn("Delivery cancelled. deliveryId={}, orderId={}, partnerId={}", deliveryId, order.getId(),
                    delivery.getDeliveryPartner().getId());
        }

        orderRepository.save(order);

        Delivery savedDelivery = deliveryRepository.save(delivery);
        
        DeliveryStatusUpdatedEvent deliveryStatusUpdatedEvent = DeliveryStatusUpdatedEvent.builder()
                .deliveryId(savedDelivery.getId())
                .orderId(savedDelivery.getOrderId())
                .deliveryPartnerId(savedDelivery.getDeliveryPartner().getId())
                .partnerName(savedDelivery.getDeliveryPartner().getPartnerName())
                .previousStatus(previousStatus.name())
                .newStatus(savedDelivery.getDeliveryStatus().name())
                .orderStatus(order.getOrderStatus().name())
                .updatedAt(LocalDateTime.now())
                .build();

        kafkaEventProducer.publishDeliveryStatusUpdatedEvent(deliveryStatusUpdatedEvent);

        log.info("Delivery status updated successfully. deliveryId={}, orderId={}, status={}", savedDelivery.getId(),
                savedDelivery.getOrderId(), savedDelivery.getDeliveryStatus());

        return mapToDeliveryResponse(savedDelivery);
    }

    // this method is for fetching delivery details using orderId.
    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryByOrderId(Long orderId) {
        validateOrderId(orderId);

        log.info("Fetching delivery by orderId={}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId).orElseThrow(() -> {
            log.warn("Delivery not found for orderId={}", orderId);
            return new RuntimeException("Delivery not found for order id: " + orderId);
        });

        log.info("Delivery fetched successfully. deliveryId={}, orderId={}", delivery.getId(), orderId);

        // Return delivery details in response DTO format for frontend tracking.
        return mapToDeliveryResponse(delivery);
    }

    // This method fetches delivery details using deliveryId
    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(Long deliveryId) {
        validateDeliveryId(deliveryId);

        log.info("Fetching delivery by deliveryId={}", deliveryId);

        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(() -> {
            log.warn("Delivery not found with deliveryId={}", deliveryId);
            return new RuntimeException("Delivery not found with id: " + deliveryId);
        });

        log.info("Delivery fetched successfully. deliveryId={}, orderId={}", delivery.getId(), delivery.getOrderId());

        return mapToDeliveryResponse(delivery);
    }

    // It fetches only the currently active deliveries assigned to the logged-in
    // delivery partner.
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getActiveDeliveriesByPartnerUserId(Long userId) {

        //// Delivery partner dashboard requires logged-in user's auth-service user id.
        if (userId == null) {
            log.warn("Active deliveries fetch failed because userId is null");
            throw new RuntimeException("User id is required");
        }

        log.info("Fetching active deliveries for delivery partner userId={}", userId);

        // Resolve delivery partner profile using auth-service user id.
        DeliveryPartner partner = deliveryPartnerRepository.findByUserId(userId).orElseThrow(() -> {
            log.warn("Delivery partner profile not found for userId={}", userId);
            return new RuntimeException("Delivery partner profile not found for user id: " + userId);
        });

        // Only active/in-progress delivery statuses should be shown on delivery
        // dashboard.
        List<DeliveryStatus> activeStatuses = List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP,
                DeliveryStatus.OUT_FOR_DELIVERY);

        List<Delivery> activeDeliveries = deliveryRepository
                .findByDeliveryPartner_IdAndDeliveryStatusInOrderByAssignedAtDesc(partner.getId(), activeStatuses);

        log.info("Active deliveries fetched successfully. userId={}, partnerId={}, activeDeliveryCount={}", userId,
                partner.getId(), activeDeliveries.size());

        return activeDeliveries.stream().map(this::mapToDeliveryResponse).toList();
    }

    // this method is a validation helper for creating a delivery partner profile.
    private void validateDeliveryPartnerRequest(DeliveryPartnerRequest request) {
        if (request == null) {
            log.warn("Create delivery partner request failed because request body is null");
            throw new RuntimeException("Request body cannot be null");
        }

        if (request.getUserId() == null) {
            log.warn("Create delivery partner request failed because userId is null");
            throw new RuntimeException("User id is required");
        }

        if (request.getPartnerName() == null || request.getPartnerName().trim().isEmpty()) {
            log.warn("Create delivery partner request failed because partnerName is blank. userId={}",
                    request.getUserId());
            throw new RuntimeException("Partner name is required");
        }

        if (request.getCurrentLatitude() == null) {
            log.warn("Create delivery partner request failed because currentLatitude is null. userId={}",
                    request.getUserId());
            throw new RuntimeException("Current latitude is required");
        }

        if (request.getCurrentLongitude() == null) {
            log.warn("Create delivery partner request failed because currentLongitude is null. userId={}",
                    request.getUserId());
            throw new RuntimeException("Current longitude is required");
        }
    }

    // validaiton helper for assigning delivery partner
    private void validateOrderForDeliveryAssignment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("Delivery assignment validation failed because order is not paid. orderId={}, paymentStatus={}",
                    order.getId(), order.getPaymentStatus());
            throw new RuntimeException("Order must be paid before delivery assignment");
        }

        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            log.warn("Delivery assignment validation failed because order is not confirmed. orderId={}, orderStatus={}",
                    order.getId(), order.getOrderStatus());
            throw new RuntimeException("Only confirmed orders can be assigned for delivery");
        }

        if (order.getDeliveryLatitude() == null || order.getDeliveryLongitude() == null) {
            log.warn("Delivery assignment validation failed because delivery location is missing. orderId={}",
                    order.getId());
            throw new RuntimeException("Order delivery location is missing");
        }
    }

    // this method will find the best delivery partner, it will check
    // availability,rating,
    // distance from restaurant using longitude and latitude along with the number
    // of order delivered.
    private DeliveryPartner findBestAvailablePartner() {
        List<DeliveryPartner> availablePartners = deliveryPartnerRepository
                .findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(PartnerStatus.AVAILABLE);

        log.info("Available delivery partners found for assignment. count={}", availablePartners.size());

        if (availablePartners.isEmpty()) {
            log.warn("No available delivery partner found during assignment");
            throw new RuntimeException("No available delivery partner found");
        }

        DeliveryPartner selectedPartner = availablePartners.stream()
                .min(Comparator.comparingInt(this::getSafeActiveDeliveryCount)
                        .thenComparingDouble(this::calculatePartnerDistanceFromRestaurant)
                        .thenComparing(Comparator.comparingDouble(this::getSafeRating).reversed()))
                .orElseThrow(() -> {
                    log.warn("No available delivery partner found after applying assignment comparator");
                    return new RuntimeException("No available delivery partner found");
                });

        log.info("Best available partner resolved. partnerId={}, activeDeliveryCount={}, rating={}",
                selectedPartner.getId(), getSafeActiveDeliveryCount(selectedPartner), getSafeRating(selectedPartner));

        return selectedPartner;
    }

    // method to calculate partner distance from restaurant
    private double calculatePartnerDistanceFromRestaurant(DeliveryPartner partner) {
        if (partner.getCurrentLatitude() == null || partner.getCurrentLongitude() == null) {
            log.debug("Partner location missing while calculating distance. partnerId={}", partner.getId());
            return Double.MAX_VALUE;
        }

        // calls calculateDistanceKm method here this is wrapper/helper function
        return calculateDistanceKm(RESTAURANT_LATITUDE, RESTAURANT_LONGITUDE, partner.getCurrentLatitude(),
                partner.getCurrentLongitude());
    }

    // this method is used to release the delivery partner after delivery is
    // completed or cancelled.
    private void makePartnerAvailable(DeliveryPartner partner) {
        int updatedActiveDeliveryCount = Math.max(0, getSafeActiveDeliveryCount(partner) - 1); // max of two

        partner.setActiveDeliveryCount(updatedActiveDeliveryCount);
        partner.setPartnerStatus(PartnerStatus.AVAILABLE);

        deliveryPartnerRepository.save(partner);

        log.info("Delivery partner marked available. partnerId={}, activeDeliveryCount={}", partner.getId(),
                partner.getActiveDeliveryCount());
    }

    private int getSafeActiveDeliveryCount(DeliveryPartner partner) {
        // Treat missing active delivery count as zero for assignment and availability
        // logic.
        return partner.getActiveDeliveryCount() == null ? 0 : partner.getActiveDeliveryCount();
    }

    private double getSafeRating(DeliveryPartner partner) {
        return partner.getRating() == null ? 0.0 : partner.getRating();
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            log.warn("Delivery operation failed because orderId is null");
            throw new RuntimeException("Order id is required");
        }
    }

    private void validateDeliveryId(Long deliveryId) {
        if (deliveryId == null) {
            log.warn("Delivery operation failed because deliveryId is null");
            throw new RuntimeException("Delivery id is required");
        }
    }

    private void validateDeliveryStatusUpdateRequest(DeliveryStatusUpdateRequest request) {
        if (request == null || request.getDeliveryStatus() == null) {
            log.warn("Delivery status update request failed because delivery status is missing");
            throw new RuntimeException("Delivery status is required");
        }
    }

    // this method calculates distance in kilometers between two latitude/longitude
    // points using the Haversine formula.
    private double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int earthRadiusKm = 6371;

        // Convert latitude and longitude differences from degrees to radians.
        double latitudeDifference = Math.toRadians(lat2 - lat1);
        double longitudeDifference = Math.toRadians(lon2 - lon1);

        double firstLatitude = Math.toRadians(lat1);
        double secondLatitude = Math.toRadians(lat2);

        // Haversine formula intermediate value.
        double haversineValue = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude) * Math.sin(longitudeDifference / 2)
                        * Math.sin(longitudeDifference / 2);

        // Angular distance between the two points on Earth's surface.
        double angularDistance = 2 * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));

        // Final distance in kilometers
        double distanceKm = earthRadiusKm * angularDistance;

        log.debug("Distance calculated using haversine formula. distanceKm={}", distanceKm);

        return distanceKm;
    }

    private int calculateEtaMinutes(double distanceKm) {
        int etaMinutes = (int) Math.ceil(distanceKm * 4); // upper limit value i.e 43.1 => 44;
        int finalEtaMinutes = Math.max(10, etaMinutes); // Final ETA should never be less than 10 minutes.

        log.debug("ETA calculated. distanceKm={}, etaMinutes={}", distanceKm, finalEtaMinutes);

        return finalEtaMinutes;
    }

    private double round(double value) { // roundoff a double value upto 2 decimal places.
        return Math.round(value * 100.0) / 100.0;
    }

    // entity(database object) -> DTO mapping(object sent to the frontend)
    private DeliveryPartnerResponse mapToDeliveryPartnerResponse(DeliveryPartner partner) {
        return DeliveryPartnerResponse.builder().deliveryPartnerId(partner.getId()).userId(partner.getUserId())
                .partnerName(partner.getPartnerName()).phone(partner.getPhone())
                .vehicleNumber(partner.getVehicleNumber()).partnerStatus(partner.getPartnerStatus())
                .currentLatitude(partner.getCurrentLatitude()).currentLongitude(partner.getCurrentLongitude())
                .activeDeliveryCount(partner.getActiveDeliveryCount()).rating(partner.getRating()).build();
    }

    // entity -> DTO mapping
    private DeliveryResponse mapToDeliveryResponse(Delivery delivery) {
        DeliveryPartner partner = delivery.getDeliveryPartner();

        return DeliveryResponse.builder().deliveryId(delivery.getId()).orderId(delivery.getOrderId())
                .deliveryPartnerId(partner.getId()).partnerName(partner.getPartnerName()).phone(partner.getPhone())
                .vehicleNumber(partner.getVehicleNumber()).deliveryStatus(delivery.getDeliveryStatus())
                .pickupLatitude(delivery.getPickupLatitude()).pickupLongitude(delivery.getPickupLongitude())
                .dropLatitude(delivery.getDropLatitude()).dropLongitude(delivery.getDropLongitude())
                .distanceKm(delivery.getDistanceKm()).estimatedTimeMinutes(delivery.getEstimatedTimeMinutes())
                .assignedAt(delivery.getAssignedAt()).pickedUpAt(delivery.getPickedUpAt())
                .outForDeliveryAt(delivery.getOutForDeliveryAt()).deliveredAt(delivery.getDeliveredAt()).build();
    }
}