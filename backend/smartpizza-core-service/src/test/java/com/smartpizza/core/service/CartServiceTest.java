package com.smartpizza.core.service;

import com.smartpizza.core.dto.AddToCartRequest;
import com.smartpizza.core.dto.CartResponse;
import com.smartpizza.core.dto.UpdateCartItemRequest;
import com.smartpizza.core.entity.Cart;
import com.smartpizza.core.entity.CartItem;
import com.smartpizza.core.entity.MenuItem;
import com.smartpizza.core.repository.CartItemRepository;
import com.smartpizza.core.repository.CartRepository;
import com.smartpizza.core.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCart_WhenNewItem_ShouldAddItemAndReturnUpdatedCart() {
        Long userId = 1L;
        Long cartId = 10L;
        Long menuItemId = 100L;

        AddToCartRequest request = AddToCartRequest.builder()
                .userId(userId)
                .menuItemId(menuItemId)
                .quantity(2)
                .build();

        MenuItem menuItem = MenuItem.builder()
                .id(menuItemId)
                .name("Farmhouse Pizza")
                .price(BigDecimal.valueOf(299))
                .available(true)
                .build();

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .cartItems(new ArrayList<>())
                .build();

        List<CartItem> cartItems = new ArrayList<>();

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndMenuItemId(cartId, menuItemId)).thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem savedItem = invocation.getArgument(0);
            savedItem.setId(1L);
            cartItems.add(savedItem);
            return savedItem;
        });

        when(cartItemRepository.findByCartId(cartId)).thenReturn(cartItems);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addToCart(request);

        assertNotNull(response);
        assertEquals(cartId, response.getCartId());
        assertEquals(userId, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals("Farmhouse Pizza", response.getItems().get(0).getItemName());
        assertEquals(2, response.getItems().get(0).getQuantity());
        assertEquals(0, BigDecimal.valueOf(598).compareTo(response.getTotalAmount()));

        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(cartRepository, times(2)).findByUserId(userId);
        verify(cartItemRepository, times(1)).findByCartIdAndMenuItemId(cartId, menuItemId);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void addToCart_WhenExistingItem_ShouldIncreaseQuantityAndReturnUpdatedCart() {
        Long userId = 1L;
        Long cartId = 10L;
        Long menuItemId = 100L;

        AddToCartRequest request = AddToCartRequest.builder()
                .userId(userId)
                .menuItemId(menuItemId)
                .quantity(2)
                .build();

        MenuItem menuItem = MenuItem.builder()
                .id(menuItemId)
                .name("Margherita Pizza")
                .price(BigDecimal.valueOf(199))
                .available(true)
                .build();

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.valueOf(199))
                .cartItems(new ArrayList<>())
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .menuItem(menuItem)
                .itemName("Margherita Pizza")
                .price(BigDecimal.valueOf(199))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(199))
                .build();

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(existingCartItem);

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndMenuItemId(cartId, menuItemId))
                .thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);
        when(cartItemRepository.findByCartId(cartId)).thenReturn(cartItems);
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.addToCart(request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(3, response.getItems().get(0).getQuantity());
        assertEquals(0, BigDecimal.valueOf(597).compareTo(response.getItems().get(0).getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(597).compareTo(response.getTotalAmount()));

        verify(cartItemRepository, times(1)).save(existingCartItem);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void addToCart_WhenMenuItemNotAvailable_ShouldThrowException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .userId(1L)
                .menuItemId(100L)
                .quantity(1)
                .build();

        MenuItem menuItem = MenuItem.builder()
                .id(100L)
                .name("Coke")
                .price(BigDecimal.valueOf(80))
                .available(false)
                .build();

        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(request);
        });

        assertEquals("Menu item is currently not available", exception.getMessage());

        verify(menuItemRepository, times(1)).findById(100L);
        verify(cartRepository, never()).findByUserId(anyLong());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_WhenQuantityIsZero_ShouldThrowException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .userId(1L)
                .menuItemId(100L)
                .quantity(0)
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(request);
        });

        assertEquals("Quantity must be greater than zero", exception.getMessage());

        verify(menuItemRepository, never()).findById(anyLong());
        verify(cartRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getCartByUserId_WhenCartExists_ShouldReturnCartResponse() {
        Long userId = 1L;
        Long cartId = 10L;

        MenuItem menuItem = MenuItem.builder()
                .id(100L)
                .name("Veg Pizza")
                .price(BigDecimal.valueOf(250))
                .available(true)
                .build();

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.valueOf(500))
                .cartItems(new ArrayList<>())
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .menuItem(menuItem)
                .itemName("Veg Pizza")
                .price(BigDecimal.valueOf(250))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(500))
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(cartItem));

        CartResponse response = cartService.getCartByUserId(userId);

        assertNotNull(response);
        assertEquals(cartId, response.getCartId());
        assertEquals(userId, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals("Veg Pizza", response.getItems().get(0).getItemName());
        assertEquals(2, response.getItems().get(0).getQuantity());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(response.getTotalAmount()));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartItemRepository, times(1)).findByCartId(cartId);
    }

    @Test
    void getCartByUserId_WhenCartDoesNotExist_ShouldCreateCartAndReturnEmptyCart() {
        Long userId = 1L;

        Cart newCart = Cart.builder()
                .id(10L)
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .cartItems(new ArrayList<>())
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        CartResponse response = cartService.getCartByUserId(userId);

        assertNotNull(response);
        assertEquals(10L, response.getCartId());
        assertEquals(userId, response.getUserId());
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount()));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartItemRepository, times(1)).findByCartId(10L);
    }

    @Test
    void updateCartItem_WhenCartItemExists_ShouldUpdateQuantityAndReturnCart() {
        Long cartItemId = 1L;
        Long userId = 1L;
        Long cartId = 10L;

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();

        MenuItem menuItem = MenuItem.builder()
                .id(100L)
                .name("Paneer Pizza")
                .price(BigDecimal.valueOf(300))
                .available(true)
                .build();

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.valueOf(300))
                .cartItems(new ArrayList<>())
                .build();

        CartItem cartItem = CartItem.builder()
                .id(cartItemId)
                .cart(cart)
                .menuItem(menuItem)
                .itemName("Paneer Pizza")
                .price(BigDecimal.valueOf(300))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(300))
                .build();

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(cartItem));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateCartItem(cartItemId, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(3, response.getItems().get(0).getQuantity());
        assertEquals(0, BigDecimal.valueOf(900).compareTo(response.getItems().get(0).getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(900).compareTo(response.getTotalAmount()));

        verify(cartItemRepository, times(1)).findById(cartItemId);
        verify(cartItemRepository, times(1)).save(cartItem);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void updateCartItem_WhenCartItemNotFound_ShouldThrowException() {
        Long cartItemId = 99L;

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.updateCartItem(cartItemId, request);
        });

        assertEquals("Cart item not found with id: 99", exception.getMessage());

        verify(cartItemRepository, times(1)).findById(cartItemId);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void removeCartItem_WhenCartItemExists_ShouldDeleteItemAndReturnUpdatedCart() {
        Long cartItemId = 1L;
        Long userId = 1L;
        Long cartId = 10L;

        MenuItem menuItem = MenuItem.builder()
                .id(100L)
                .name("Coke")
                .price(BigDecimal.valueOf(80))
                .available(true)
                .build();

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.valueOf(80))
                .cartItems(new ArrayList<>())
                .build();

        CartItem cartItem = CartItem.builder()
                .id(cartItemId)
                .cart(cart)
                .menuItem(menuItem)
                .itemName("Coke")
                .price(BigDecimal.valueOf(80))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(80))
                .build();

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of());
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.removeCartItem(cartItemId);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount()));

        verify(cartItemRepository, times(1)).findById(cartItemId);
        verify(cartItemRepository, times(1)).delete(cartItem);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void clearCart_WhenCartExists_ShouldDeleteAllCartItemsAndReturnEmptyCart() {
        Long userId = 1L;
        Long cartId = 10L;

        Cart cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .totalAmount(BigDecimal.valueOf(500))
                .cartItems(new ArrayList<>())
                .build();

        CartItem cartItem1 = CartItem.builder()
                .id(1L)
                .cart(cart)
                .itemName("Pizza")
                .price(BigDecimal.valueOf(250))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(250))
                .build();

        CartItem cartItem2 = CartItem.builder()
                .id(2L)
                .cart(cart)
                .itemName("Burger")
                .price(BigDecimal.valueOf(250))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(250))
                .build();

        List<CartItem> cartItems = List.of(cartItem1, cartItem2);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId))
                .thenReturn(cartItems)
                .thenReturn(List.of());
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.clearCart(userId);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount()));

        verify(cartRepository, times(2)).findByUserId(userId);
        verify(cartItemRepository, times(1)).deleteAll(cartItems);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void clearCart_WhenUserIdIsNull_ShouldThrowException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.clearCart(null);
        });

        assertEquals("User id is required", exception.getMessage());

        verify(cartRepository, never()).findByUserId(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }
}