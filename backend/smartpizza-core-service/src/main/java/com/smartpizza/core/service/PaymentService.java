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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;
	private final DeliveryService deliveryService;

	public PaymentResponse payOrder(Long orderId, PaymentRequest request) {

		// Order id is mandatory for payment processing.
		validateOrderId(orderId);

		log.info("Starting payment process for orderId={}", orderId);

		// Fetch order before payment.
		Order order = orderRepository.findById(orderId).orElseThrow(() -> {
			log.warn("Payment failed because order was not found. orderId={}", orderId);
			return new RuntimeException("Order not found with id: " + orderId);
		});

		// Validate order state before allowing payment.
		validateOrderForPayment(order);

		// Resolve payment inputs or fallback to default (dummy implementation).
		PaymentGateway paymentGateway = resolvePaymentGateway(request);
		String paymentMethod = resolvePaymentMethod(request);

		log.info("Payment details resolved. orderId={}, userId={}, gateway={}, method={}", order.getId(),
				order.getUserId(), paymentGateway, paymentMethod);

		// Create payment record (dummy gateway simulation).
		Payment payment = Payment.builder().orderId(order.getId()).userId(order.getUserId())
				.paymentGateway(paymentGateway)

				// Simulated gateway identifiers for demo.
				.gatewayOrderId(generateDummyGatewayOrderId(order.getId()))
				.gatewayPaymentId(generateDummyGatewayPaymentId(order.getId()))

				.amount(order.getFinalAmount()).currency("INR").transactionStatus(TransactionStatus.SUCCESS)
				.paymentMethod(paymentMethod).paidAt(LocalDateTime.now()).build();

		Payment savedPayment = paymentRepository.save(payment);

		log.info("Payment record saved successfully. paymentId={}, orderId={}, userId={}, amount={}, status={}",
				savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getUserId(), savedPayment.getAmount(),
				savedPayment.getTransactionStatus());

		// Update order lifecycle after successful payment.
		order.setPaymentStatus(PaymentStatus.PAID);
		order.setOrderStatus(OrderStatus.CONFIRMED);
		orderRepository.save(order);

		log.info("Order status updated after payment. orderId={}, paymentStatus={}, orderStatus={}", order.getId(),
				order.getPaymentStatus(), order.getOrderStatus());

		// Trigger delivery assignment after payment confirmation.
		tryAutoAssignDelivery(order.getId());

		return mapToPaymentResponse(savedPayment, "Payment completed successfully");
	}

	public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {

		validateOrderId(orderId);

		log.info("Fetching payment records for orderId={}", orderId);

		// Fetch all payment records for a given order (latest first).
		List<PaymentResponse> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
				.map(payment -> mapToPaymentResponse(payment, "Payment record fetched successfully")).toList();

		log.info("Payment records fetched successfully for orderId={}, count={}", orderId, payments.size());

		return payments;
	}

	public List<PaymentResponse> getPaymentsByUserId(Long userId) {

		validateUserId(userId);

		log.info("Fetching payment history for userId={}", userId);

		// Fetch all payment history for a user.
		List<PaymentResponse> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(payment -> mapToPaymentResponse(payment, "Payment record fetched successfully")).toList();

		log.info("Payment history fetched successfully for userId={}, count={}", userId, payments.size());

		return payments;
	}

	private void tryAutoAssignDelivery(Long orderId) {
		try {
			// Attempt automatic delivery assignment after payment.
			deliveryService.assignDeliveryPartner(orderId);

			log.info("Delivery auto-assignment triggered successfully for orderId={}", orderId);
		} catch (RuntimeException exception) {

			// Do not fail payment flow if delivery assignment fails.
			log.warn("Delivery auto-assignment skipped for orderId={}. Reason={}", orderId, exception.getMessage());
		}
	}

	private void validateOrderId(Long orderId) {
		if (orderId == null) {
			log.warn("Payment operation failed because orderId is null");
			throw new RuntimeException("Order id is required");
		}
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			log.warn("Payment operation failed because userId is null");
			throw new RuntimeException("User id is required");
		}
	}

	private void validateOrderForPayment(Order order) {

		// Prevent duplicate payment.
		if (order.getPaymentStatus() == PaymentStatus.PAID) {
			log.warn("Payment rejected because order is already paid. orderId={}", order.getId());
			throw new RuntimeException("Order is already paid");
		}

		// Cancelled orders should not proceed with payment.
		if (order.getOrderStatus() == OrderStatus.CANCELLED) {
			log.warn("Payment rejected because order is cancelled. orderId={}", order.getId());
			throw new RuntimeException("Cancelled order cannot be paid");
		}

		// Ensure amount is present before payment.
		if (order.getFinalAmount() == null) {
			log.warn("Payment rejected because final amount is missing. orderId={}", order.getId());
			throw new RuntimeException("Order final amount is missing");
		}
	}

	private PaymentGateway resolvePaymentGateway(PaymentRequest request) {

		// Default to DUMMY gateway if not provided (for demo use case).
		if (request == null || request.getPaymentGateway() == null) {
			log.debug("Payment gateway not provided. Falling back to DUMMY gateway.");
			return PaymentGateway.DUMMY;
		}

		return request.getPaymentGateway();
	}

	private String resolvePaymentMethod(PaymentRequest request) {

		// Default payment method if not provided.
		if (request == null || request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
			log.debug("Payment method not provided. Falling back to DUMMY method.");
			return "DUMMY";
		}

		return request.getPaymentMethod().trim().toUpperCase();
	}

	private String generateDummyGatewayOrderId(Long orderId) {

		// Simulate external payment gateway order id.
		String gatewayOrderId = "DUMMY_ORDER_" + orderId + "_" + System.currentTimeMillis();

		log.debug("Dummy gateway order id generated for orderId={}", orderId);

		return gatewayOrderId;
	}

	private String generateDummyGatewayPaymentId(Long orderId) {

		// Simulate external payment gateway payment id.
		String gatewayPaymentId = "DUMMY_PAYMENT_" + orderId + "_" + System.currentTimeMillis();

		log.debug("Dummy gateway payment id generated for orderId={}", orderId);

		return gatewayPaymentId;
	}

	private PaymentResponse mapToPaymentResponse(Payment payment, String message) {

		// Entity -> DTO mapping for payment response.
		return PaymentResponse.builder().paymentId(payment.getId()).orderId(payment.getOrderId())
				.userId(payment.getUserId()).paymentGateway(payment.getPaymentGateway())
				.gatewayOrderId(payment.getGatewayOrderId()).gatewayPaymentId(payment.getGatewayPaymentId())
				.amount(payment.getAmount()).currency(payment.getCurrency())
				.transactionStatus(payment.getTransactionStatus()).paymentMethod(payment.getPaymentMethod())
				.paidAt(payment.getPaidAt()).message(message).build();
	}
}
