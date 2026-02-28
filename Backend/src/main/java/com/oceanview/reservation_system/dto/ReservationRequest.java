package com.oceanview.reservation_system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Guest name is required")
    @Size(min = 2, max = 100, message = "Guest name must be between 2 and 100 characters")
    private String guestName;

    @NotBlank(message = "Guest address is required")
    private String guestAddress;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Invalid contact number")
    private String contactNumber;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or a future date")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be a future date")
    private LocalDate checkOutDate;
}
