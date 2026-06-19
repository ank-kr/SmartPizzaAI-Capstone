package com.smartpizza.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartpizza.core.dto.OrderItemResponse;
import com.smartpizza.core.dto.OrderResponse;
import com.smartpizza.core.dto.PlaceOrderRequest;
import com.smartpizza.core.entity.Cart;
import com.smartpizza.core.entity.CartItem;
import com.smartpizza.core.entity.Coupon;
import com.smartpizza.core.entity.Order;
import com.smartpizza.core.entity.OrderItem;
import com.smartpizza.core.enums.DiscountType;
import com.smartpizza.core.enums.OrderStatus;
import com.smartpizza.core.enums.PaymentStatus;
import com.smartpizza.core.event.OrderPlacedEvent;
import com.smartpizza.core.kafka.KafkaEventProducer;
import com.smartpizza.core.repository.CartItemRepository;
import com.smartpizza.core.repository.CartRepository;
import com.smartpizza.core.repository.CouponRepository;
import com.smartpizza.core.repository.OrderItemRepository;
import com.smartpizza.core.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	// Business constants used in order price calculation logic.
	private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.05);
	private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(40);
	private static final BigDecimal FREE_DELIVERY_LIMIT = BigDecimal.valueOf(500);

	// Repository dependencies for cart, coupon, and order operations.
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final CouponRepository couponRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final KafkaEventProducer kafkaEventProducer;

	@Transactional // Ensures order placement is atomic (all DB ops succeed or rollback).
	public OrderResponse placeOrder(PlaceOrderRequest request) {

		validatePlaceOrderRequest(request);

		log.info("Placing order for userId={}", request.getUserId());

		// Fetch user's cart.
		Cart cart = cartRepository.findByUserId(request.getUserId()).orElseThrow(() -> {
			log.warn("Order placement failed because cart was not found. userId={}", request.getUserId());
			return new RuntimeException("Cart not found for user id: " + request.getUserId());
		});

		List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

		// Prevent placing order with empty cart.
		if (cartItems.isEmpty()) {
			log.warn("Order placement failed because cart is empty. cartId={}, userId={}", cart.getId(),
					request.getUserId());
			throw new RuntimeException("Cart is empty. Please add items before placing order.");
		}

		// Price calculation pipeline.
		BigDecimal subtotal = calculateSubtotal(cartItems);
		BigDecimal discountAmount = calculateDiscountAmount(request.getCouponCode(), subtotal);
		BigDecimal amountAfterDiscount = subtotal.subtract(discountAmount);
		BigDecimal taxAmount = calculateTax(amountAfterDiscount);
		BigDecimal deliveryCharge = calculateDeliveryCharge(amountAfterDiscount);
		BigDecimal finalAmount = amountAfterDiscount.add(taxAmount).add(deliveryCharge);

		log.info(
				"Order price calculated. userId={}, subtotal={}, discountAmount={}, taxAmount={}, deliveryCharge={}, finalAmount={}",
				request.getUserId(), scale(subtotal), scale(discountAmount), scale(taxAmount), scale(deliveryCharge),
				scale(finalAmount));

		// Create order with initial PAYMENT_PENDING state.
		Order order = Order.builder().userId(request.getUserId())
				.couponCode(normalizeCouponCode(request.getCouponCode())).orderStatus(OrderStatus.PAYMENT_PENDING)
				.paymentStatus(PaymentStatus.PENDING).subtotal(scale(subtotal)).discountAmount(scale(discountAmount))
				.taxAmount(scale(taxAmount)).deliveryCharge(scale(deliveryCharge)).finalAmount(scale(finalAmount))
				.deliveryAddress(request.getDeliveryAddress()).deliveryLatitude(request.getDeliveryLatitude())
				.deliveryLongitude(request.getDeliveryLongitude()).orderTime(LocalDateTime.now()).build();

		Order savedOrder = orderRepository.save(order);

		OrderPlacedEvent orderPlacedEvent = OrderPlacedEvent.builder().orderId(savedOrder.getId())
				.userId(savedOrder.getUserId()).finalAmount(savedOrder.getFinalAmount())
				.orderStatus(savedOrder.getOrderStatus().name()).paymentStatus(savedOrder.getPaymentStatus().name())
				.orderTime(savedOrder.getOrderTime()).build();

		kafkaEventProducer.publishOrderPlacedEvent(orderPlacedEvent);

		log.info("Order created successfully. orderId={}, userId={}, orderStatus={}, paymentStatus={}",
				savedOrder.getId(), savedOrder.getUserId(), savedOrder.getOrderStatus(), savedOrder.getPaymentStatus());

		// Convert cart items into order items.
		List<OrderItem> orderItems = cartItems.stream().map(cartItem -> createOrderItem(savedOrder, cartItem)).toList();

		orderItemRepository.saveAll(orderItems);

		log.info("Order items saved successfully. orderId={}, itemCount={}", savedOrder.getId(), orderItems.size());

		// Clear cart after successful order placement.
		clearCart(cart, cartItems);

		log.info("Order placement completed successfully. orderId={}, userId={}", savedOrder.getId(),
				savedOrder.getUserId());

		return getOrderById(savedOrder.getId());
	}

	public OrderResponse getOrderById(Long orderId) {

		validateOrderId(orderId);

		log.info("Fetching order by orderId={}", orderId);

		// Fetch order.
		Order order = orderRepository.findById(orderId).orElseThrow(() -> {
			log.warn("Order not found. orderId={}", orderId);
			return new RuntimeException("Order not found with id: " + orderId);
		});

		// Fetch and map order items.
		List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId()).stream()
				.map(this::mapToOrderItemResponse).toList();

		log.info("Order fetched successfully. orderId={}, userId={}, itemCount={}", order.getId(), order.getUserId(),
				itemResponses.size());

		return mapToOrderResponse(order, itemResponses);
	}

	public List<OrderResponse> getOrdersByUserId(Long userId) {

		validateUserId(userId);

		log.info("Fetching orders for userId={}", userId);

		// Fetch user orders (latest first).
		List<OrderResponse> orders = orderRepository.findByUserIdOrderByOrderTimeDesc(userId).stream().map(order -> {
			List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId()).stream()
					.map(this::mapToOrderItemResponse).toList();

			return mapToOrderResponse(order, itemResponses);
		}).toList();

		log.info("User orders fetched successfully. userId={}, orderCount={}", userId, orders.size());

		return orders;
	}

	public List<OrderResponse> getAllOrdersForAdmin() {

		log.info("Fetching all orders for admin");

		// Admin view: fetch all orders sorted by latest.
		List<OrderResponse> orders = orderRepository.findAllByOrderByOrderTimeDesc().stream().map(order -> {
			List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId()).stream()
					.map(this::mapToOrderItemResponse).toList();

			return mapToOrderResponse(order, itemResponses);
		}).toList();

		log.info("Admin orders fetched successfully. orderCount={}", orders.size());

		return orders;
	}

	private void validatePlaceOrderRequest(PlaceOrderRequest request) {

		if (request == null) {
			log.warn("Order placement failed because request body is null");
			throw new RuntimeException("Request body cannot be null");
		}

		validateUserId(request.getUserId());

		// Delivery address is mandatory for order placement.
		if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
			log.warn("Order placement failed because delivery address is missing. userId={}", request.getUserId());
			throw new RuntimeException("Delivery address is required");
		}
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			log.warn("Order operation failed because userId is null");
			throw new RuntimeException("User id is required");
		}
	}

	private void validateOrderId(Long orderId) {
		if (orderId == null) {
			log.warn("Order operation failed because orderId is null");
			throw new RuntimeException("Order id is required");
		}
	}

	private BigDecimal calculateSubtotal(List<CartItem> cartItems) {

		// Sum all cart item subtotals.
		BigDecimal subtotal = cartItems.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

		log.debug("Subtotal calculated. subtotal={}, itemCount={}", subtotal, cartItems.size());

		return subtotal;
	}

	private BigDecimal calculateDiscountAmount(String couponCode, BigDecimal subtotal) {

		// No coupon applied.
		if (couponCode == null || couponCode.trim().isEmpty()) {
			log.debug("No coupon applied. subtotal={}", subtotal);
			return BigDecimal.ZERO;
		}

		String normalizedCode = couponCode.trim().toUpperCase();

		log.info("Applying coupon during order placement. code={}, subtotal={}", normalizedCode, subtotal);

		// Fetch coupon.
		Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode).orElseThrow(() -> {
			log.warn("Order coupon validation failed because coupon code is invalid. code={}", normalizedCode);
			return new RuntimeException("Invalid coupon code: " + normalizedCode);
		});

		validateCoupon(coupon, subtotal);

		BigDecimal discountAmount;

		// Percentage vs flat discount logic.
		if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
			discountAmount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2,
					RoundingMode.HALF_UP);
		} else {
			discountAmount = coupon.getDiscountValue();
		}

		log.debug("Discount calculated before caps. code={}, discountType={}, discountAmount={}", coupon.getCode(),
				coupon.getDiscountType(), discountAmount);

		// Apply max discount cap if present.
		if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
			log.debug("Discount capped by maxDiscount. code={}, discountAmount={}, maxDiscount={}", coupon.getCode(),
					discountAmount, coupon.getMaxDiscount());
			discountAmount = coupon.getMaxDiscount();
		}

		// Prevent discount exceeding subtotal.
		if (discountAmount.compareTo(subtotal) > 0) {
			log.debug("Discount capped by subtotal. code={}, discountAmount={}, subtotal={}", coupon.getCode(),
					discountAmount, subtotal);
			discountAmount = subtotal;
		}

		return scale(discountAmount);
	}

	private void validateCoupon(Coupon coupon, BigDecimal subtotal) {

		if (!Boolean.TRUE.equals(coupon.getActive())) {
			log.warn("Coupon validation failed because coupon is inactive. code={}", coupon.getCode());
			throw new RuntimeException("Coupon is not active");
		}

		LocalDateTime now = LocalDateTime.now();

		// Validate coupon validity period.
		if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
			log.warn("Coupon validation failed because coupon is not active yet. code={}, startDate={}",
					coupon.getCode(), coupon.getStartDate());
			throw new RuntimeException("Coupon is not active yet");
		}

		if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
			log.warn("Coupon validation failed because coupon has expired. code={}, expiryDate={}", coupon.getCode(),
					coupon.getExpiryDate());
			throw new RuntimeException("Coupon has expired");
		}

		// Validate minimum order constraint.
		if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
			log.warn(
					"Coupon validation failed because subtotal is below minimum order amount. code={}, subtotal={}, minOrderAmount={}",
					coupon.getCode(), subtotal, coupon.getMinOrderAmount());
			throw new RuntimeException("Minimum order amount required is " + coupon.getMinOrderAmount());
		}
	}

	private BigDecimal calculateTax(BigDecimal amountAfterDiscount) {
		BigDecimal taxAmount = scale(amountAfterDiscount.multiply(TAX_RATE));

		log.debug("Tax calculated. amountAfterDiscount={}, taxRate={}, taxAmount={}", amountAfterDiscount, TAX_RATE,
				taxAmount);

		return taxAmount;
	}

	private BigDecimal calculateDeliveryCharge(BigDecimal amountAfterDiscount) {

		// Free delivery for orders above threshold.
		if (amountAfterDiscount.compareTo(FREE_DELIVERY_LIMIT) >= 0) {
			log.debug("Free delivery applied. amountAfterDiscount={}, freeDeliveryLimit={}", amountAfterDiscount,
					FREE_DELIVERY_LIMIT);
			return BigDecimal.ZERO;
		}

		log.debug("Delivery charge applied. amountAfterDiscount={}, deliveryCharge={}", amountAfterDiscount,
				DELIVERY_CHARGE);

		return DELIVERY_CHARGE;
	}

	private OrderItem createOrderItem(Order order, CartItem cartItem) {

		// Snapshot cart item into order item.
		return OrderItem.builder().order(order).menuItemId(cartItem.getMenuItem().getId())
				.itemName(cartItem.getItemName()).price(cartItem.getPrice()).quantity(cartItem.getQuantity())
				.subtotal(cartItem.getSubtotal()).build();
	}

	private void clearCart(Cart cart, List<CartItem> cartItems) {

		// Remove all cart items and reset total.
		cartItemRepository.deleteAll(cartItems);
		cart.setTotalAmount(BigDecimal.ZERO);
		cartRepository.save(cart);

		log.info("Cart cleared after order placement. cartId={}, userId={}, removedItemCount={}", cart.getId(),
				cart.getUserId(), cartItems.size());
	}

	private String normalizeCouponCode(String couponCode) {

		// Normalize coupon for consistent storage/search.
		if (couponCode == null || couponCode.trim().isEmpty()) {
			return null;
		}

		return couponCode.trim().toUpperCase();
	}

	private BigDecimal scale(BigDecimal value) {

		// Ensure currency values are stored with 2 decimal precision.
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {

		// Entity -> DTO mapping for order item.
		return OrderItemResponse.builder().orderItemId(orderItem.getId()).menuItemId(orderItem.getMenuItemId())
				.itemName(orderItem.getItemName()).price(orderItem.getPrice()).quantity(orderItem.getQuantity())
				.subtotal(orderItem.getSubtotal()).build();
	}

	private OrderResponse mapToOrderResponse(Order order, List<OrderItemResponse> itemResponses) {

		// Entity -> DTO mapping for order with aggregated item details.
		return OrderResponse.builder().orderId(order.getId()).userId(order.getUserId())
				.couponCode(order.getCouponCode()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).subtotal(order.getSubtotal())
				.discountAmount(order.getDiscountAmount()).taxAmount(order.getTaxAmount())
				.deliveryCharge(order.getDeliveryCharge()).finalAmount(order.getFinalAmount())
				.deliveryAddress(order.getDeliveryAddress()).deliveryLatitude(order.getDeliveryLatitude())
				.deliveryLongitude(order.getDeliveryLongitude()).orderTime(order.getOrderTime()).items(itemResponses)
				.build();
	}
}