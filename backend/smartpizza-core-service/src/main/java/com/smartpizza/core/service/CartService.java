package com.smartpizza.core.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service                        //This class contains business logic and should be managed as a Spring bean.
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;

    //create cart for customer 
    public CartResponse addToCart(AddToCartRequest request) {
        validateAddToCartRequest(request);

        log.info(
                "Adding item to cart. userId={}, menuItemId={}, quantity={}",
                request.getUserId(),
                request.getMenuItemId(),
                request.getQuantity()
        );

        // Fetch the selected menu item and ensure it exists before adding to cart.
        MenuItem menuItem = getMenuItem(request.getMenuItemId());
        Cart cart = getOrCreateCart(request.getUserId());         //create a new cart for the user if one doesnot exist

        // Check whether the same menu item is already present in the user's cart.
        CartItem cartItem = cartItemRepository
                .findByCartIdAndMenuItemId(cart.getId(), menuItem.getId())
                .orElse(null);

        // if cart is empty
        if (cartItem == null) {

            log.info(
                    "Creating new cart item. cartId={}, menuItemId={}",
                    cart.getId(),
                    menuItem.getId()
            );

            // First time this menu item is being added to the cart.
            cartItem = createCartItem(cart, menuItem, request.getQuantity());
        } else {

            log.info(
                    "Updating existing cart item quantity. cartItemId={}, quantityToAdd={}",
                    cartItem.getId(),
                    request.getQuantity()
            );

            updateExistingCartItem(cartItem, request.getQuantity());  //else update the existing item
        }

        cartItemRepository.save(cartItem);
        updateCartTotal(cart);   // // Recalculate cart total after item quantity/subtotal changes.

        log.info(
                "Cart updated successfully after add-to-cart. cartId={}, userId={}",
                cart.getId(),
                request.getUserId()
        );

        return getCartByUserId(request.getUserId());  //return latest cart state
    }


    //This method is used to fetch the current cart of a user

    public CartResponse getCartByUserId(Long userId) {
        validateUserId(userId);

        log.info("Fetching cart for userId={}", userId);

        // Ensure the user has a cart before fetching cart items.
        Cart cart = getOrCreateCart(userId);

        // Fetch all cart items linked to this cart and convert them into response DTOs.
        List<CartItemResponse> itemResponses = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(this::mapToCartItemResponse)
                .toList();

        log.info(
                "Cart fetched successfully. cartId={}, userId={}, itemCount={}",
                cart.getId(),
                cart.getUserId(),
                itemResponses.size()
        );

        // Return cart details along with all cart items for frontend display.
        return CartResponse.builder()  //builder() here is coming from Lombok @Builder annotation means Create a CartResponse object in a clean, readable way without writing a long constructor.
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .totalAmount(cart.getTotalAmount())
                .items(itemResponses)
                .build();
    }


    //this method is for updating quantity of an existing cart item from the cart page.

    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        validateCartItemId(cartItemId);
        validateUpdateCartItemRequest(request);

        log.info(
                "Updating cart item. cartItemId={}, newQuantity={}",
                cartItemId,
                request.getQuantity()
        );

        // Fetch the cart item before updating quantity.check whether available or not
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found during update. cartItemId={}", cartItemId);
                    return new RuntimeException("Cart item not found with id: " + cartItemId);
                });

        // Update quantity and recalculate item subtotal based on item price.
        cartItem.setQuantity(request.getQuantity());
        cartItem.setSubtotal(calculateSubtotal(cartItem.getPrice(), request.getQuantity()));

        cartItemRepository.save(cartItem);

        //if any update happen again calculate the updated cart
        Cart cart = cartItem.getCart();
        updateCartTotal(cart);

        log.info(
                "Cart item updated successfully. cartItemId={}, cartId={}, userId={}",
                cartItemId,
                cart.getId(),
                cart.getUserId()
        );

        //return latest cart state for frontend synchronization.
        return getCartByUserId(cart.getUserId());
    }

    // this method is used for removecart item

    public CartResponse removeCartItem(Long cartItemId) {
        validateCartItemId(cartItemId);

        log.info("Removing cart item. cartItemId={}", cartItemId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found during remove. cartItemId={}", cartItemId);
                    return new RuntimeException("Cart item not found with id: " + cartItemId);
                });

        Cart cart = cartItem.getCart();
        Long userId = cart.getUserId();

        cartItemRepository.delete(cartItem);
        updateCartTotal(cart);

        log.info(
                "Cart item removed successfully. cartItemId={}, cartId={}, userId={}",
                cartItemId,
                cart.getId(),
                userId
        );

        return getCartByUserId(userId);
    }

    //method for clearing complete cart 

    public CartResponse clearCart(Long userId) {
        validateUserId(userId);

        log.info("Clearing cart for userId={}", userId);

        Cart cart = getOrCreateCart(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        cartItemRepository.deleteAll(cartItems);

        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info(
                "Cart cleared successfully. cartId={}, userId={}, removedItemCount={}",
                cart.getId(),
                userId,
                cartItems.size()
        );

        return getCartByUserId(userId);
    }

    //request validation helper function
    //It checks whether the incoming request has all required values before backend tries to add item to cart.
    private void validateAddToCartRequest(AddToCartRequest request) {

        // Request body is mandatory because add-to-cart needs userId, menuItemId and quantity.
        if (request == null) {
            log.warn("Add-to-cart request failed because request body is null");
            throw new RuntimeException("Request body cannot be null");
        }

        // Cart operation must be linked to a valid user.
        validateUserId(request.getUserId());

        // Menu item id is required to identify which item should be added.
        if (request.getMenuItemId() == null) {
            log.warn("Add-to-cart request failed because menuItemId is null. userId={}", request.getUserId());
            throw new RuntimeException("Menu item id is required");
        }

        // Quantity must be present and greater than zero.
        validateQuantity(request.getQuantity());
    }

    // Update quantity request must contain a valid request body.
    private void validateUpdateCartItemRequest(UpdateCartItemRequest request) {
        if (request == null) {
            log.warn("Update cart item request failed because request body is null");
            throw new RuntimeException("Request body cannot be null");
        }

        // Quantity must be present and greater than zero.
        validateQuantity(request.getQuantity());
    }

    private void validateUserId(Long userId) {
        // User id is required to identify which customer's cart should be accessed.
        if (userId == null) {
            log.warn("Cart operation failed because userId is null");
            throw new RuntimeException("User id is required");
        }
    }

    private void validateCartItemId(Long cartItemId) {
        // Cart item id is required to identify which cart item should be updated or removed.
        if (cartItemId == null) {
            log.warn("Cart operation failed because cartItemId is null");
            throw new RuntimeException("Cart item id is required");
        }
    }


    //quantity validation method

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            log.warn("Cart operation failed because quantity is invalid. quantity={}", quantity);
            throw new RuntimeException("Quantity must be greater than zero");
        }
    }


    //this method is a helper method inside CartService.this method fetch Fetch menu item by ID,ensure item available,return valid menu obj
    private MenuItem getMenuItem(Long menuItemId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> {
                    log.warn("Menu item not found. menuItemId={}", menuItemId);
                    return new RuntimeException("Menu item not found with id: " + menuItemId);
                });

        // Prevent unavailable menu items from being added to cart.
        if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
            log.warn("Unavailable menu item requested for cart operation. menuItemId={}", menuItemId);
            throw new RuntimeException("Menu item is currently not available");
        }

        return menuItem;
    }


    //this method
    //    1. Find existing cart for the user
    //    OR
    //    2. Create a new cart if user does not have one

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Cart not found for userId={}. Creating new cart.", userId);
                    return createCart(userId);
                });
    }


    //cart creation helper 
    private Cart createCart(Long userId) {

        //create an empty cart for new user.
        Cart cart = Cart.builder()
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .cartItems(new ArrayList<>())
                .build();

        Cart savedCart = cartRepository.save(cart);

        log.info("New cart created successfully. cartId={}, userId={}", savedCart.getId(), userId);

        return savedCart;
    }

    //this method creates a new CartItem entity, when item being added to cart
    private CartItem createCartItem(Cart cart, MenuItem menuItem, Integer quantity) {
        return CartItem.builder()
                .cart(cart) //link this cart item to the user's cart.
                .menuItem(menuItem)
                .itemName(menuItem.getName())
                .price(menuItem.getPrice())
                .quantity(quantity)
                .subtotal(calculateSubtotal(menuItem.getPrice(), quantity))
                .build();
    }


    //method to update existing item in the cart
    private void updateExistingCartItem(CartItem cartItem, Integer quantityToAdd) {
        Integer updatedQuantity = cartItem.getQuantity() + quantityToAdd;

        cartItem.setQuantity(updatedQuantity);
        cartItem.setSubtotal(calculateSubtotal(cartItem.getPrice(), updatedQuantity));
    }

    //helper function to calculate sub-total
    private BigDecimal calculateSubtotal(BigDecimal price, Integer quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    //update cart total
    private void updateCartTotal(Cart cart) {
        BigDecimal totalAmount = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);

        log.debug("Cart total updated. cartId={}, totalAmount={}", cart.getId(), totalAmount);
    }

  //entity(database object) -> DTO mapping(object sent to the frontend)
   
    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .cartItemId(cartItem.getId())  //// Expose cart item id so frontend can update or remove this item.
                .menuItemId(cartItem.getMenuItem().getId()) // // Expose menu item id for item reference and future actions.
                .itemName(cartItem.getItemName())  // Send display-ready item details to frontend.
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())// Send current quantity and calculated subtotal.
                .subtotal(cartItem.getSubtotal())
                .build();
    }
}