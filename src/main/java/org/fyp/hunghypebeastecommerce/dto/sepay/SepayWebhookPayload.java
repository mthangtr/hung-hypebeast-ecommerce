package org.fyp.hunghypebeastecommerce.dto.sepay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SepayWebhookPayload {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("gateway")
    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("code")
    private String code;

    @JsonProperty("content")
    private String content;

    @JsonProperty("transferType")
    private String transferType;

    @JsonProperty("transferAmount")
    private BigDecimal transferAmount;

    @JsonProperty("accumulated")
    private BigDecimal accumulated;

    @JsonProperty("subAccount")
    private String subAccount;

    @JsonProperty("referenceCode")
    private String referenceCode;

    @JsonProperty("description")
    private String description;
}
