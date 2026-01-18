package org.fyp.hunghypebeastecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.fyp.hunghypebeastecommerce.dto.ResponseObject;
import org.fyp.hunghypebeastecommerce.dto.checkout.ReservationDTO;
import org.fyp.hunghypebeastecommerce.service.InventoryReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final InventoryReservationService reservationService;

    // Khi người dùng bấm checkout -> tạm thời giữ hàng để người khác không mua hết (trong 15 phút)
    // Hết 15 phút mà không thanh toán thì trả hàng về kho qua InventoryReservationScheduler
    @PostMapping("/reserve")
    public ResponseEntity<ResponseObject<ReservationDTO>> reserveInventory(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        ReservationDTO reservation = reservationService.reserveInventory(sessionId);
        return ResponseEntity.ok(ResponseObject.success("Inventory reserved for 15 minutes", reservation));
    }

    // Mục đích để kiểm tra và trả về thông tin reservation hiện tại của session
    // Ví dụ như để hiển thị cho fe biết hàng đang được giữa bao lâu nữa
    @GetMapping("/reservation")
    public ResponseEntity<ResponseObject<ReservationDTO>> getReservation(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        ReservationDTO reservation = reservationService.getActiveReservation(sessionId);
        if (reservation == null) {
            return ResponseEntity.ok(ResponseObject.success("No active reservation", null));
        }
        return ResponseEntity.ok(ResponseObject.success("Active reservation found", reservation));
    }

    @DeleteMapping("/reservation")
    public ResponseEntity<ResponseObject<Void>> cancelReservation(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        reservationService.releaseReservation(sessionId);
        return ResponseEntity.ok(ResponseObject.success("Reservation cancelled", null));
    }
}
