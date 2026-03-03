package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.dto.CartItemDto;
import com.eshoppingzone.cart.dto.CartRequest;
import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.CartItem;
import com.eshoppingzone.cart.exception.ResourceNotFoundException;
import com.eshoppingzone.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CartResponse getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse createOrUpdateCart(CartRequest request) {
        Cart cart = cartRepository.findByUserId(request.getUserId())
                .orElse(new Cart());

        cart.setUserId(request.getUserId());
        cart.clearItems();

        if (request.getItems() != null) {
            for (CartItemDto itemDto : request.getItems()) {
                CartItem cartItem = mapToCartItem(itemDto);
                cart.addCartItem(cartItem);
            }
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public CartResponse addItemToCart(Long userId, CartItemDto itemDto) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return newCart;
                });

        // Check if product already exists in cart
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + itemDto.getQuantity());
            if (itemDto.getCategory() != null) {
                existingItem.setCategory(itemDto.getCategory());
            }
        } else {
            CartItem newItem = mapToCartItem(itemDto);
            cart.addCartItem(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart: " + productId));

        cart.removeCartItem(itemToRemove);
        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
        cart.clearItems();
        cartRepository.save(cart);
    }

    private CartResponse mapToResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUserId());

        List<CartItemDto> itemDtos = cart.getCartItems().stream()
                .map(this::mapToCartItemDto)
                .collect(Collectors.toList());
        response.setItems(itemDtos);

        return response;
    }

    private CartItemDto mapToCartItemDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        dto.setCategory(item.getCategory());
        return dto;
    }

    private CartItem mapToCartItem(CartItemDto dto) {
        CartItem item = new CartItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setCategory(dto.getCategory());
        return item;
    }
}
