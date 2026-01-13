package com.airline.booking.demo.feature.bookings.service;

import com.airline.booking.demo.common.db.ReactiveTx;
import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.bookings.dto.BookingRequest;
import com.airline.booking.demo.feature.bookings.repository.BookingRepository;
import com.airline.booking.demo.feature.bookings.repository.entity.Booking;
import com.airline.booking.demo.feature.bookings.repository.entity.BookingStatus;
import com.airline.booking.demo.feature.flights.repository.FlightRepository;
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final ReactiveTx tx;
    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;

    @Inject
    public BookingService(final ReactiveTx tx,
            final FlightRepository flightRepository,
            final BookingRepository bookingRepository,
            final PassengerRepository passengerRepository) {
        this.tx = tx;
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
    }

    public Future<Booking> getById(final Long id) {
        log.debug("Fetching booking id={}", id);

        return bookingRepository.findById(id)
                .onSuccess(b -> log.info("Fetched booking id={} status={}", b.getId(), b.getStatus()))
                .onFailure(err -> log.error("Failed to fetch booking id={}", id, err));
    }

    public Future<Booking> create(final BookingRequest req) {
        final Long passengerId = req.passengerId();
        final Long flightId = req.flightId();
        final String seat = req.seatNumber();

        log.info("Creating booking passengerId={}, flightId={}, seat={}", passengerId, flightId, seat);

        return passengerRepository.findById(passengerId)
                .onSuccess(p -> log.debug("Passenger validation passed: id={}", passengerId))
                .onFailure(err -> log.error("Passenger not found id={}", passengerId, err))
                .compose(p -> tx.withTx(conn ->
                        flightRepository.findByIdForUpdate(conn, flightId)
                                .onSuccess(flight -> log.debug("Locked flight id={} availableSeats={}", flightId,
                                        flight.getAvailableSeats()))
                                .compose(flight -> {
                                    if (flight.getAvailableSeats() <= 0) {
                                        log.warn("Booking failed: no seats left for flightId={}", flightId);
                                        return Future.failedFuture(new BusinessServiceException(
                                                ErrorCodeEnum.INVALID_PARAM,
                                                "No seats available for flight " + flightId
                                        ));
                                    }

                                    flight.setAvailableSeats(flight.getAvailableSeats() - 1);

                                    final Booking b = new Booking();
                                    b.setPassengerId(passengerId);
                                    b.setFlightId(flightId);
                                    b.setSeatNumber(seat);
                                    b.setStatus(BookingStatus.CONFIRMED.name());
                                    b.setTotalAmount(flight.getPrice() != null ? flight.getPrice() : BigDecimal.ZERO);
                                    b.setBookingReference(generateBookingRef(flight.getFlightNumber()));

                                    log.debug("Booking entity prepared: passengerId={}, flightId={}, reference={}",
                                            passengerId, flightId, b.getBookingReference());

                                    return flightRepository.save(conn, flight)
                                            .onSuccess(f -> log.debug("Flight availability updated id={}", f.getId()))
                                            .compose(saved -> bookingRepository.save(conn, b))
                                            .onSuccess(saved -> log.info("Booking created id={} reference={}",
                                                    saved.getId(), saved.getBookingReference()))
                                            .onFailure(err -> log.error(
                                                    "Booking creation failed passengerId={}, flightId={}",
                                                    passengerId, flightId, err));
                                })
                ));
    }

    private String generateBookingRef(final String flightNumber) {
        return flightNumber + LocalDate.now().toString().replace("-", "");
    }

    public Future<Void> cancel(final Long id) {
        log.info("Cancelling booking id={}", id);

        return tx.withTx(conn ->
                bookingRepository.findById(conn, id)
                        .onFailure(err -> log.error("Cancel failed: booking not found id={}", id, err))
                        .compose(b -> {
                            b.setStatus(BookingStatus.CANCELLED.name());
                            return bookingRepository.save(conn, b)
                                    .onSuccess(x -> log.info("Booking cancelled id={}", id))
                                    .onFailure(err -> log.error("Failed to cancel booking id={}", id, err))
                                    .mapEmpty();
                        })
        );
    }

    public Future<List<Booking>> findByPassenger(final Long passengerId) {
        log.debug("Fetching bookings by passengerId={}", passengerId);

        return tx.withTx(conn -> bookingRepository.findByPassengerId(conn, passengerId))
                .onSuccess(list -> log.info("Found {} bookings for passengerId={}", list.size(), passengerId))
                .onFailure(err -> log.error("Failed to fetch bookings for passengerId={}", passengerId, err));
    }
}
