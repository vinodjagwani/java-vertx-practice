package com.airline.booking.demo.feature.bookings.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long passengerId,
        Long flightId,
        String seatNumber,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime bookingDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}
