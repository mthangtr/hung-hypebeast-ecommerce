package org.fyp.hunghypebeastecommerce.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.hunghypebeastecommerce.dto.sepay.SepayPaymentInfoDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private UUID trackingToken;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingDistrict;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String status;
    private String customerNote;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private List<OrderItemDTO> items;
    private SepayPaymentInfoDTO sepayPaymentInfo;
}
