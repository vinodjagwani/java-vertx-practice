package com.airline.booking.demo.feature.airlines.mapper;

import com.airline.booking.demo.feature.airlines.dto.AirlineRequest;
import com.airline.booking.demo.feature.airlines.dto.AirlineResponse;
import com.airline.booking.demo.feature.airlines.repository.entity.Airline;

public final class AirlineMapper {

    private AirlineMapper() {
        // Empty Constructor
    }

    public static Airline toEntity(final AirlineRequest req) {
        final Airline airline = new Airline();
        airline.setCode(req.code());
        airline.setName(req.name());
        airline.setCountry(req.country());
        return airline;
    }

    public static AirlineResponse toResponse(final Airline entity) {
        return new AirlineResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCountry()
        );
    }
}
