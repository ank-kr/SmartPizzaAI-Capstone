package com.smartpizza.core.controller;

import com.smartpizza.core.dto.AddToCartRequest;
import com.smartpizza.core.dto.CartResponse;
import com.smartpizza.core.dto.UpdateCartItemRequest;
import com.smartpizza.core.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable Long userId) {
        CartResponse response = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request
    ) {
        CartResponse response = cartService.updateCartItem(cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable Long cartItemId) {
        CartResponse response = cartService.removeCartItem(cartItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<CartResponse> clearCart(@PathVariable Long userId) {
        CartResponse response = cartService.clearCart(userId);
        return ResponseEntity.ok(response);
    }
}
