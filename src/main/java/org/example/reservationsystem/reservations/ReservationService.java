package org.example.reservationsystem.reservations;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id));
        return reservationMapper.toDomain(reservationEntity);
    }

    public List<Reservation> searchAllByFilter(
            ReservationSearchFilter filter
    ) {
        int pageSize = filter.pageSize() != null ? filter.pageSize() : 10;
        int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;
        var pageable = Pageable.ofSize(pageSize).withPage(pageNumber);

        List<ReservationEntity> allEntities = reservationRepository.searchAllByFilter(
                filter.roomId(),
                filter.userId(),
                pageable
        );
        return allEntities.stream()
                .map(reservationMapper::toDomain)
                .toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("Reservation status must be null");
        }
        if (!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())) {
            throw new IllegalArgumentException("End date must be 1 day after than start date");
        }
        ReservationEntity entityToSave = reservationMapper.toEntity(reservationToCreate);
        entityToSave.setStatus(ReservationStatus.PENDING);
        ReservationEntity savedEntity = reservationRepository.save(entityToSave);
        return reservationMapper.toDomain(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = reservationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id)
        );
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with id = " + id
                    + " status = " + reservationEntity.getStatus());
        }
        if (!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())) {
            throw new IllegalArgumentException("End date must be 1 day after than start date");
        }
        var reservationToSave = reservationMapper.toEntity(reservationToUpdate);
        reservationToSave.setId(reservationToSave.getId());
        reservationToSave.setStatus(ReservationStatus.PENDING);

        ReservationEntity updatedReservation = reservationRepository.save(reservationToSave);
        return reservationMapper.toDomain(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));
        if (reservationEntity.getStatus().equals(ReservationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot cancel approved reservation");
        }
        if (reservationEntity.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("Reservation with id = " + id + " is already cancelled");
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
        var isConflict = isReservationConflict(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );
        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with id = " + id
                    + " because of conflict");
        }
        reservationEntity.setStatus(ReservationStatus.APPROVED);
        reservationRepository.save(reservationEntity);
        return reservationMapper.toDomain(reservationEntity);
    }

    private boolean isReservationConflict(Long roomId, LocalDate startDate, LocalDate endDate) {
        List<Long> conflictReservationIds = reservationRepository.findConflictReservationIds(
                roomId, startDate, endDate, ReservationStatus.APPROVED
        );
        if (conflictReservationIds.isEmpty()) {
            return false;
        }
        log.info("Found conflict reservation ids: {}", conflictReservationIds);
        return true;
    }
}
