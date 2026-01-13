package com.airline.booking.demo.feature.bookings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "cant' be null or empty") Long passengerId,
        @NotNull(message = "cant' be null or empty") Long flightId,
        @NotBlank(message = "cant' be null or empty") String seatNumber
) {

}
