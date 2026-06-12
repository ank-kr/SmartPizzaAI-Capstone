package com.smartpizza.core.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;

    public PaymentResponse payOrder(Long orderId, PaymentRequest request) {
        validateOrderId(orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        validateOrderForPayment(order);

        PaymentGateway paymentGateway = resolvePaymentGateway(request);
        String paymentMethod = resolvePaymentMethod(request);

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .paymentGateway(paymentGateway)
                .gatewayOrderId(generateDummyGatewayOrderId(order.getId()))
                .gatewayPaymentId(generateDummyGatewayPaymentId(order.getId()))
                .amount(order.getFinalAmount())
                .currency("INR")
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentMethod(paymentMethod)
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        tryAutoAssignDelivery(order.getId());

        return mapToPaymentResponse(savedPayment, "Payment completed successfully");
    }

    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        validateOrderId(orderId);

        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(payment -> mapToPaymentResponse(payment, "Payment record fetched successfully"))
                .toList();
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        validateUserId(userId);

        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(payment -> mapToPaymentResponse(payment, "Payment record fetched successfully"))
                .toList();
    }

    private void tryAutoAssignDelivery(Long orderId) {
        try {
            deliveryService.assignDeliveryPartner(orderId);
        } catch (RuntimeException exception) {
            System.out.println("Delivery auto-assignment skipped for order id "
                    + orderId
                    + ". Reason: "
                    + exception.getMessage());
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("Order id is required");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
    }

    private void validateOrderForPayment(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Order is already paid");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled order cannot be paid");
        }

        if (order.getFinalAmount() == null) {
            throw new RuntimeException("Order final amount is missing");
        }
    }

    private PaymentGateway resolvePaymentGateway(PaymentRequest request) {
        if (request == null || request.getPaymentGateway() == null) {
            return PaymentGateway.DUMMY;
        }

        return request.getPaymentGateway();
    }

    private String resolvePaymentMethod(PaymentRequest request) {
        if (request == null || request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            return "DUMMY";
        }

        return request.getPaymentMethod().trim().toUpperCase();
    }

    private String generateDummyGatewayOrderId(Long orderId) {
        return "DUMMY_ORDER_" + orderId + "_" + System.currentTimeMillis();
    }

    private String generateDummyGatewayPaymentId(Long orderId) {
        return "DUMMY_PAYMENT_" + orderId + "_" + System.currentTimeMillis();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentGateway(payment.getPaymentGateway())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionStatus(payment.getTransactionStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
                .message(message)
                .build();
    }
}

