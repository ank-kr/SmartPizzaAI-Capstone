package com.smartpizza.core.service;

import com.smartpizza.core.dto.AddToCartRequest;
import com.smartpizza.core.dto.CartItemResponse;
import com.smartpizza.core.dto.CartResponse;
import com.smartpizza.core.dto.UpdateCartItemRequest;
import com.smartpizza.core.entity.Cart;
import com.smartpizza.core.entity.CartItem;
import com.smartpizza.core.entity.MenuItem;
import com.smartpizza.core.repository.CartItemRepository;
import com.smartpizza.core.repository.CartRepository;
import com.smartpizza.core.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;

    public CartResponse addToCart(AddToCartRequest request) {
        validateAddToCartRequest(request);

        MenuItem menuItem = getMenuItem(request.getMenuItemId());
        Cart cart = getOrCreateCart(request.getUserId());

        CartItem cartItem = cartItemRepository
                .findByCartIdAndMenuItemId(cart.getId(), menuItem.getId())
                .orElse(null);

        if (cartItem == null) {
            cartItem = createCartItem(cart, menuItem, request.getQuantity());
        } else {
            updateExistingCartItem(cartItem, request.getQuantity());
        }

        cartItemRepository.save(cartItem);
        updateCartTotal(cart);

        return getCartByUserId(request.getUserId());
    }

    public CartResponse getCartByUserId(Long userId) {
        validateUserId(userId);

        Cart cart = getOrCreateCart(userId);

        List<CartItemResponse> itemResponses = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(this::mapToCartItemResponse)
                .toList();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .totalAmount(cart.getTotalAmount())
                .items(itemResponses)
                .build();
    }

    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        validateCartItemId(cartItemId);
        validateUpdateCartItemRequest(request);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found with id: " + cartItemId));

        cartItem.setQuantity(request.getQuantity());
        cartItem.setSubtotal(calculateSubtotal(cartItem.getPrice(), request.getQuantity()));

        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        updateCartTotal(cart);

        return getCartByUserId(cart.getUserId());
    }

    public CartResponse removeCartItem(Long cartItemId) {
        validateCartItemId(cartItemId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found with id: " + cartItemId));

        Cart cart = cartItem.getCart();
        Long userId = cart.getUserId();

        cartItemRepository.delete(cartItem);
        updateCartTotal(cart);

        return getCartByUserId(userId);
    }

    public CartResponse clearCart(Long userId) {
        validateUserId(userId);

        Cart cart = getOrCreateCart(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        cartItemRepository.deleteAll(cartItems);

        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        return getCartByUserId(userId);
    }

    private void validateAddToCartRequest(AddToCartRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        validateUserId(request.getUserId());

        if (request.getMenuItemId() == null) {
            throw new RuntimeException("Menu item id is required");
        }

        validateQuantity(request.getQuantity());
    }

    private void validateUpdateCartItemRequest(UpdateCartItemRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        validateQuantity(request.getQuantity());
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User id is required");
        }
    }

    private void validateCartItemId(Long cartItemId) {
        if (cartItemId == null) {
            throw new RuntimeException("Cart item id is required");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
    }

    private MenuItem getMenuItem(Long menuItemId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found with id: " + menuItemId));

        if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
            throw new RuntimeException("Menu item is currently not available");
        }

        return menuItem;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .cartItems(new ArrayList<>())
                .build();

        return cartRepository.save(cart);
    }

    private CartItem createCartItem(Cart cart, MenuItem menuItem, Integer quantity) {
        return CartItem.builder()
                .cart(cart)
                .menuItem(menuItem)
                .itemName(menuItem.getName())
                .price(menuItem.getPrice())
                .quantity(quantity)
                .subtotal(calculateSubtotal(menuItem.getPrice(), quantity))
                .build();
    }

    private void updateExistingCartItem(CartItem cartItem, Integer quantityToAdd) {
        Integer updatedQuantity = cartItem.getQuantity() + quantityToAdd;

        cartItem.setQuantity(updatedQuantity);
        cartItem.setSubtotal(calculateSubtotal(cartItem.getPrice(), updatedQuantity));
    }

    private BigDecimal calculateSubtotal(BigDecimal price, Integer quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private void updateCartTotal(Cart cart) {
        BigDecimal totalAmount = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .menuItemId(cartItem.getMenuItem().getId())
                .itemName(cartItem.getItemName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(cartItem.getSubtotal())
                .build();
    }
}