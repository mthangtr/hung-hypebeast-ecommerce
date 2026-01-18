package org.fyp.hunghypebeastecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.hunghypebeastecommerce.service.InventoryReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationScheduler {

    private final InventoryReservationService reservationService;

    @Scheduled(fixedRateString = "${inventory.reservation.cleanup-interval-ms:60000}")
    public void releaseExpiredReservations() {
        int released = reservationService.releaseExpiredReservations();
        if (released > 0) {
            log.info("Released {} expired inventory reservations", released);
        }
    }
}
