package com.smartpizza.core.service;

import com.smartpizza.core.dto.PaymentRequest;
import com.smartpizza.core.dto.PaymentResponse;
import com.smartpizza.core.entity.Order;
import com.smartpizza.core.entity.Payment;
import com.smartpizza.core.enums.OrderStatus;
import com.smartpizza.core.enums.PaymentGateway;
import com.smartpizza.core.enums.PaymentStatus;
import com.smartpizza.core.enums.TransactionStatus;
import com.smartpizza.core.repository.OrderRepository;
import com.smartpizza.core.repository.PaymentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void payOrder_ShouldCompletePaymentSuccessfully_WithGivenRequest() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("upi")
                .build();

        Payment savedPayment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .userId(1L)
                .paymentGateway(PaymentGateway.DUMMY)
                .gatewayOrderId("DUMMY_ORDER_20_123")
                .gatewayPaymentId("DUMMY_PAYMENT_20_123")
                .amount(BigDecimal.valueOf(458.95))
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod("UPI")
                .paidAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.payOrder(orderId, request);

        assertNotNull(response);
        assertEquals(1L, response.getPaymentId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(1L, response.getUserId());
        assertEquals(PaymentGateway.DUMMY, response.getPaymentGateway());
        assertEquals(BigDecimal.valueOf(458.95), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(TransactionStatus.SUCCESS, response.getTransactionStatus());
        assertEquals("UPI", response.getPaymentMethod());
        assertEquals("Payment completed successfully", response.getMessage());

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());

        verify(orderRepository, times(1)).findById(orderId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(order);
        verify(deliveryService, times(1)).assignDeliveryPartner(orderId);
    }

    @Test
    void payOrder_ShouldUseDefaultGatewayAndMethod_WhenRequestIsNull() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);

        Payment savedPayment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .userId(1L)
                .paymentGateway(PaymentGateway.DUMMY)
                .gatewayOrderId("DUMMY_ORDER_20_123")
                .gatewayPaymentId("DUMMY_PAYMENT_20_123")
                .amount(BigDecimal.valueOf(458.95))
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod("DUMMY")
                .paidAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.payOrder(orderId, null);

        assertNotNull(response);
        assertEquals(PaymentGateway.DUMMY, response.getPaymentGateway());
        assertEquals("DUMMY", response.getPaymentMethod());
        assertEquals(TransactionStatus.SUCCESS, response.getTransactionStatus());

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());

        verify(deliveryService, times(1)).assignDeliveryPartner(orderId);
    }

    @Test
    void payOrder_ShouldUseDefaultPaymentMethod_WhenPaymentMethodIsBlank() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("   ")
                .build();

        Payment savedPayment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .userId(1L)
                .paymentGateway(PaymentGateway.DUMMY)
                .gatewayOrderId("DUMMY_ORDER_20_123")
                .gatewayPaymentId("DUMMY_PAYMENT_20_123")
                .amount(BigDecimal.valueOf(458.95))
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod("DUMMY")
                .paidAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.payOrder(orderId, request);

        assertNotNull(response);
        assertEquals("DUMMY", response.getPaymentMethod());

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(order);
        verify(deliveryService, times(1)).assignDeliveryPartner(orderId);
    }

    @Test
    void payOrder_ShouldStillCompletePayment_WhenDeliveryAutoAssignmentFails() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("CARD")
                .build();

        Payment savedPayment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .userId(1L)
                .paymentGateway(PaymentGateway.DUMMY)
                .gatewayOrderId("DUMMY_ORDER_20_123")
                .gatewayPaymentId("DUMMY_PAYMENT_20_123")
                .amount(BigDecimal.valueOf(458.95))
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod("CARD")
                .paidAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("No available delivery partner found"))
                .when(deliveryService)
                .assignDeliveryPartner(orderId);

        PaymentResponse response = paymentService.payOrder(orderId, request);

        assertNotNull(response);
        assertEquals("Payment completed successfully", response.getMessage());
        assertEquals(TransactionStatus.SUCCESS, response.getTransactionStatus());

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());

        verify(deliveryService, times(1)).assignDeliveryPartner(orderId);
        verify(orderRepository, times(1)).save(order);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void payOrder_ShouldThrowException_WhenOrderIdIsNull() {
        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("UPI")
                .build();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.payOrder(null, request)
        );

        assertEquals("Order id is required", exception.getMessage());

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(deliveryService);
    }

    @Test
    void payOrder_ShouldThrowException_WhenOrderNotFound() {
        Long orderId = 99L;

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("UPI")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.payOrder(orderId, request)
        );

        assertEquals("Order not found with id: 99", exception.getMessage());

        verify(orderRepository, times(1)).findById(orderId);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(deliveryService, never()).assignDeliveryPartner(anyLong());
    }

    @Test
    void payOrder_ShouldThrowException_WhenOrderAlreadyPaid() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);
        order.setPaymentStatus(PaymentStatus.PAID);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("UPI")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.payOrder(orderId, request)
        );

        assertEquals("Order is already paid", exception.getMessage());

        verify(orderRepository, times(1)).findById(orderId);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(deliveryService, never()).assignDeliveryPartner(anyLong());
    }

    @Test
    void payOrder_ShouldThrowException_WhenOrderIsCancelled() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);
        order.setOrderStatus(OrderStatus.CANCELLED);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("UPI")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.payOrder(orderId, request)
        );

        assertEquals("Cancelled order cannot be paid", exception.getMessage());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(deliveryService, never()).assignDeliveryPartner(anyLong());
    }

    @Test
    void payOrder_ShouldThrowException_WhenFinalAmountIsMissing() {
        Long orderId = 20L;

        Order order = createPendingOrder(orderId);
        order.setFinalAmount(null);

        PaymentRequest request = PaymentRequest.builder()
                .paymentGateway(PaymentGateway.DUMMY)
                .paymentMethod("UPI")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.payOrder(orderId, request)
        );

        assertEquals("Order final amount is missing", exception.getMessage());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(deliveryService, never()).assignDeliveryPartner(anyLong());
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnPaymentsForOrder() {
        Long orderId = 20L;

        Payment payment1 = createPayment(1L, orderId, 1L, "UPI");
        Payment payment2 = createPayment(2L, orderId, 1L, "CARD");

        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of(payment2, payment1));

        List<PaymentResponse> responses = paymentService.getPaymentsByOrderId(orderId);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(2L, responses.get(0).getPaymentId());
        assertEquals("CARD", responses.get(0).getPaymentMethod());
        assertEquals("Payment record fetched successfully", responses.get(0).getMessage());

        assertEquals(1L, responses.get(1).getPaymentId());
        assertEquals("UPI", responses.get(1).getPaymentMethod());

        verify(paymentRepository, times(1)).findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Test
    void getPaymentsByOrderId_ShouldThrowException_WhenOrderIdIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.getPaymentsByOrderId(null)
        );

        assertEquals("Order id is required", exception.getMessage());

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPaymentsByUserId_ShouldReturnPaymentsForUser() {
        Long userId = 1L;

        Payment payment1 = createPayment(1L, 20L, userId, "UPI");
        Payment payment2 = createPayment(2L, 21L, userId, "CARD");

        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(payment2, payment1));

        List<PaymentResponse> responses = paymentService.getPaymentsByUserId(userId);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(2L, responses.get(0).getPaymentId());
        assertEquals(21L, responses.get(0).getOrderId());
        assertEquals("CARD", responses.get(0).getPaymentMethod());
        assertEquals("Payment record fetched successfully", responses.get(0).getMessage());

        assertEquals(1L, responses.get(1).getPaymentId());
        assertEquals(20L, responses.get(1).getOrderId());
        assertEquals("UPI", responses.get(1).getPaymentMethod());

        verify(paymentRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getPaymentsByUserId_ShouldThrowException_WhenUserIdIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.getPaymentsByUserId(null)
        );

        assertEquals("User id is required", exception.getMessage());

        verifyNoInteractions(paymentRepository);
    }

    private Order createPendingOrder(Long orderId) {
        return Order.builder()
                .id(orderId)
                .userId(1L)
                .couponCode(null)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(BigDecimal.valueOf(399.00))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.valueOf(19.95))
                .deliveryCharge(BigDecimal.valueOf(40.00))
                .finalAmount(BigDecimal.valueOf(458.95))
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .orderTime(LocalDateTime.now())
                .build();
    }

    private Payment createPayment(Long paymentId, Long orderId, Long userId, String paymentMethod) {
        return Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(userId)
                .paymentGateway(PaymentGateway.DUMMY)
                .gatewayOrderId("DUMMY_ORDER_" + orderId + "_123")
                .gatewayPaymentId("DUMMY_PAYMENT_" + orderId + "_123")
                .amount(BigDecimal.valueOf(458.95))
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod(paymentMethod)
                .paidAt(LocalDateTime.now())
                .build();
    }
}