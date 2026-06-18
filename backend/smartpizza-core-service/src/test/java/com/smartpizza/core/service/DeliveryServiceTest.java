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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void createDeliveryPartner_ShouldCreatePartnerSuccessfully() {
        DeliveryPartnerRequest request = DeliveryPartnerRequest.builder()
                .userId(10L)
                .partnerName("Karan Delivery")
                .phone("8437338473")
                .vehicleNumber("KA-02-23-00")
                .partnerStatus(PartnerStatus.AVAILABLE)
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .activeDeliveryCount(0)
                .rating(4.8)
                .build();

        DeliveryPartner savedPartner = DeliveryPartner.builder()
                .id(1L)
                .userId(10L)
                .partnerName("Karan Delivery")
                .phone("8437338473")
                .vehicleNumber("KA-02-23-00")
                .partnerStatus(PartnerStatus.AVAILABLE)
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .activeDeliveryCount(0)
                .rating(4.8)
                .build();

        when(deliveryPartnerRepository.existsByUserId(10L)).thenReturn(false);
        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenReturn(savedPartner);

        DeliveryPartnerResponse response = deliveryService.createDeliveryPartner(request);

        assertNotNull(response);
        assertEquals(1L, response.getDeliveryPartnerId());
        assertEquals(10L, response.getUserId());
        assertEquals("Karan Delivery", response.getPartnerName());
        assertEquals("8437338473", response.getPhone());
        assertEquals("KA-02-23-00", response.getVehicleNumber());
        assertEquals(PartnerStatus.AVAILABLE, response.getPartnerStatus());
        assertEquals(0, response.getActiveDeliveryCount());
        assertEquals(4.8, response.getRating());

        verify(deliveryPartnerRepository, times(1)).existsByUserId(10L);
        verify(deliveryPartnerRepository, times(1)).save(any(DeliveryPartner.class));
    }

    @Test
    void createDeliveryPartner_ShouldThrowException_WhenRequestIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.createDeliveryPartner(null)
        );

        assertEquals("Request body cannot be null", exception.getMessage());

        verifyNoInteractions(deliveryPartnerRepository);
    }

    @Test
    void createDeliveryPartner_ShouldThrowException_WhenUserIdIsNull() {
        DeliveryPartnerRequest request = DeliveryPartnerRequest.builder()
                .partnerName("Karan Delivery")
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .build();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.createDeliveryPartner(request)
        );

        assertEquals("User id is required", exception.getMessage());

        verifyNoInteractions(deliveryPartnerRepository);
    }

    @Test
    void createDeliveryPartner_ShouldThrowException_WhenPartnerAlreadyExists() {
        DeliveryPartnerRequest request = DeliveryPartnerRequest.builder()
                .userId(10L)
                .partnerName("Karan Delivery")
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .build();

        when(deliveryPartnerRepository.existsByUserId(10L)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.createDeliveryPartner(request)
        );

        assertEquals("Delivery partner already exists for user id: 10", exception.getMessage());

        verify(deliveryPartnerRepository, times(1)).existsByUserId(10L);
        verify(deliveryPartnerRepository, never()).save(any(DeliveryPartner.class));
    }

    @Test
    void getAllDeliveryPartners_ShouldReturnPartnerList() {
        DeliveryPartner partner = createAvailablePartner();

        when(deliveryPartnerRepository.findAll()).thenReturn(List.of(partner));

        List<DeliveryPartnerResponse> responses = deliveryService.getAllDeliveryPartners();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Karan Delivery", responses.get(0).getPartnerName());
        assertEquals(PartnerStatus.AVAILABLE, responses.get(0).getPartnerStatus());

        verify(deliveryPartnerRepository, times(1)).findAll();
    }

    @Test
    void assignDeliveryPartner_ShouldAssignAvailablePartnerSuccessfully() {
        Long orderId = 20L;

        Order order = createPaidConfirmedOrder(orderId);
        DeliveryPartner partner = createAvailablePartner();

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(deliveryPartnerRepository.findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(
                PartnerStatus.AVAILABLE
        )).thenReturn(List.of(partner));

        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(1L);
            return delivery;
        });

        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.assignDeliveryPartner(orderId);

        assertNotNull(response);
        assertEquals(1L, response.getDeliveryId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(1L, response.getDeliveryPartnerId());
        assertEquals("Karan Delivery", response.getPartnerName());
        assertEquals(DeliveryStatus.ASSIGNED, response.getDeliveryStatus());
        assertNotNull(response.getDistanceKm());
        assertNotNull(response.getEstimatedTimeMinutes());

        assertEquals(PartnerStatus.BUSY, partner.getPartnerStatus());
        assertEquals(1, partner.getActiveDeliveryCount());
        assertEquals(OrderStatus.ASSIGNED_TO_DELIVERY, order.getOrderStatus());

        verify(deliveryRepository, times(1)).findByOrderId(orderId);
        verify(orderRepository, times(1)).findById(orderId);
        verify(deliveryPartnerRepository, times(1))
                .findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(PartnerStatus.AVAILABLE);
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        verify(deliveryPartnerRepository, times(1)).save(partner);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void assignDeliveryPartner_ShouldThrowException_WhenDeliveryAlreadyAssigned() {
        Long orderId = 20L;

        Delivery existingDelivery = Delivery.builder()
                .id(1L)
                .orderId(orderId)
                .deliveryStatus(DeliveryStatus.ASSIGNED)
                .build();

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingDelivery));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.assignDeliveryPartner(orderId)
        );

        assertEquals("Delivery already assigned for order id: 20", exception.getMessage());

        verify(deliveryRepository, times(1)).findByOrderId(orderId);
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    void assignDeliveryPartner_ShouldThrowException_WhenOrderNotFound() {
        Long orderId = 20L;

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.assignDeliveryPartner(orderId)
        );

        assertEquals("Order not found with id: 20", exception.getMessage());

        verify(deliveryRepository, times(1)).findByOrderId(orderId);
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void assignDeliveryPartner_ShouldThrowException_WhenOrderPaymentIsNotPaid() {
        Long orderId = 20L;

        Order order = createPaidConfirmedOrder(orderId);
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.assignDeliveryPartner(orderId)
        );

        assertEquals("Order must be paid before delivery assignment", exception.getMessage());

        verify(deliveryPartnerRepository, never())
                .findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(any());
    }

    @Test
    void assignDeliveryPartner_ShouldThrowException_WhenOrderIsNotConfirmed() {
        Long orderId = 20L;

        Order order = createPaidConfirmedOrder(orderId);
        order.setOrderStatus(OrderStatus.DELIVERED);

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.assignDeliveryPartner(orderId)
        );

        assertEquals("Only confirmed orders can be assigned for delivery", exception.getMessage());

        verify(deliveryPartnerRepository, never())
                .findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(any());
    }

    @Test
    void assignDeliveryPartner_ShouldThrowException_WhenNoAvailablePartnerFound() {
        Long orderId = 20L;

        Order order = createPaidConfirmedOrder(orderId);

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(deliveryPartnerRepository.findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(
                PartnerStatus.AVAILABLE
        )).thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.assignDeliveryPartner(orderId)
        );

        assertEquals("No available delivery partner found", exception.getMessage());

        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void assignDeliveryPartner_ShouldChooseNearestPartner_WhenMultiplePartnersAvailable() {
        Long orderId = 20L;

        Order order = createPaidConfirmedOrder(orderId);

        DeliveryPartner farPartner = DeliveryPartner.builder()
                .id(1L)
                .userId(10L)
                .partnerName("Far Partner")
                .phone("1111111111")
                .vehicleNumber("KA-01-AA-1111")
                .partnerStatus(PartnerStatus.AVAILABLE)
                .currentLatitude(13.1000)
                .currentLongitude(77.8000)
                .activeDeliveryCount(0)
                .rating(4.9)
                .build();

        DeliveryPartner nearPartner = DeliveryPartner.builder()
                .id(2L)
                .userId(11L)
                .partnerName("Near Partner")
                .phone("2222222222")
                .vehicleNumber("KA-02-BB-2222")
                .partnerStatus(PartnerStatus.AVAILABLE)
                .currentLatitude(12.9360)
                .currentLongitude(77.6250)
                .activeDeliveryCount(0)
                .rating(4.5)
                .build();

        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(deliveryPartnerRepository.findByPartnerStatusOrderByActiveDeliveryCountAscRatingDesc(
                PartnerStatus.AVAILABLE
        )).thenReturn(List.of(farPartner, nearPartner));

        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(1L);
            return delivery;
        });

        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.assignDeliveryPartner(orderId);

        assertNotNull(response);
        assertEquals(2L, response.getDeliveryPartnerId());
        assertEquals("Near Partner", response.getPartnerName());
        assertEquals(PartnerStatus.BUSY, nearPartner.getPartnerStatus());
        assertEquals(PartnerStatus.AVAILABLE, farPartner.getPartnerStatus());
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateToPickedUp() {
        DeliveryPartner partner = createBusyPartner();
        Order order = createAssignedOrder(20L);
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        DeliveryStatusUpdateRequest request = DeliveryStatusUpdateRequest.builder()
                .deliveryStatus(DeliveryStatus.PICKED_UP)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.updateDeliveryStatus(1L, request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.PICKED_UP, response.getDeliveryStatus());
        assertNotNull(delivery.getPickedUpAt());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getOrderStatus());

        verify(orderRepository, times(1)).save(order);
        verify(deliveryRepository, times(1)).save(delivery);
        verify(deliveryPartnerRepository, never()).save(any(DeliveryPartner.class));
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateToOutForDelivery() {
        DeliveryPartner partner = createBusyPartner();
        Order order = createAssignedOrder(20L);
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        DeliveryStatusUpdateRequest request = DeliveryStatusUpdateRequest.builder()
                .deliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.updateDeliveryStatus(1L, request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, response.getDeliveryStatus());
        assertNotNull(delivery.getOutForDeliveryAt());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getOrderStatus());

        verify(orderRepository, times(1)).save(order);
        verify(deliveryRepository, times(1)).save(delivery);
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateToDelivered_AndMakePartnerAvailable() {
        DeliveryPartner partner = createBusyPartner();
        Order order = createAssignedOrder(20L);
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        DeliveryStatusUpdateRequest request = DeliveryStatusUpdateRequest.builder()
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.updateDeliveryStatus(1L, request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.DELIVERED, response.getDeliveryStatus());
        assertNotNull(delivery.getDeliveredAt());
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        assertEquals(PartnerStatus.AVAILABLE, partner.getPartnerStatus());
        assertEquals(0, partner.getActiveDeliveryCount());

        verify(deliveryPartnerRepository, times(1)).save(partner);
        verify(orderRepository, times(1)).save(order);
        verify(deliveryRepository, times(1)).save(delivery);
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateToCancelled_AndMakePartnerAvailable() {
        DeliveryPartner partner = createBusyPartner();
        Order order = createAssignedOrder(20L);
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        DeliveryStatusUpdateRequest request = DeliveryStatusUpdateRequest.builder()
                .deliveryStatus(DeliveryStatus.CANCELLED)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.updateDeliveryStatus(1L, request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.CANCELLED, response.getDeliveryStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(PartnerStatus.AVAILABLE, partner.getPartnerStatus());
        assertEquals(0, partner.getActiveDeliveryCount());

        verify(deliveryPartnerRepository, times(1)).save(partner);
    }

    @Test
    void updateDeliveryStatus_ShouldThrowException_WhenDeliveryNotFound() {
        DeliveryStatusUpdateRequest request = DeliveryStatusUpdateRequest.builder()
                .deliveryStatus(DeliveryStatus.PICKED_UP)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.updateDeliveryStatus(1L, request)
        );

        assertEquals("Delivery not found with id: 1", exception.getMessage());

        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    void getDeliveryByOrderId_ShouldReturnDelivery() {
        DeliveryPartner partner = createBusyPartner();
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        when(deliveryRepository.findByOrderId(20L)).thenReturn(Optional.of(delivery));

        DeliveryResponse response = deliveryService.getDeliveryByOrderId(20L);

        assertNotNull(response);
        assertEquals(1L, response.getDeliveryId());
        assertEquals(20L, response.getOrderId());
        assertEquals("Karan Delivery", response.getPartnerName());

        verify(deliveryRepository, times(1)).findByOrderId(20L);
    }

    @Test
    void getDeliveryByOrderId_ShouldThrowException_WhenDeliveryNotFound() {
        when(deliveryRepository.findByOrderId(20L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.getDeliveryByOrderId(20L)
        );

        assertEquals("Delivery not found for order id: 20", exception.getMessage());

        verify(deliveryRepository, times(1)).findByOrderId(20L);
    }

    @Test
    void getDeliveryById_ShouldReturnDelivery() {
        DeliveryPartner partner = createBusyPartner();
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        DeliveryResponse response = deliveryService.getDeliveryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getDeliveryId());
        assertEquals(20L, response.getOrderId());

        verify(deliveryRepository, times(1)).findById(1L);
    }

    @Test
    void getActiveDeliveriesByPartnerUserId_ShouldReturnActiveDeliveries() {
        DeliveryPartner partner = createBusyPartner();
        Delivery delivery = createAssignedDelivery(1L, 20L, partner);

        when(deliveryPartnerRepository.findByUserId(10L)).thenReturn(Optional.of(partner));
        when(deliveryRepository.findByDeliveryPartner_IdAndDeliveryStatusInOrderByAssignedAtDesc(
                eq(1L),
                anyList()
        )).thenReturn(List.of(delivery));

        List<DeliveryResponse> responses = deliveryService.getActiveDeliveriesByPartnerUserId(10L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(20L, responses.get(0).getOrderId());
        assertEquals(DeliveryStatus.ASSIGNED, responses.get(0).getDeliveryStatus());

        verify(deliveryPartnerRepository, times(1)).findByUserId(10L);
        verify(deliveryRepository, times(1))
                .findByDeliveryPartner_IdAndDeliveryStatusInOrderByAssignedAtDesc(eq(1L), anyList());
    }

    @Test
    void getActiveDeliveriesByPartnerUserId_ShouldThrowException_WhenPartnerProfileNotFound() {
        when(deliveryPartnerRepository.findByUserId(10L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deliveryService.getActiveDeliveriesByPartnerUserId(10L)
        );

        assertEquals("Delivery partner profile not found for user id: 10", exception.getMessage());

        verify(deliveryPartnerRepository, times(1)).findByUserId(10L);
        verify(deliveryRepository, never())
                .findByDeliveryPartner_IdAndDeliveryStatusInOrderByAssignedAtDesc(anyLong(), anyList());
    }

    private DeliveryPartner createAvailablePartner() {
        return DeliveryPartner.builder()
                .id(1L)
                .userId(10L)
                .partnerName("Karan Delivery")
                .phone("8437338473")
                .vehicleNumber("KA-02-23-00")
                .partnerStatus(PartnerStatus.AVAILABLE)
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .activeDeliveryCount(0)
                .rating(4.8)
                .build();
    }

    private DeliveryPartner createBusyPartner() {
        return DeliveryPartner.builder()
                .id(1L)
                .userId(10L)
                .partnerName("Karan Delivery")
                .phone("8437338473")
                .vehicleNumber("KA-02-23-00")
                .partnerStatus(PartnerStatus.BUSY)
                .currentLatitude(12.9345)
                .currentLongitude(77.6238)
                .activeDeliveryCount(1)
                .rating(4.8)
                .build();
    }

    private Order createPaidConfirmedOrder(Long orderId) {
        return Order.builder()
                .id(orderId)
                .userId(1L)
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .subtotal(BigDecimal.valueOf(399))
                .discountAmount(BigDecimal.valueOf(79.8))
                .taxAmount(BigDecimal.valueOf(15.96))
                .deliveryCharge(BigDecimal.valueOf(40))
                .finalAmount(BigDecimal.valueOf(375.16))
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .orderTime(LocalDateTime.now())
                .build();
    }

    private Order createAssignedOrder(Long orderId) {
        return Order.builder()
                .id(orderId)
                .userId(1L)
                .orderStatus(OrderStatus.ASSIGNED_TO_DELIVERY)
                .paymentStatus(PaymentStatus.PAID)
                .subtotal(BigDecimal.valueOf(399))
                .discountAmount(BigDecimal.valueOf(79.8))
                .taxAmount(BigDecimal.valueOf(15.96))
                .deliveryCharge(BigDecimal.valueOf(40))
                .finalAmount(BigDecimal.valueOf(375.16))
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .orderTime(LocalDateTime.now())
                .build();
    }

    private Delivery createAssignedDelivery(Long deliveryId, Long orderId, DeliveryPartner partner) {
        return Delivery.builder()
                .id(deliveryId)
                .orderId(orderId)
                .deliveryPartner(partner)
                .deliveryStatus(DeliveryStatus.ASSIGNED)
                .pickupLatitude(12.9352)
                .pickupLongitude(77.6245)
                .dropLatitude(12.8452)
                .dropLongitude(77.6602)
                .distanceKm(10.73)
                .estimatedTimeMinutes(43)
                .assignedAt(LocalDateTime.now())
                .build();
    }
}