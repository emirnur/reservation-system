package org.example.reservationsystem;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id));
        return toDomainReservation(reservationEntity);
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allEntities = reservationRepository.findAll();
        return allEntities.stream()
                .map(this::toDomainReservation)
                .toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.id() != null) {
            throw new IllegalArgumentException("Reservation id must be null");
        }
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("Reservation status must be null");
        }
        ReservationEntity entityToSave = new ReservationEntity(
                null,
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        ReservationEntity savedEntity = reservationRepository.save(entityToSave);
        return toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = reservationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id)
        );
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with id = " + id
                    + " status = " + reservationEntity.getStatus());
        }
        var reservationToSave = new ReservationEntity(
                reservationEntity.getId(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        ReservationEntity updatedReservation = reservationRepository.save(reservationToSave);
        return toDomainReservation(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new EntityNotFoundException("Not found reservation by id = " + id);
        }
        reservationRepository.setStatus(id, ReservationStatus.CANCELLED);
        log.info("Successfully canceled reservation: id = {}", id);
    }

    public Reservation approveReservation(Long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id));
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with id = " + id
                    + " status = " + reservationEntity.getStatus());
        }
        var isConflict = isReservationConflict(reservationEntity);
        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with id = " + id
                    + " because of conflict");
        }
        reservationEntity.setStatus(ReservationStatus.APPROVED);
        reservationRepository.save(reservationEntity);
        return toDomainReservation(reservationEntity);
    }

    private boolean isReservationConflict(ReservationEntity reservation) {
        var allReservations = reservationRepository.findAll();
        for (ReservationEntity existingReservation : allReservations) {
            if (existingReservation.getId().equals(reservation.getId())) {
                continue;
            }
            if (!existingReservation.getRoomId().equals(reservation.getRoomId())) {
                continue;
            }
            if (existingReservation.getStatus() != ReservationStatus.APPROVED) {
                continue;
            }
            if (reservation.getStartDate().isBefore(existingReservation.getEndDate())
                    && existingReservation.getStartDate().isBefore(reservation.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    private Reservation toDomainReservation(ReservationEntity reservationEntity) {
        return new Reservation(
                reservationEntity.getId(),
                reservationEntity.getUserId(),
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate(),
                reservationEntity.getStatus()
        );
    }
}
