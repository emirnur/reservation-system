package org.example.reservationsystem;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservationService {

    private final Map<Long, Reservation> reservationMap;
    private final AtomicLong idCounter;

    public ReservationService() {
        reservationMap = new HashMap<>();
        idCounter = new AtomicLong();

    }

    public Reservation getReservationById(Long id) {
        if(!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation by id = " + id);
        }
        return reservationMap.get(id);
    }

    public List<Reservation> findAllReservations() {
        return reservationMap.values().stream().toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.id() != null) {
            throw new IllegalArgumentException("Reservation id must be null");
        }
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("Reservation status must be null");
        }
        var newReservation = new Reservation(
                idCounter.incrementAndGet(),
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        reservationMap.put(newReservation.id(), newReservation);
        return newReservation;
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation by id = " + id);
        }
        var reservation = reservationMap.get(id);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with id = " + id
                    + " status = " + reservation.status());
        }
        var updatedReservation = new Reservation(
                id,
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        reservationMap.put(reservation.id(), updatedReservation);
        return updatedReservation;
    }

    public void deleteReservation(Long id) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation by id = " + id);
        }
        reservationMap.remove(id);
    }

    public Reservation approveReservation(Long id) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation by id = " + id);
        }
        var reservation  = reservationMap.get(id);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with id = " + id
                    + " status = " + reservation.status());
        }
        var isConflict = isReservationConflict(reservation);
        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with id = " + id
                    + " because of conflict");
        }
        var approvedReservation = new Reservation(
                reservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.APPROVED
        );
        reservationMap.put(reservation.id(), approvedReservation);
        return approvedReservation;
    }

    private boolean isReservationConflict(Reservation reservation) {
        for (Reservation existingReservation : reservationMap.values()) {
            if (existingReservation.id().equals(reservation.id())) {
                continue;
            }
            if (!existingReservation.roomId().equals(reservation.roomId())) {
                continue;
            }
            if (reservation.startDate().isBefore(existingReservation.endDate())
                    && existingReservation.startDate().isBefore(reservation.startDate())) {
                return true;
            }
        }
        return false;
    }
}
