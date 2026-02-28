package com.oceanview.reservation_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID reservationNumber;

    // ── Relationships ─────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // ── Guest Details (snapshot at booking time) ──────────────────────────────
    @Column(nullable = false, length = 100)
    private String guestName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String guestAddress;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    // ── Booking Details ───────────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    private LocalDateTime createdAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (reservationNumber == null) reservationNumber = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Nested Enum ───────────────────────────────────────────────────────────
    public enum ReservationStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        CHECKED_OUT
    }
}