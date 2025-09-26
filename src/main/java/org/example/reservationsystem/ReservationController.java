package org.example.reservationsystem;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReservationController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReservationController.class);
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable("id") Long id) {
        log.info("Called getReservationById: id = {}", id);
        return reservationService.getReservationById(id);
    }

    @GetMapping()
    public List<Reservation> getAllReservations() {
        log.info("Called getAllReservations");
        return reservationService.findAllReservations();
    }
}
