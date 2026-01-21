package org.fyp.hunghypebeastecommerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.hunghypebeastecommerce.config.SepayConfig;
import org.fyp.hunghypebeastecommerce.dto.sepay.SepayWebhookPayload;
import org.fyp.hunghypebeastecommerce.entity.Order;
import org.fyp.hunghypebeastecommerce.entity.PaymentTransaction;
import org.fyp.hunghypebeastecommerce.exception.CustomException;
import org.fyp.hunghypebeastecommerce.exception.ErrorCode;
import org.fyp.hunghypebeastecommerce.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookService {

    private final SepayConfig sepayConfig;
    private final OrderService orderService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("(ORD\\d+)");

    @Transactional
    public void processWebhook(SepayWebhookPayload payload, String apiKey) {
        log.info("Processing Sepay webhook: transactionId={}, amount={}, content={}", 
                payload.getId(), payload.getTransferAmount(), payload.getContent());

        verifyApiKey(apiKey);

        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            log.warn("Ignoring non-incoming transaction: type={}", payload.getTransferType());
            return;
        }

        if (isDuplicateTransaction(String.valueOf(payload.getId()))) {
            log.warn("Duplicate transaction detected: transactionId={}", payload.getId());
            throw new CustomException(ErrorCode.DUPLICATE_TRANSACTION);
        }

        String orderNumber = extractOrderNumber(payload.getContent());
        if (orderNumber == null) {
            log.error("Order number not found in transfer content: {}", payload.getContent());
            throw new CustomException(ErrorCode.ORDER_NUMBER_NOT_FOUND_IN_CONTENT);
        }

        Order order = orderService.getOrderByOrderNumber(orderNumber);

        verifyPaymentAmount(order, payload.getTransferAmount());

        if ("paid".equals(order.getPaymentStatus())) {
            log.warn("Order already paid: orderNumber={}", orderNumber);
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        createOrUpdatePaymentTransaction(order, payload);

        orderService.updatePaymentStatus(
                order.getId(), 
                "paid", 
                String.valueOf(payload.getId()), 
                serializePayload(payload)
        );

        emailService.sendOrderConfirmation(order);

        log.info("Successfully processed Sepay webhook for order: {}", orderNumber);
    }

    private void verifyApiKey(String apiKey) {
        if (!sepayConfig.isVerifySignature()) {
            log.info("Webhook signature verification is disabled");
            return;
        }
        
        log.info("Verifying API key - Expected: {}, Received: {}", 
                sepayConfig.getApiKey(), apiKey);
        if (!sepayConfig.getApiKey().equals(apiKey)) {
            log.error("Invalid API key received - Expected: {}, Received: {}", 
                    sepayConfig.getApiKey(), apiKey);
            throw new CustomException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    private boolean isDuplicateTransaction(String transactionId) {
        return paymentTransactionRepository.findByTransactionId(transactionId).isPresent();
    }

    private String extractOrderNumber(String content) {
        if (content == null) {
            return null;
        }

        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private void verifyPaymentAmount(Order order, BigDecimal paidAmount) {
        if (order.getTotalAmount().compareTo(paidAmount) != 0) {
            log.error("Payment amount mismatch: expected={}, received={}", 
                    order.getTotalAmount(), paidAmount);
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void createOrUpdatePaymentTransaction(Order order, SepayWebhookPayload payload) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setTransactionId(String.valueOf(payload.getId()));
        transaction.setAmount(payload.getTransferAmount());
        transaction.setPaymentMethod("SEPAY");
        transaction.setStatus("success");
        transaction.setGatewayResponse(serializePayload(payload));

        paymentTransactionRepository.save(transaction);
    }

    private String serializePayload(SepayWebhookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload", e);
            return payload.toString();
        }
    }
}
