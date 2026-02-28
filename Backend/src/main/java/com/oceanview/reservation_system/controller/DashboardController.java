package com.oceanview.reservation_system.controller;

import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import com.oceanview.reservation_system.repository.ReservationRepository;
import com.oceanview.reservation_system.repository.RoomRepository;
import com.oceanview.reservation_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalReservations = reservationRepository.count();
        long confirmedReservations = reservationRepository.findByStatus(ReservationStatus.CONFIRMED).size();
        long pendingReservations = reservationRepository.findByStatus(ReservationStatus.PENDING).size();
        long cancelledReservations = reservationRepository.findByStatus(ReservationStatus.CANCELLED).size();
        long checkedOutReservations = reservationRepository.findByStatus(ReservationStatus.CHECKED_OUT).size();

        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findByAvailableTrue().size();

        long totalUsers = userRepository.count();

        BigDecimal totalRevenue = reservationRepository.findByStatus(ReservationStatus.CONFIRMED).stream()
                .map(r -> r.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(reservationRepository.findByStatus(ReservationStatus.CHECKED_OUT).stream()
                        .map(r -> r.getTotalCost())
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReservations", totalReservations);
        stats.put("confirmedReservations", confirmedReservations);
        stats.put("pendingReservations", pendingReservations);
        stats.put("cancelledReservations", cancelledReservations);
        stats.put("checkedOutReservations", checkedOutReservations);
        stats.put("totalRooms", totalRooms);
        stats.put("availableRooms", availableRooms);
        stats.put("occupiedRooms", totalRooms - availableRooms);
        stats.put("totalUsers", totalUsers);
        stats.put("totalRevenue", totalRevenue);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueByRoomType() {
        Map<String, BigDecimal> revenueByType = new LinkedHashMap<>();

        reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        || r.getStatus() == ReservationStatus.CHECKED_OUT)
                .forEach(r -> {
                    String type = r.getRoom().getRoomType().name();
                    revenueByType.merge(type, r.getTotalCost(), BigDecimal::add);
                });

        return ResponseEntity.ok(Map.of("revenueByRoomType", revenueByType));
    }
}
