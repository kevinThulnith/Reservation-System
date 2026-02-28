package com.oceanview.reservation_system.dto;

import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import com.oceanview.reservation_system.model.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private UUID reservationNumber;
    private String guestName;
    private String guestAddress;
    private String contactNumber;

    private String roomNumber;
    private Room.RoomType roomType;
    private Integer floor;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private long nights;

    private BigDecimal pricePerNight;
    private BigDecimal totalCost;

    private ReservationStatus status;
}
