package org.example.reservationsystem.reservations.availability;

import lombok.RequiredArgsConstructor;
import org.example.reservationsystem.reservations.ReservationRepository;
import org.example.reservationsystem.reservations.ReservationStatus;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReservationAvailabilityService.class);
    private final ReservationRepository reservationRepository;

    public boolean isReservationAvailable(Long roomId, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be 1 day after than start date");
        }
        List<Long> conflictReservationIds = reservationRepository.findConflictReservationIds(
                roomId, startDate, endDate, ReservationStatus.APPROVED
        );
        if (conflictReservationIds.isEmpty()) {
            return true;
        }
        log.info("Found conflict reservation ids: {}", conflictReservationIds);
        return false;
    }
}
