package com.airline.booking.demo.feature.passengers.repository.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Passenger {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String passportNumber;
    private LocalDate dateOfBirth;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
