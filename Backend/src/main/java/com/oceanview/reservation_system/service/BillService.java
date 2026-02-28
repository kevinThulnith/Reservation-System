package com.oceanview.reservation_system.service;

import com.oceanview.reservation_system.dto.BillResponse;
import com.oceanview.reservation_system.model.Reservation;
import com.oceanview.reservation_system.model.Room;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

@Service
public class BillService {

    public BillResponse generateBill(Reservation reservation) {
        Room room = reservation.getRoom();
        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());

        return BillResponse.builder()
                .reservationNumber(reservation.getReservationNumber())
                .guestName(reservation.getGuestName())
                .guestAddress(reservation.getGuestAddress())
                .contactNumber(reservation.getContactNumber())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .floor(room.getFloor())
                .checkInDate(reservation.getCheckInDate())
                .checkOutDate(reservation.getCheckOutDate())
                .nights(nights)
                .pricePerNight(room.getPricePerNight())
                .totalCost(reservation.getTotalCost())
                .status(reservation.getStatus())
                .build();
    }
}
