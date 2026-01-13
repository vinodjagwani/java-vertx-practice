package com.airline.booking.demo.feature.flights.dto;

import java.math.BigDecimal;

public record FlightResponse(
        Long id,
        String flightNumber,
        Long airlineId,
        String departureAirport,
        String arrivalAirport,
        String departureTime,
        String arrivalTime,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal price,
        String status,
        String createdAt,
        String updatedAt
) {

}
