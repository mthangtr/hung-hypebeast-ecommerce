package org.fyp.hunghypebeastecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_number", columnList = "order_number"),
    @Index(name = "idx_orders_tracking", columnList = "tracking_token"),
    @Index(name = "idx_orders_email", columnList = "customer_email"),
    @Index(name = "idx_orders_status", columnList = "status, created_at"),
    @Index(name = "idx_orders_payment", columnList = "payment_status"),
    @Index(name = "idx_orders_created", columnList = "created_at")
})
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "tracking_token", nullable = false, unique = true)
    private UUID trackingToken;

    // Customer Information
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    // Shipping Address
    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;

    // Payment
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // 'COD', 'SEPAY'

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "pending"; // 'pending', 'paid', 'failed'

    // Order Amounts
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Order Status
    @Column(length = 20)
    private String status = "pending"; // 'pending', 'confirmed', 'processing', 'shipping', 'completed', 'cancelled'

    // Notes
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (trackingToken == null) {
            trackingToken = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
