package org.fyp.hunghypebeastecommerce.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PRODUCT_NOT_FOUND(1001, "Product not found"),
    VARIANT_NOT_FOUND(1002, "Product variant not found"),
    INVALID_PRICE_RANGE(1003, "Invalid price range: min price cannot be greater than max price"),
    INVALID_PAGINATION(1004, "Invalid pagination parameters"),
    
    CART_NOT_FOUND(2001, "Shopping cart not found"),
    CART_ITEM_NOT_FOUND(2002, "Cart item not found"),
    INSUFFICIENT_STOCK(2003, "Insufficient stock for requested quantity"),
    
    ORDER_NOT_FOUND(3001, "Order not found"),
    INVALID_ORDER_STATUS(3002, "Invalid order status transition"),
    
    RESERVATION_EXPIRED(4001, "Inventory reservation has expired"),
    RESERVATION_FAILED(4002, "Failed to reserve inventory"),
    
    ORDER_ALREADY_PAID(5004, "Order has already been paid"),
    PAYMENT_TRANSACTION_NOT_FOUND(5005, "Payment transaction not found"),
    INVALID_PAYMENT_STATUS(5006, "Invalid payment status transition"),
    
    INVALID_INPUT(9001, "Invalid input data"),
    INTERNAL_ERROR(9999, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
