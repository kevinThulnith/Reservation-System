package com.oceanview.reservation_system.controller;

import com.oceanview.reservation_system.dto.BillResponse;
import com.oceanview.reservation_system.dto.ReservationRequest;
import com.oceanview.reservation_system.dto.ReservationResponse;
import com.oceanview.reservation_system.model.Reservation.ReservationStatus;
import com.oceanview.reservation_system.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ── User endpoints ────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reservationService.getMyReservations(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/number/{reservationNumber}")
    public ResponseEntity<ReservationResponse> getByReservationNumber(
            @PathVariable UUID reservationNumber) {
        return ResponseEntity.ok(reservationService.getByReservationNumber(reservationNumber));
    }

    @GetMapping("/{id}/bill")
    public ResponseEntity<BillResponse> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getBill(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, userDetails.getUsername()));
    }

    // ── Admin-only endpoints ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
