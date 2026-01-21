package org.fyp.hunghypebeastecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.hunghypebeastecommerce.dto.checkout.ReservationDTO;
import org.fyp.hunghypebeastecommerce.dto.checkout.ReservationItemDTO;
import org.fyp.hunghypebeastecommerce.entity.Cart;
import org.fyp.hunghypebeastecommerce.entity.CartItem;
import org.fyp.hunghypebeastecommerce.entity.InventoryReservation;
import org.fyp.hunghypebeastecommerce.entity.ProductVariant;
import org.fyp.hunghypebeastecommerce.exception.CustomException;
import org.fyp.hunghypebeastecommerce.exception.ErrorCode;
import org.fyp.hunghypebeastecommerce.repository.CartRepository;
import org.fyp.hunghypebeastecommerce.repository.InventoryReservationRepository;
import org.fyp.hunghypebeastecommerce.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final ProductVariantRepository variantRepository;
    private final CartRepository cartRepository;

    @Value("${inventory.reservation.duration-minutes:15}")
    private int reservationDurationMinutes;

    @Transactional
    public ReservationDTO reserveInventory(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // để hủy bỏ bất kỳ reservation nào đang active của session đó 
        // -> giảm reserved_quantity của các variant về lại kho và đánh dấu reservation là "cancelled" 
        // -> để đảm bảo không có reservation bị trùng
        releaseExistingReservations(sessionId);

        // Tạo reservation mới
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationDurationMinutes);
        List<InventoryReservation> reservations = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = variantRepository.findByIdWithLock(cartItem.getVariant().getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

            // Với mỗi biến thể sản phẩm, kiểm tra xem có đủ hàng không
            // Bằng cách kiểm tra lại số lượng tồn kho trừ đi số lượng đã được đặt trước (giống với bước add cart)
            int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();

            if (cartItem.getQuantity() > availableStock) {
                rollbackReservations(reservations);
                throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
            }

            // Cập nhật số lượng đã được đặt trước trong ProductVariant để tránh oversell
            // Bằng cách cộng thêm số lượng của cartItem vào reservedQuantity
            variant.setReservedQuantity(variant.getReservedQuantity() + cartItem.getQuantity());
            variantRepository.save(variant);

            InventoryReservation reservation = new InventoryReservation();
            reservation.setVariant(variant);
            reservation.setQuantity(cartItem.getQuantity());
            reservation.setSessionId(sessionId);
            reservation.setExpiresAt(expiresAt);
            reservation.setStatus("active");
            reservations.add(reservationRepository.save(reservation));
        }

        return buildReservationDTO(sessionId, reservations, expiresAt);
    }

    @Transactional
    public void releaseReservation(String sessionId) {
        releaseExistingReservations(sessionId);
    }

    @Transactional
    public void completeReservation(String sessionId, Long orderId) {
        List<InventoryReservation> reservations = reservationRepository
                .findBySessionIdAndStatus(sessionId, "active");

        for (InventoryReservation reservation : reservations) {
            ProductVariant variant = variantRepository.findByIdWithLock(reservation.getVariant().getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

            variant.setStockQuantity(variant.getStockQuantity() - reservation.getQuantity());
            variant.setReservedQuantity(variant.getReservedQuantity() - reservation.getQuantity());
            variantRepository.save(variant);

            reservation.setStatus("completed");
            reservation.setReservedForOrderId(orderId);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<InventoryReservation> expired = reservationRepository.findExpiredReservations(now);

        for (InventoryReservation reservation : expired) {
            ProductVariant variant = variantRepository.findByIdWithLock(reservation.getVariant().getId())
                    .orElse(null);

            if (variant != null) {
                // Giảm số lượng đang được giữ ở InventoryReservation và đảm bảo không âm
                variant.setReservedQuantity(
                        Math.max(0, variant.getReservedQuantity() - reservation.getQuantity())
                );
                variantRepository.save(variant);
            }

            reservation.setStatus("expired");
            reservationRepository.save(reservation);
        }

        return expired.size();
    }

    @Transactional(readOnly = true)
    public ReservationDTO getActiveReservation(String sessionId) {
        List<InventoryReservation> reservations = reservationRepository
                .findBySessionIdAndStatus(sessionId, "active");

        if (reservations.isEmpty()) {
            return null;
        }

        LocalDateTime expiresAt = reservations.get(0).getExpiresAt();
        if (expiresAt.isBefore(LocalDateTime.now())) {
            return null;
        }

        return buildReservationDTO(sessionId, reservations, expiresAt);
    }

    // khi người dùng thêm sản phẩm vào giỏ hàng hoặc bắt đầu thanh toán, hệ thống tạo reservation để "khóa" kho hàng tạm thời.
    // Nếu phiên bị hủy (ví dụ: timeout, người dùng thoát), hàm này giải phóng kho để tránh lãng phí.
    private void releaseExistingReservations(String sessionId) {
        List<InventoryReservation> existing = reservationRepository
                .findBySessionIdAndStatus(sessionId, "active");

        for (InventoryReservation reservation : existing) {
            ProductVariant variant = variantRepository.findByIdWithLock(reservation.getVariant().getId())
                    .orElse(null);

            if (variant != null) {
                // Giảm reservedQuantity (số lượng đã đặt trước) của biến thể bằng cách trừ đi quantity của reservation
                variant.setReservedQuantity(
                        Math.max(0, variant.getReservedQuantity() - reservation.getQuantity())
                );
                variantRepository.save(variant);
            }

            reservation.setStatus("cancelled");
            reservationRepository.save(reservation);
        }
    }

    private void rollbackReservations(List<InventoryReservation> reservations) {
        for (InventoryReservation reservation : reservations) {
            ProductVariant variant = reservation.getVariant();
            variant.setReservedQuantity(
                    Math.max(0, variant.getReservedQuantity() - reservation.getQuantity())
            );
            variantRepository.save(variant);
            reservation.setStatus("cancelled");
            reservationRepository.save(reservation);
        }
    }

    private ReservationDTO buildReservationDTO(String sessionId, 
                                                List<InventoryReservation> reservations,
                                                LocalDateTime expiresAt) {
        List<ReservationItemDTO> items = reservations.stream()
                .map(r -> ReservationItemDTO.builder()
                        .variantId(r.getVariant().getId())
                        .sku(r.getVariant().getSku())
                        .productName(r.getVariant().getProduct().getName())
                        .size(r.getVariant().getSize())
                        .color(r.getVariant().getColor())
                        .quantity(r.getQuantity())
                        .build())
                .toList();

        long remainingSeconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();

        return ReservationDTO.builder()
                .sessionId(sessionId)
                .items(items)
                .expiresAt(expiresAt)
                .remainingSeconds(Math.max(0, remainingSeconds))
                .build();
    }
}
