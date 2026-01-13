package com.airline.booking.demo.feature.bookings.mapper;

import com.airline.booking.demo.feature.bookings.dto.BookingRequest;
import com.airline.booking.demo.feature.bookings.dto.BookingResponse;
import com.airline.booking.demo.feature.bookings.repository.entity.Booking;

public final class BookingMapper {

    private BookingMapper() {
        // Empty Constructor
    }

    public static Booking toEntity(final BookingRequest req) {
        final Booking booking = new Booking();
        booking.setPassengerId(req.passengerId());
        booking.setFlightId(req.flightId());
        booking.setSeatNumber(req.seatNumber());
        return booking;
    }

    public static BookingResponse toResponse(final Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getPassengerId(),
                booking.getFlightId(),
                booking.getSeatNumber(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getBookingDate(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
