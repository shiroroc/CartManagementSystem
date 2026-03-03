package com.eshoppingzone.cart.controller;

import com.eshoppingzone.cart.dto.CartItemDto;
import com.eshoppingzone.cart.dto.CartRequest;
import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@Tag(name = "Shopping Cart", description = "Shopping Cart Management APIs")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
        logger.info("CartController initialized");
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get cart by user ID", description = "Retrieves the shopping cart for the specified user")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable Long userId) {
        logger.info("GET /cart/{} - Fetching cart for user", userId);
        CartResponse cart = cartService.getCartByUserId(userId);
        logger.info("Cart retrieved for user {} with {} items", userId, cart.getItems().size());
        return ResponseEntity.ok(cart);
    }

    @PostMapping
    @Operation(summary = "Create or update cart", description = "Creates a new cart or updates existing cart for a user")
    public ResponseEntity<CartResponse> createOrUpdateCart(@Valid @RequestBody CartRequest request) {
        logger.info("POST /cart - Creating/updating cart for user: {}", request.getUserId());
        CartResponse response = cartService.createOrUpdateCart(request);
        logger.info("Cart created/updated for user {} with {} items", request.getUserId(), response.getItems().size());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a new item or updates quantity if item already exists")
    public ResponseEntity<CartResponse> addItemToCart(
            @PathVariable Long userId,
            @Valid @RequestBody CartItemDto itemDto) {
        logger.info("POST /cart/{}/items - Adding item {} to cart", userId, itemDto.getProductId());
        CartResponse response = cartService.addItemToCart(userId, itemDto);
        logger.info("Item {} added to cart for user {}", itemDto.getProductId(), userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific product from the user's cart")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        logger.info("DELETE /cart/{}/items/{} - Removing item from cart", userId, productId);
        CartResponse response = cartService.removeItemFromCart(userId, productId);
        logger.info("Item {} removed from cart for user {}", productId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Clear cart", description = "Removes all items from the user's cart")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        logger.info("DELETE /cart/{} - Clearing cart for user", userId);
        cartService.clearCart(userId);
        logger.info("Cart cleared for user {}", userId);
        return ResponseEntity.noContent().build();
    }
}
