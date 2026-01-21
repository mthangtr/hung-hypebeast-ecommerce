package org.fyp.hunghypebeastecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.hunghypebeastecommerce.entity.*;
import org.fyp.hunghypebeastecommerce.exception.CustomException;
import org.fyp.hunghypebeastecommerce.exception.ErrorCode;
import org.fyp.hunghypebeastecommerce.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryReservationService inventoryReservationService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public Order createOrder(String sessionId, String customerName, String customerEmail, 
                            String customerPhone, String shippingAddress, String shippingCity,
                            String shippingDistrict, String paymentMethod, String customerNote) {
        
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (inventoryReservationService.getActiveReservation(sessionId) == null) {
            throw new CustomException(ErrorCode.RESERVATION_EXPIRED);
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setTrackingToken(UUID.randomUUID());
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerPhone(customerPhone);
        order.setShippingAddress(shippingAddress);
        order.setShippingCity(shippingCity);
        order.setShippingDistrict(shippingDistrict);
        order.setPaymentMethod(paymentMethod);
        order.setCustomerNote(customerNote);
        order.setStatus("pending");
        order.setPaymentStatus("pending");

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = variantRepository.findByIdWithLock(cartItem.getVariant().getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

            BigDecimal variantPrice = variant.getProduct().getBasePrice().add(variant.getPriceAdjustment());
            BigDecimal itemSubtotal = variantPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(variant);
            orderItem.setProductName(variant.getProduct().getName());
            orderItem.setVariantSku(variant.getSku());
            orderItem.setVariantSize(variant.getSize());
            orderItem.setVariantColor(variant.getColor());
            orderItem.setUnitPrice(variantPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(itemSubtotal);

            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(subtotal);

        Order savedOrder = orderRepository.save(order);

        if ("SEPAY".equals(paymentMethod)) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setOrder(savedOrder);
            transaction.setAmount(savedOrder.getTotalAmount());
            transaction.setPaymentMethod("SEPAY");
            transaction.setStatus("pending");
            paymentTransactionRepository.save(transaction);
        }

        inventoryReservationService.completeReservation(sessionId, savedOrder.getId());

        cartRepository.delete(cart);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrderByTrackingToken(UUID trackingToken) {
        return orderRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Order> listOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return orderRepository.findByStatus(status, pageable);
        }
        return orderRepository.findAll(pageable);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus, String adminNote, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        String oldStatus = order.getStatus();
        
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        order.setStatus(newStatus);
        if (adminNote != null && !adminNote.isBlank()) {
            order.setAdminNote(adminNote);
        }

        if ("confirmed".equals(newStatus) && order.getConfirmedAt() == null) {
            order.setConfirmedAt(LocalDateTime.now());
        }
        if ("processing".equals(newStatus)) {
            order.setConfirmedAt(LocalDateTime.now());
        }
        if ("shipping".equals(newStatus) && order.getShippedAt() == null) {
            order.setShippedAt(LocalDateTime.now());
        }
        if ("completed".equals(newStatus) && order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }
        if ("cancelled".equals(newStatus) && order.getCancelledAt() == null) {
            order.setCancelledAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "pending" -> "confirmed".equals(newStatus) || "cancelled".equals(newStatus);
            case "confirmed" -> "processing".equals(newStatus) || "cancelled".equals(newStatus);
            case "processing" -> "shipping".equals(newStatus) || "cancelled".equals(newStatus);
            case "shipping" -> "completed".equals(newStatus) || "cancelled".equals(newStatus);
            default -> false;
        };
    }

    @Transactional
    public Order updatePaymentStatus(Long orderId, String status, String transactionId, String gatewayResponse) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (status == null || status.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if ("paid".equals(order.getPaymentStatus())) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        order.setPaymentStatus(status);
        if ("paid".equals(status) && order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }
}
