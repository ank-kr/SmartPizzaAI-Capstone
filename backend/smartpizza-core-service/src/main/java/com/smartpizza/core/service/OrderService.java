package com.smartpizza.core.service;

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
import com.smartpizza.core.repository.CartItemRepository;
import com.smartpizza.core.repository.CartRepository;
import com.smartpizza.core.repository.CouponRepository;
import com.smartpizza.core.repository.OrderItemRepository;
import com.smartpizza.core.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(40);
    private static final BigDecimal FREE_DELIVERY_LIMIT = BigDecimal.valueOf(500);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        validatePlaceOrderRequest(request);

        Cart cart = cartRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user id: " + request.getUserId()));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty. Please add items before placing order.");
        }

        BigDecimal subtotal = calculateSubtotal(cartItems);
        BigDecimal discountAmount = calculateDiscountAmount(request.getCouponCode(), subtotal);
        BigDecimal amountAfterDiscount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = calculateTax(amountAfterDiscount);
        BigDecimal deliveryCharge = calculateDeliveryCharge(amountAfterDiscount);
        BigDecimal finalAmount = amountAfterDiscount.add(taxAmount).add(deliveryCharge);

        Order order = Order.builder()
                .userId(request.getUserId())
                .couponCode(normalizeCouponCode(request.getCouponCode()))
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(scale(subtotal))
                .discountAmount(scale(discountAmount))
                .taxAmount(scale(taxAmount))
                .deliveryCharge(scale(deliveryCharge))
                .finalAmount(scale(finalAmount))
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .orderTime(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> createOrderItem(savedOrder, cartItem))
                .toList();

        orderItemRepository.saveAll(orderItems);

        clearCart(cart, cartItems);

        return getOrderById(savedOrder.getId());
    }

    public OrderResponse getOrderById(Long orderId) {
        validateOrderId(orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        return mapToOrderResponse(order, itemResponses);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        validateUserId(userId);

        return orderRepository.findByUserIdOrderByOrderTimeDesc(userId)
                .stream()
                .map(order -> {
                    List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(this::mapToOrderItemResponse)
                            .toList();

                    return mapToOrderResponse(order, itemResponses);
                })
                .toList();
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderTimeDesc()
                .stream()
                .map(order -> {
                    List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(this::mapToOrderItemResponse)
                            .toList();

                    return mapToOrderResponse(order, itemResponses);
                })
                .toList();
    }

    private void validatePlaceOrderRequest(PlaceOrderRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        validateUserId(request.getUserId());

        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("Delivery address is required");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("Order id is required");
        }
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDiscountAmount(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String normalizedCode = couponCode.trim().toUpperCase();

        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code: " + normalizedCode));

        validateCoupon(coupon, subtotal);

        BigDecimal discountAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
            discountAmount = coupon.getMaxDiscount();
        }

        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        return scale(discountAmount);
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new RuntimeException("Coupon is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new RuntimeException("Coupon is not active yet");
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new RuntimeException("Coupon has expired");
        }

        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Minimum order amount required is " + coupon.getMinOrderAmount());
        }
    }

    private BigDecimal calculateTax(BigDecimal amountAfterDiscount) {
        return scale(amountAfterDiscount.multiply(TAX_RATE));
    }

    private BigDecimal calculateDeliveryCharge(BigDecimal amountAfterDiscount) {
        if (amountAfterDiscount.compareTo(FREE_DELIVERY_LIMIT) >= 0) {
            return BigDecimal.ZERO;
        }

        return DELIVERY_CHARGE;
    }

    private OrderItem createOrderItem(Order order, CartItem cartItem) {
        return OrderItem.builder()
                .order(order)
                .menuItemId(cartItem.getMenuItem().getId())
                .itemName(cartItem.getItemName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(cartItem.getSubtotal())
                .build();
    }

    private void clearCart(Cart cart, List<CartItem> cartItems) {
        cartItemRepository.deleteAll(cartItems);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return null;
        }

        return couponCode.trim().toUpperCase();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .menuItemId(orderItem.getMenuItemId())
                .itemName(orderItem.getItemName())
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getSubtotal())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order, List<OrderItemResponse> itemResponses) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .couponCode(order.getCouponCode())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .finalAmount(order.getFinalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLatitude(order.getDeliveryLatitude())
                .deliveryLongitude(order.getDeliveryLongitude())
                .orderTime(order.getOrderTime())
                .items(itemResponses)
                .build();
    }
}