package com.oceanview.reservation_system.controller;

import com.oceanview.reservation_system.dto.RoomRequest;
import com.oceanview.reservation_system.dto.RoomResponse;
import com.oceanview.reservation_system.model.Room;
import com.oceanview.reservation_system.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms() {
        return ResponseEntity.ok(roomService.getAvailableRooms());
    }

    /**
     * Search available rooms by date range and optional room type.
     * e.g. GET /api/rooms/search?checkIn=2026-03-01&checkOut=2026-03-05&type=DOUBLE
     */
    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> searchRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Room.RoomType type) {
        return ResponseEntity.ok(roomService.searchAvailableRooms(checkIn, checkOut, type));
    }

    @GetMapping("/type/{roomType}")
    public ResponseEntity<List<RoomResponse>> getRoomsByType(@PathVariable Room.RoomType roomType) {
        return ResponseEntity.ok(roomService.getRoomsByType(roomType));
    }

    // ── Admin-only endpoints ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<RoomResponse> addRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.addRoom(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<RoomResponse> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.toggleAvailability(id));
    }
}
