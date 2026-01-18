package org.fyp.hunghypebeastecommerce.repository;

import org.fyp.hunghypebeastecommerce.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findBySessionIdAndStatus(String sessionId, String status);

    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'active' AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
