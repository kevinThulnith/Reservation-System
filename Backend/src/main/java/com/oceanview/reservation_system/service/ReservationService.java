package com.oceanview.reservation_system.service;

import com.oceanview.reservation_system.dto.BillResponse;
import com.oceanview.reservation_system.dto.ReservationRequest;
import com.oceanview.reservation_system.dto.ReservationResponse;
import com.oceanview.reservation_system.exeption.ResourceNotFoundException;
import com.oceanview.reservation_system.model.Reservation;
import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import com.oceanview.reservation_system.model.Room;
import com.oceanview.reservation_system.model.User;
import com.oceanview.reservation_system.repository.ReservationRepository;
import com.oceanview.reservation_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final BillService billService;

    // ── Mapper ────────────────────────────────────────────────────────────────

    public ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .reservationNumber(r.getReservationNumber())
                .guestName(r.getGuestName())
                .guestAddress(r.getGuestAddress())
                .contactNumber(r.getContactNumber())
                .checkInDate(r.getCheckInDate())
                .checkOutDate(r.getCheckOutDate())
                .totalCost(r.getTotalCost())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .username(r.getUser().getUsername())
                .room(roomService.toResponse(r.getRoom()))
                .build();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public ReservationResponse createReservation(ReservationRequest request, String username) {
        // Validate dates
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        // Fetch user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Fetch and validate room
        Room room = roomService.findRoomOrThrow(request.getRoomId());
        if (!room.getAvailable()) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is not available for booking");
        }

        // Check date conflicts
        boolean conflict = reservationRepository.existsConflict(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());
        if (conflict) {
            throw new IllegalStateException(
                    "Room " + room.getRoomNumber() + " is already booked for the selected dates");
        }

        // Compute total cost
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalCost = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Reservation reservation = Reservation.builder()
                .user(user)
                .room(room)
                .guestName(request.getGuestName())
                .guestAddress(request.getGuestAddress())
                .contactNumber(request.getContactNumber())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalCost(totalCost)
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return reservationRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReservationResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ReservationResponse getByReservationNumber(UUID reservationNumber) {
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationNumber));
        return toResponse(reservation);
    }

    public BillResponse getBill(Long id) {
        return billService.generateBill(findOrThrow(id));
    }

    // ── Status mutations ──────────────────────────────────────────────────────

    public ReservationResponse cancelReservation(Long id, String username) {
        Reservation reservation = findOrThrow(id);

        // Users can only cancel their own reservations
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new IllegalStateException("You are not authorised to cancel this reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled");
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Cannot cancel a checked-out reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateStatus(Long id, ReservationStatus newStatus) {
        Reservation reservation = findOrThrow(id);
        reservation.setStatus(newStatus);
        return toResponse(reservationRepository.save(reservation));
    }

    public void deleteReservation(Long id) {
        findOrThrow(id);
        reservationRepository.deleteById(id);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }
}
