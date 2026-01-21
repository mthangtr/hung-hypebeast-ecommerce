package org.fyp.hunghypebeastecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.fyp.hunghypebeastecommerce.dto.ResponseObject;
import org.fyp.hunghypebeastecommerce.dto.sepay.SepayWebhookPayload;
import org.fyp.hunghypebeastecommerce.service.SepayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final SepayWebhookService sepayWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<ResponseObject<Void>> handleWebhook(
            @RequestBody SepayWebhookPayload payload,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam(value = "api_key", required = false) String apiKeyParam
    ) {
        String finalApiKey = apiKey != null ? apiKey : apiKeyParam;
        sepayWebhookService.processWebhook(payload, finalApiKey);
        return ResponseEntity.ok(ResponseObject.success("Webhook processed", null));
    }
}
