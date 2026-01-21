package org.fyp.hunghypebeastecommerce.dto.sepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SepayPaymentInfoDTO {

    private String bankName;
    
    private String accountNumber;
    
    private String accountName;
    
    private BigDecimal amount;
    
    private String orderNumber;
    
    private String transferContent;
}
