package com.airline.booking.demo.feature.flights.repository.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Flight {

    private Long id;
    private String flightNumber;
    private Long airlineId;
    private String departureAirport;
    private String arrivalAirport;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal price;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
