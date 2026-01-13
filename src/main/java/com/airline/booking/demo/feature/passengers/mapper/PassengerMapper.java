package com.airline.booking.demo.feature.passengers.mapper;

import com.airline.booking.demo.feature.passengers.dto.PassengerRequest;
import com.airline.booking.demo.feature.passengers.dto.PassengerResponse;
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PassengerMapper {

    private PassengerMapper() {
        // Empty Constructor
    }

    public static Passenger toEntity(final PassengerRequest req) {
        if (req == null) {
            return null;
        }

        Passenger passenger = new Passenger();
        passenger.setFirstName(req.firstName());
        passenger.setLastName(req.lastName());
        passenger.setEmail(req.email());
        passenger.setPhone(req.phone());
        passenger.setPassportNumber(req.passportNumber());
        passenger.setDateOfBirth(LocalDate.parse(req.dateOfBirth(), DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        return passenger;
    }

    public static PassengerResponse toResponse(final Passenger entity) {
        if (entity == null) {
            return null;
        }

        return new PassengerResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPassportNumber(),
                entity.getDateOfBirth().toString(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
