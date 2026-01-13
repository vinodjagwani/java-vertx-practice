package com.airline.booking.demo.feature.passengers.service;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest;
import com.airline.booking.demo.feature.passengers.mapper.PassengerMapper;
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository;
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PassengerService {

    private static final Logger log = LoggerFactory.getLogger(PassengerService.class);

    private final PassengerRepository passengerRepository;

    @Inject
    public PassengerService(final PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    public Future<Passenger> create(final PassengerRequest req) {
        log.debug("Attempting to create passenger: email={}, passport={}",
                req.email(), req.passportNumber());

        final Passenger passenger = PassengerMapper.toEntity(req);

        return passengerRepository.save(passenger)
                .onSuccess(saved ->
                        log.info("Passenger created successfully with id={}", saved.getId())
                );
    }

    public Future<Passenger> getById(final Long id) {
        log.debug("Fetching passenger by id={}", id);
        return passengerRepository.findById(id)
                .compose(p -> {
                    if (p == null) {
                        log.warn("Passenger not found id={}", id);
                        return Future.failedFuture(new BusinessServiceException(
                                ErrorCodeEnum.ENTITY_NOT_FOUND,
                                "Passenger not found: " + id
                        ));
                    }
                    log.debug("Passenger found id={}, email={}", p.getId(), p.getEmail());
                    return Future.succeededFuture(p);
                });
    }
}
