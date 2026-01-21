package org.fyp.hunghypebeastecommerce.service;

import lombok.RequiredArgsConstructor;
import org.fyp.hunghypebeastecommerce.dto.cart.*;
import org.fyp.hunghypebeastecommerce.entity.Cart;
import org.fyp.hunghypebeastecommerce.entity.CartItem;
import org.fyp.hunghypebeastecommerce.entity.ProductVariant;
import org.fyp.hunghypebeastecommerce.exception.CustomException;
import org.fyp.hunghypebeastecommerce.exception.ErrorCode;
import org.fyp.hunghypebeastecommerce.repository.CartItemRepository;
import org.fyp.hunghypebeastecommerce.repository.CartRepository;
import org.fyp.hunghypebeastecommerce.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true)
    public CartDTO getCart(String sessionId) {
        Cart cart = getOrCreateCart(sessionId);
        return convertToDTO(cart);
    }

    @Transactional
    public CartDTO addToCart(String sessionId, AddToCartRequest request) {
        if (request.getVariantId() == null || request.getQuantity() == null || request.getQuantity() < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Cart cart = getOrCreateCart(sessionId);
        ProductVariant variant = findVariant(request.getVariantId());

        // Phải lấy stock hiện tại trừ đi số lượng đã được đặt trước để tránh oversell
        int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();
        CartItem existingItem = cartItemRepository.findByCartAndVariant(cart, variant).orElse(null);

        // Số lượng hiện có của product variant trong card
        int currentQuantityInCart = existingItem != null ? existingItem.getQuantity() : 0;

        // Tổng số lượng = số lượng hiện có trong cart + số lượng muốn thêm
        int requestedTotal = currentQuantityInCart + request.getQuantity();

        // Kiểm tra nếu tổng số lượng yêu cầu vượt quá số lượng tồn kho
        if (requestedTotal > availableStock) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }

        if (existingItem != null) {
            existingItem.setQuantity(requestedTotal);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Transactional
    public CartDTO updateCartItem(String sessionId, Long itemId, UpdateCartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Cart cart = findCartBySessionId(sessionId);
        CartItem item = findCartItem(cart, itemId);

        ProductVariant variant = item.getVariant();
        int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();

        if (request.getQuantity() > availableStock) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }

        item.setQuantity(request.getQuantity());
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Transactional
    public CartDTO removeCartItem(String sessionId, Long itemId) {
        Cart cart = findCartBySessionId(sessionId);
        CartItem item = findCartItem(cart, itemId);

        cart.getItems().remove(item);
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    private Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
    }

    private Cart findCartBySessionId(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));
    }

    private ProductVariant findVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .filter(ProductVariant::getIsActive)
                .orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));
    }

    private CartItem findCartItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private CartDTO convertToDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::convertToItemDTO)
                .collect(Collectors.toList());

        int totalItems = itemDTOs.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        BigDecimal totalAmount = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .items(itemDTOs)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemDTO convertToItemDTO(CartItem item) {
        ProductVariant variant = item.getVariant();
        BigDecimal unitPrice = variant.getProduct().getBasePrice()
                .add(variant.getPriceAdjustment());
        int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();

        return CartItemDTO.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(availableStock)
                .build();
    }
}
