package com.oceanview.reservation_system.service;

import com.oceanview.reservation_system.dto.RoomRequest;
import com.oceanview.reservation_system.dto.RoomResponse;
import com.oceanview.reservation_system.exeption.ResourceNotFoundException;
import com.oceanview.reservation_system.model.Room;
import com.oceanview.reservation_system.repository.RoomRepository;
import com.oceanview.reservation_system.service.pricing.PricingStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final PricingStrategyFactory pricingStrategyFactory;

    // ── Mappers ───────────────────────────────────────────────────────────────

    public RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .description(room.getDescription())
                .imageUrl(room.getImageUrl())
                .floor(room.getFloor())
                .maxOccupancy(room.getMaxOccupancy())
                .pricePerNight(room.getPricePerNight())
                .available(room.getAvailable())
                .build();
    }

    // ── Queries (public) ──────────────────────────────────────────────────────

    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse getRoomById(Long id) {
        return toResponse(findRoomOrThrow(id));
    }

    public List<RoomResponse> getAvailableRooms() {
        return roomRepository.findByAvailableTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, Room.RoomType roomType) {
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
        return roomRepository.findAvailableRooms(checkIn, checkOut, roomType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getRoomsByType(Room.RoomType roomType) {
        return roomRepository.findByRoomType(roomType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin mutations ───────────────────────────────────────────────────────

    public RoomResponse addRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new IllegalArgumentException("Room number already exists: " + request.getRoomNumber());
        }

        // Use default pricing if no price supplied (price is @NotNull in request, but kept for safety)
        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomType(request.getRoomType())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .floor(request.getFloor())
                .maxOccupancy(request.getMaxOccupancy())
                .pricePerNight(request.getPricePerNight() != null
                        ? request.getPricePerNight()
                        : pricingStrategyFactory.getStrategy(request.getRoomType()).getDefaultNightlyRate())
                .available(true)
                .build();

        return toResponse(roomRepository.save(room));
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoomOrThrow(id);

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(request.getRoomType());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        room.setFloor(request.getFloor());
        room.setMaxOccupancy(request.getMaxOccupancy());
        room.setPricePerNight(request.getPricePerNight());

        return toResponse(roomRepository.save(room));
    }

    public void deleteRoom(Long id) {
        findRoomOrThrow(id);
        roomRepository.deleteById(id);
    }

    public RoomResponse toggleAvailability(Long id) {
        Room room = findRoomOrThrow(id);
        room.setAvailable(!room.getAvailable());
        return toResponse(roomRepository.save(room));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }
}
