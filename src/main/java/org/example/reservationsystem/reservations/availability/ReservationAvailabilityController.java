package org.example.reservationsystem.reservations.availability;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservation/availability")
@RequiredArgsConstructor
public class ReservationAvailabilityController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReservationAvailabilityController.class);
    private final ReservationAvailabilityService reservationAvailabilityService;

    @PostMapping("/check")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(
            @RequestBody @Valid CheckAvailabilityRequest request
    ){
        log.info("Called method checkAvailability: request ={}", request);
        boolean isAvailable = reservationAvailabilityService
                .isReservationAvailable(request.roomId(), request.startDate(), request.endDate());
        var message = isAvailable ? "Room available" : "Room not available";
        var status = isAvailable ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.RESERVED;
        return ResponseEntity.ok(new CheckAvailabilityResponse(message, status));
    }
}
