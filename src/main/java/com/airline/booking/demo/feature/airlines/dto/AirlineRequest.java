package com.airline.booking.demo.feature.airlines.dto;

import jakarta.validation.constraints.NotBlank;

public record AirlineRequest(@NotBlank(message = "cant' be null or empty") String code,
                             @NotBlank(message = "cant' be null or empty") String name,
                             @NotBlank(message = "cant' be null or empty") String country) {

}
