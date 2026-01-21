package org.fyp.hunghypebeastecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.fyp.hunghypebeastecommerce.dto.ResponseObject;
import org.fyp.hunghypebeastecommerce.dto.checkout.CreateOrderRequest;
import org.fyp.hunghypebeastecommerce.dto.checkout.OrderDTO;
import org.fyp.hunghypebeastecommerce.dto.checkout.OrderItemDTO;
import org.fyp.hunghypebeastecommerce.dto.checkout.ReservationDTO;
import org.fyp.hunghypebeastecommerce.entity.Order;
import org.fyp.hunghypebeastecommerce.entity.OrderItem;
import org.fyp.hunghypebeastecommerce.service.EmailService;
import org.fyp.hunghypebeastecommerce.service.InventoryReservationService;
import org.fyp.hunghypebeastecommerce.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.fyp.hunghypebeastecommerce.config.SepayConfig;
import org.fyp.hunghypebeastecommerce.dto.sepay.SepayPaymentInfoDTO;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final InventoryReservationService reservationService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final SepayConfig sepayConfig;

    // Khi người dùng bấm checkout -> tạm thời giữ hàng để người khác không mua hết (trong 15 phút)
    // Hết 15 phút mà không thanh toán thì trả hàng về kho qua InventoryReservationScheduler
    @PostMapping("/reserve")
    public ResponseEntity<ResponseObject<ReservationDTO>> reserveInventory(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        ReservationDTO reservation = reservationService.reserveInventory(sessionId);
        return ResponseEntity.ok(ResponseObject.success("Inventory reserved for 15 minutes", reservation));
    }

    // Mục đích để kiểm tra và trả về thông tin reservation hiện tại của session
    // Ví dụ như để hiển thị cho fe biết hàng đang được giữa bao lâu nữa
    @GetMapping("/reservation")
    public ResponseEntity<ResponseObject<ReservationDTO>> getReservation(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        ReservationDTO reservation = reservationService.getActiveReservation(sessionId);
        if (reservation == null) {
            return ResponseEntity.ok(ResponseObject.success("No active reservation", null));
        }
        return ResponseEntity.ok(ResponseObject.success("Active reservation found", reservation));
    }

    @DeleteMapping("/reservation")
    public ResponseEntity<ResponseObject<Void>> cancelReservation(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        reservationService.releaseReservation(sessionId);
        return ResponseEntity.ok(ResponseObject.success("Reservation cancelled", null));
    }

    @PostMapping("/order")
    public ResponseEntity<ResponseObject<OrderDTO>> createOrder(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody CreateOrderRequest request
    ) {
        Order order = orderService.createOrder(
                sessionId,
                request.getCustomerName(),
                request.getCustomerEmail(),
                request.getCustomerPhone(),
                request.getShippingAddress(),
                request.getShippingCity(),
                request.getShippingDistrict(),
                request.getPaymentMethod(),
                request.getCustomerNote()
        );

        emailService.sendOrderConfirmation(order);

        OrderDTO orderDTO = mapToOrderDTO(order);
        
        if ("SEPAY".equals(order.getPaymentMethod())) {
            orderDTO.setSepayPaymentInfo(buildSepayPaymentInfo(order));
        }

        return ResponseEntity.ok(ResponseObject.success("Order created successfully", orderDTO));
    }

    private OrderDTO mapToOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingToken(order.getTrackingToken())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingDistrict(order.getShippingDistrict())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .customerNote(order.getCustomerNote())
                .adminNote(order.getAdminNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .items(order.getItems().stream()
                        .map(this::mapToOrderItemDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    private OrderItemDTO mapToOrderItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .variantId(item.getVariant().getId())
                .productName(item.getProductName())
                .variantSku(item.getVariantSku())
                .variantSize(item.getVariantSize())
                .variantColor(item.getVariantColor())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private SepayPaymentInfoDTO buildSepayPaymentInfo(Order order) {
        return SepayPaymentInfoDTO.builder()
                .bankName(sepayConfig.getBankName())
                .accountNumber(sepayConfig.getBankAccountNumber())
                .accountName(sepayConfig.getBankAccountName())
                .amount(order.getTotalAmount())
                .orderNumber(order.getOrderNumber())
                .transferContent("Thanh toan " + order.getOrderNumber())
                .build();
    }
}
