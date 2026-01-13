package com.airline.booking.demo.feature.bookings.repository.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Booking {

    private Long id;
    private String bookingReference;
    private Long passengerId;
    private Long flightId;
    private String seatNumber;
    private String status;
    private BigDecimal totalAmount;
    private OffsetDateTime bookingDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
