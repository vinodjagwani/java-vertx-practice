package com.airline.booking.demo.feature.passengers.dto;

import java.time.OffsetDateTime;

public record PassengerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String passportNumber,
        String dateOfBirth,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}
