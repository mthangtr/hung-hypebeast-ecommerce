package org.fyp.hunghypebeastecommerce.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SepayConfig {

    @Value("${sepay.webhook.api-key}")
    private String apiKey;

    @Value("${sepay.bank.account-number}")
    private String bankAccountNumber;

    @Value("${sepay.bank.account-name}")
    private String bankAccountName;

    @Value("${sepay.bank.name}")
    private String bankName;

    @Value("${sepay.webhook.verify-signature}")
    private boolean verifySignature;
}
