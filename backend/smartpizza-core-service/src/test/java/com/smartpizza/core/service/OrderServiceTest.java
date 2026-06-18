package com.smartpizza.core.service;

import com.smartpizza.core.dto.OrderResponse;
import com.smartpizza.core.dto.PlaceOrderRequest;
import com.smartpizza.core.entity.Cart;
import com.smartpizza.core.entity.CartItem;
import com.smartpizza.core.entity.Coupon;
import com.smartpizza.core.entity.MenuItem;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_ShouldPlaceOrderSuccessfully_WithoutCoupon() {
        PlaceOrderRequest request = createPlaceOrderRequest(null);

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(399), 1)
        );

        Order savedOrder = createPendingOrder(31L, null);
        OrderItem orderItem = createOrderItem(101L, savedOrder, cartItems.get(0));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(orderItem));

        when(orderRepository.findById(31L)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(31L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(31L, response.getOrderId());
        assertEquals(1L, response.getUserId());
        assertNull(response.getCouponCode());
        assertEquals(OrderStatus.PAYMENT_PENDING, response.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());

        assertEquals(0, BigDecimal.valueOf(399.00).compareTo(response.getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(0.00).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(19.95).compareTo(response.getTaxAmount()));
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(response.getDeliveryCharge()));
        assertEquals(0, BigDecimal.valueOf(458.95).compareTo(response.getFinalAmount()));

        assertEquals("Electronic City, Bengaluru", response.getDeliveryAddress());
        assertEquals(1, response.getItems().size());
        assertEquals("Farmhouse Pizza", response.getItems().get(0).getItemName());

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartItemRepository, times(1)).findByCartId(10L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
        verify(cartItemRepository, times(1)).deleteAll(cartItems);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void placeOrder_ShouldApplyPercentageCouponSuccessfully() {
        PlaceOrderRequest request = createPlaceOrderRequest("pizza20");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(399), 1)
        );

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("PIZZA20")
                .description("20 percent discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(200))
                .maxDiscount(BigDecimal.valueOf(100))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .active(true)
                .build();

        Order savedOrder = createPendingOrder(31L, "PIZZA20");
        savedOrder.setSubtotal(BigDecimal.valueOf(399.00).setScale(2));
        savedOrder.setDiscountAmount(BigDecimal.valueOf(79.80).setScale(2));
        savedOrder.setTaxAmount(BigDecimal.valueOf(15.96).setScale(2));
        savedOrder.setDeliveryCharge(BigDecimal.valueOf(40.00).setScale(2));
        savedOrder.setFinalAmount(BigDecimal.valueOf(375.16).setScale(2));

        OrderItem orderItem = createOrderItem(101L, savedOrder, cartItems.get(0));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(orderItem));

        when(orderRepository.findById(31L)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(31L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals("PIZZA20", response.getCouponCode());
        assertEquals(0, BigDecimal.valueOf(399.00).compareTo(response.getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(79.80).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(15.96).compareTo(response.getTaxAmount()));
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(response.getDeliveryCharge()));
        assertEquals(0, BigDecimal.valueOf(375.16).compareTo(response.getFinalAmount()));

        verify(couponRepository, times(1)).findByCodeIgnoreCase("PIZZA20");
        verify(cartItemRepository, times(1)).deleteAll(cartItems);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void placeOrder_ShouldApplyMaxDiscount_WhenPercentageDiscountExceedsMaxDiscount() {
        PlaceOrderRequest request = createPlaceOrderRequest("PIZZA50");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(1000), 1)
        );

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("PIZZA50")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(50))
                .minOrderAmount(BigDecimal.valueOf(200))
                .maxDiscount(BigDecimal.valueOf(200))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .active(true)
                .build();

        Order savedOrder = createPendingOrder(32L, "PIZZA50");
        savedOrder.setSubtotal(BigDecimal.valueOf(1000.00).setScale(2));
        savedOrder.setDiscountAmount(BigDecimal.valueOf(200.00).setScale(2));
        savedOrder.setTaxAmount(BigDecimal.valueOf(40.00).setScale(2));
        savedOrder.setDeliveryCharge(BigDecimal.ZERO.setScale(2));
        savedOrder.setFinalAmount(BigDecimal.valueOf(840.00).setScale(2));

        OrderItem orderItem = createOrderItem(102L, savedOrder, cartItems.get(0));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("PIZZA50")).thenReturn(Optional.of(coupon));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(orderItem));

        when(orderRepository.findById(32L)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(32L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(response.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(response.getTaxAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getDeliveryCharge()));
        assertEquals(0, BigDecimal.valueOf(840.00).compareTo(response.getFinalAmount()));
    }

    @Test
    void placeOrder_ShouldGiveFreeDelivery_WhenAmountAfterDiscountGreaterThanOrEqual500() {
        PlaceOrderRequest request = createPlaceOrderRequest(null);

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Large Farmhouse Pizza", BigDecimal.valueOf(600), 1)
        );

        Order savedOrder = createPendingOrder(33L, null);
        savedOrder.setSubtotal(BigDecimal.valueOf(600.00).setScale(2));
        savedOrder.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        savedOrder.setTaxAmount(BigDecimal.valueOf(30.00).setScale(2));
        savedOrder.setDeliveryCharge(BigDecimal.ZERO.setScale(2));
        savedOrder.setFinalAmount(BigDecimal.valueOf(630.00).setScale(2));

        OrderItem orderItem = createOrderItem(103L, savedOrder, cartItems.get(0));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(orderItem));

        when(orderRepository.findById(33L)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(33L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getDeliveryCharge()));
        assertEquals(0, BigDecimal.valueOf(630.00).compareTo(response.getFinalAmount()));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenRequestIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(null)
        );

        assertEquals("Request body cannot be null", exception.getMessage());

        verifyNoInteractions(cartRepository);
    }

    @Test
    void placeOrder_ShouldThrowException_WhenUserIdIsNull() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .build();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("User id is required", exception.getMessage());

        verifyNoInteractions(cartRepository);
    }

    @Test
    void placeOrder_ShouldThrowException_WhenDeliveryAddressIsBlank() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .userId(1L)
                .deliveryAddress(" ")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .build();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Delivery address is required", exception.getMessage());

        verifyNoInteractions(cartRepository);
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCartNotFound() {
        PlaceOrderRequest request = createPlaceOrderRequest(null);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Cart not found for user id: 1", exception.getMessage());

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartItemRepository, never()).findByCartId(anyLong());
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCartIsEmpty() {
        PlaceOrderRequest request = createPlaceOrderRequest(null);

        Cart cart = createCart();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Cart is empty. Please add items before placing order.", exception.getMessage());

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartItemRepository, times(1)).findByCartId(10L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCouponCodeInvalid() {
        PlaceOrderRequest request = createPlaceOrderRequest("INVALID");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(399), 1)
        );

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Invalid coupon code: INVALID", exception.getMessage());

        verify(couponRepository, times(1)).findByCodeIgnoreCase("INVALID");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCouponInactive() {
        PlaceOrderRequest request = createPlaceOrderRequest("PIZZA20");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(399), 1)
        );

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("PIZZA20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(200))
                .active(false)
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Coupon is not active", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCouponExpired() {
        PlaceOrderRequest request = createPlaceOrderRequest("OLD20");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Farmhouse Pizza", BigDecimal.valueOf(399), 1)
        );

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("OLD20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(200))
                .startDate(LocalDateTime.now().minusDays(10))
                .expiryDate(LocalDateTime.now().minusDays(1))
                .active(true)
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("OLD20")).thenReturn(Optional.of(coupon));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Coupon has expired", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenSubtotalLessThanCouponMinimumAmount() {
        PlaceOrderRequest request = createPlaceOrderRequest("PIZZA20");

        Cart cart = createCart();
        List<CartItem> cartItems = List.of(
                createCartItem(1L, 1L, "Coke", BigDecimal.valueOf(80), 1)
        );

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("PIZZA20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(200))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .active(true)
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(cartItems);
        when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Minimum order amount required is 200", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrderSuccessfully() {
        Order order = createPendingOrder(31L, null);

        OrderItem orderItem = OrderItem.builder()
                .id(101L)
                .order(order)
                .menuItemId(1L)
                .itemName("Farmhouse Pizza")
                .price(BigDecimal.valueOf(399))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(399))
                .build();

        when(orderRepository.findById(31L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(31L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.getOrderById(31L);

        assertNotNull(response);
        assertEquals(31L, response.getOrderId());
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals("Farmhouse Pizza", response.getItems().get(0).getItemName());

        verify(orderRepository, times(1)).findById(31L);
        verify(orderItemRepository, times(1)).findByOrderId(31L);
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.getOrderById(99L)
        );

        assertEquals("Order not found with id: 99", exception.getMessage());

        verify(orderRepository, times(1)).findById(99L);
        verify(orderItemRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void getOrdersByUserId_ShouldReturnOrdersForUser() {
        Order order1 = createPendingOrder(31L, null);
        Order order2 = createPendingOrder(32L, "PIZZA20");

        OrderItem orderItem1 = OrderItem.builder()
                .id(101L)
                .order(order1)
                .menuItemId(1L)
                .itemName("Farmhouse Pizza")
                .price(BigDecimal.valueOf(399))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(399))
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .id(102L)
                .order(order2)
                .menuItemId(2L)
                .itemName("Margherita Pizza")
                .price(BigDecimal.valueOf(299))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(299))
                .build();

        when(orderRepository.findByUserIdOrderByOrderTimeDesc(1L)).thenReturn(List.of(order2, order1));
        when(orderItemRepository.findByOrderId(32L)).thenReturn(List.of(orderItem2));
        when(orderItemRepository.findByOrderId(31L)).thenReturn(List.of(orderItem1));

        List<OrderResponse> responses = orderService.getOrdersByUserId(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(32L, responses.get(0).getOrderId());
        assertEquals(31L, responses.get(1).getOrderId());

        verify(orderRepository, times(1)).findByUserIdOrderByOrderTimeDesc(1L);
        verify(orderItemRepository, times(1)).findByOrderId(32L);
        verify(orderItemRepository, times(1)).findByOrderId(31L);
    }

    @Test
    void getOrdersByUserId_ShouldThrowException_WhenUserIdIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.getOrdersByUserId(null)
        );

        assertEquals("User id is required", exception.getMessage());

        verifyNoInteractions(orderRepository);
    }

    @Test
    void getAllOrdersForAdmin_ShouldReturnAllOrders() {
        Order order1 = createPendingOrder(31L, null);
        Order order2 = createPendingOrder(32L, "PIZZA20");

        OrderItem orderItem1 = OrderItem.builder()
                .id(101L)
                .order(order1)
                .menuItemId(1L)
                .itemName("Farmhouse Pizza")
                .price(BigDecimal.valueOf(399))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(399))
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .id(102L)
                .order(order2)
                .menuItemId(2L)
                .itemName("Margherita Pizza")
                .price(BigDecimal.valueOf(299))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(299))
                .build();

        when(orderRepository.findAllByOrderByOrderTimeDesc()).thenReturn(List.of(order2, order1));
        when(orderItemRepository.findByOrderId(32L)).thenReturn(List.of(orderItem2));
        when(orderItemRepository.findByOrderId(31L)).thenReturn(List.of(orderItem1));

        List<OrderResponse> responses = orderService.getAllOrdersForAdmin();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(32L, responses.get(0).getOrderId());
        assertEquals(31L, responses.get(1).getOrderId());

        verify(orderRepository, times(1)).findAllByOrderByOrderTimeDesc();
    }

    private PlaceOrderRequest createPlaceOrderRequest(String couponCode) {
        return PlaceOrderRequest.builder()
                .userId(1L)
                .couponCode(couponCode)
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .build();
    }

    private Cart createCart() {
        return Cart.builder()
                .id(10L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(399))
                .build();
    }

    private CartItem createCartItem(
            Long cartItemId,
            Long menuItemId,
            String itemName,
            BigDecimal price,
            Integer quantity
    ) {
        MenuItem menuItem = MenuItem.builder()
                .id(menuItemId)
                .name(itemName)
                .price(price)
                .build();

        return CartItem.builder()
                .id(cartItemId)
                .cart(createCart())
                .menuItem(menuItem)
                .itemName(itemName)
                .price(price)
                .quantity(quantity)
                .subtotal(price.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private Order createPendingOrder(Long orderId, String couponCode) {
        return Order.builder()
                .id(orderId)
                .userId(1L)
                .couponCode(couponCode)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(BigDecimal.valueOf(399.00).setScale(2))
                .discountAmount(BigDecimal.ZERO.setScale(2))
                .taxAmount(BigDecimal.valueOf(19.95).setScale(2))
                .deliveryCharge(BigDecimal.valueOf(40.00).setScale(2))
                .finalAmount(BigDecimal.valueOf(458.95).setScale(2))
                .deliveryAddress("Electronic City, Bengaluru")
                .deliveryLatitude(12.8452)
                .deliveryLongitude(77.6602)
                .orderTime(LocalDateTime.now())
                .build();
    }

    private OrderItem createOrderItem(Long orderItemId, Order order, CartItem cartItem) {
        return OrderItem.builder()
                .id(orderItemId)
                .order(order)
                .menuItemId(cartItem.getMenuItem().getId())
                .itemName(cartItem.getItemName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(cartItem.getSubtotal())
                .build();
    }
}