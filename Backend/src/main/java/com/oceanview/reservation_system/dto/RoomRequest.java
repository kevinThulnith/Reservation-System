package com.oceanview.reservation_system.dto;

import com.oceanview.reservation_system.model.Room;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private Room.RoomType roomType;

    private String description;

    private String imageUrl;

    @Min(value = 1, message = "Floor must be at least 1")
    private Integer floor;

    @Min(value = 1, message = "Max occupancy must be at least 1")
    private Integer maxOccupancy;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerNight;
}
