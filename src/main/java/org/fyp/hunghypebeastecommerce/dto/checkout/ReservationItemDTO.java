package org.fyp.hunghypebeastecommerce.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItemDTO {

    private Long variantId;
    private String sku;
    private String productName;
    private String size;
    private String color;
    private Integer quantity;
}
