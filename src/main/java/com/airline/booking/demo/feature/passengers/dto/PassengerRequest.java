package com.airline.booking.demo.feature.passengers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PassengerRequest(
        @NotBlank(message = "cant' be null or empty") String firstName,
        @NotBlank(message = "cant' be null or empty") String lastName,
        @NotBlank(message = "cant' be null or empty") @Email(message = "Invalid") String email,
        @NotBlank(message = "cant' be null or empty") String phone,
        @NotBlank(message = "cant' be null or empty") String passportNumber,
        @NotNull(message = "cant' be null or empty") String dateOfBirth
) {

}
