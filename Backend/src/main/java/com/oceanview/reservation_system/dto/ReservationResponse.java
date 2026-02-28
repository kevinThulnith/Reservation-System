package com.oceanview.reservation_system.dto;

import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private UUID reservationNumber;

    // Guest
    private String guestName;
    private String guestAddress;
    private String contactNumber;

    // Booking
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalCost;
    private ReservationStatus status;
    private LocalDateTime createdAt;

    // Relations (flattened for convenience)
    private String username;
    private RoomResponse room;
}
