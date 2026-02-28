package com.oceanview.reservation_system.repository;

import com.oceanview.reservation_system.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByAvailableTrue();

    List<Room> findByRoomType(Room.RoomType roomType);

    List<Room> findByAvailableTrueAndRoomType(Room.RoomType roomType);

    boolean existsByRoomNumber(String roomNumber);

    /**
     * Returns rooms that are available (not under maintenance) AND have no
     * overlapping PENDING or CONFIRMED reservation for the requested date range.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE r.available = true
            AND (:roomType IS NULL OR r.roomType = :roomType)
            AND r.id NOT IN (
                SELECT res.room.id FROM Reservation res
                WHERE res.status IN ('PENDING', 'CONFIRMED')
                AND res.checkInDate  < :checkOut
                AND res.checkOutDate > :checkIn
            )
            """)
    List<Room> findAvailableRooms(
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("roomType") Room.RoomType roomType
    );
}
