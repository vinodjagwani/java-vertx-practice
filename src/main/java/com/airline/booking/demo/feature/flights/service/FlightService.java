package com.airline.booking.demo.feature.flights.service;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository;
import com.airline.booking.demo.feature.flights.dto.FlightRequest;
import com.airline.booking.demo.feature.flights.mapper.FlightMapper;
import com.airline.booking.demo.feature.flights.repository.FlightRepository;
import com.airline.booking.demo.feature.flights.repository.entity.Flight;
import com.airline.booking.demo.feature.flights.repository.entity.FlightStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;

    @Inject
    public FlightService(final FlightRepository flightRepository,
            final AirlineRepository airlineRepository) {
        this.flightRepository = flightRepository;
        this.airlineRepository = airlineRepository;
    }

    public Future<Flight> getById(final Long id) {
        log.debug("Fetching flight id={}", id);
        return flightRepository.findById(id);
    }

    public Future<List<Flight>> search(final String from, final String to) {
        log.debug("Searching flights from={} to={}", from, to);
        return flightRepository.findByRoute(from, to)
                .onSuccess(list ->
                        log.info("Found {} flights from={} to={}", list.size(), from, to)
                );
    }

    public Future<Flight> create(final FlightRequest req) {
        log.debug("Creating flight number={} airlineId={}",
                req.flightNumber(), req.airlineId());

        final Flight flight = FlightMapper.toEntity(req);

        return airlineRepository.findById(req.airlineId())
                .compose(a -> {
                    if (flight.getAvailableSeats() == null) {
                        flight.setAvailableSeats(flight.getTotalSeats());
                        log.debug("Setting availableSeats to totalSeats={}", flight.getTotalSeats());
                    }
                    if (flight.getStatus() == null) {
                        flight.setStatus(FlightStatus.SCHEDULED.name());
                        log.debug("Setting default status=SCHEDULED");
                    }

                    return flightRepository.save(flight);
                })
                .onFailure(err -> {
                    if (!(err instanceof BusinessServiceException)) {
                        log.error("Unexpected error creating flight", err);
                    }
                });
    }
}
