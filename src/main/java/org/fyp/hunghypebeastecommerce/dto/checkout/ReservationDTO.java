package org.fyp.hunghypebeastecommerce.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private String sessionId;
    private List<ReservationItemDTO> items;
    private LocalDateTime expiresAt;
    private long remainingSeconds;
}
