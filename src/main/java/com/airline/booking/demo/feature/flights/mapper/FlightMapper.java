package com.airline.booking.demo.feature.flights.mapper;

import com.airline.booking.demo.feature.flights.dto.FlightRequest;
import com.airline.booking.demo.feature.flights.dto.FlightResponse;
import com.airline.booking.demo.feature.flights.repository.entity.Flight;
import com.airline.booking.demo.feature.flights.repository.entity.FlightStatus;
import java.time.OffsetDateTime;

public final class FlightMapper {

    private FlightMapper() {
    }

    public static Flight toEntity(final FlightRequest req) {
        Flight f = new Flight();
        f.setFlightNumber(req.flightNumber());
        f.setAirlineId(req.airlineId());
        f.setDepartureAirport(req.departureAirport());
        f.setArrivalAirport(req.arrivalAirport());
        f.setDepartureTime(OffsetDateTime.parse(req.departureTime()));
        f.setArrivalTime(OffsetDateTime.parse(req.arrivalTime()));
        f.setTotalSeats(req.totalSeats());
        f.setAvailableSeats(req.availableSeats() == null ? req.totalSeats() : req.availableSeats());
        f.setPrice(req.price());
        f.setStatus(FlightStatus.SCHEDULED.name());
        return f;
    }

    public static FlightResponse toResponse(final Flight f) {
        return new FlightResponse(
                f.getId(),
                f.getFlightNumber(),
                f.getAirlineId(),
                f.getDepartureAirport(),
                f.getArrivalAirport(),
                f.getDepartureTime().toString(),
                f.getArrivalTime().toString(),
                f.getTotalSeats(),
                f.getAvailableSeats(),
                f.getPrice(),
                f.getStatus(),
                f.getCreatedAt() != null ? f.getCreatedAt().toString() : null,
                f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null
        );
    }
}
