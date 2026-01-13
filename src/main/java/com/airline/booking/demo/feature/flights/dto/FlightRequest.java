package com.airline.booking.demo.feature.flights.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FlightRequest(
        @NotBlank(message = "cant' be null or empty") String flightNumber,
        @NotNull(message = "cant' be null or empty") Long airlineId,
        @NotBlank(message = "cant' be null or empty") String departureAirport,
        @NotBlank(message = "cant' be null or empty") String arrivalAirport,
        @NotBlank(message = "cant' be null or empty") String departureTime,
        @NotBlank(message = "cant' be null or empty") String arrivalTime,
        @NotNull(message = "cant' be null or empty") Integer totalSeats,
        @NotNull(message = "cant' be null or empty") Integer availableSeats,
        @NotNull(message = "cant' be null or empty") BigDecimal price
) {

}
