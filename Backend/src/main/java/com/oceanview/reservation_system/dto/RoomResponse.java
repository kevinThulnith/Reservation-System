package com.oceanview.reservation_system.dto;

import com.oceanview.reservation_system.model.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private Room.RoomType roomType;
    private String description;
    private String imageUrl;
    private Integer floor;
    private Integer maxOccupancy;
    private BigDecimal pricePerNight;
    private Boolean available;
}
