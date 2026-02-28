package com.oceanview.reservation_system.repository;

import com.oceanview.reservation_system.model.Reservation;
import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationNumber(UUID reservationNumber);

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByRoomId(Long roomId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByGuestNameContainingIgnoreCase(String guestName);

    /**
     * Checks whether a room already has a PENDING or CONFIRMED reservation
     * that overlaps with the requested date range.
     * Uses the standard interval-overlap condition: existingStart < newEnd AND existingEnd > newStart
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.room.id  = :roomId
            AND   r.status  IN ('PENDING', 'CONFIRMED')
            AND   r.checkInDate  < :checkOut
            AND   r.checkOutDate > :checkIn
            """)
    boolean existsConflict(
            @Param("roomId")   Long roomId,
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
