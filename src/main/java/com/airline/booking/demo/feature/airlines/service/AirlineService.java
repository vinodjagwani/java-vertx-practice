package com.airline.booking.demo.feature.airlines.service;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.airlines.dto.AirlineRequest;
import com.airline.booking.demo.feature.airlines.mapper.AirlineMapper;
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository;
import com.airline.booking.demo.feature.airlines.repository.entity.Airline;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AirlineService {

    private static final Logger log = LoggerFactory.getLogger(AirlineService.class);

    private final AirlineRepository airlineRepository;

    @Inject
    public AirlineService(final AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    public Future<Airline> create(final AirlineRequest request) {
        log.info("Create airline request received: code={}", request.code());

        return airlineRepository.findByCodeOptional(request.code())
                .compose(existing -> {
                    if (existing != null) {
                        log.error("Airline creation failed: code={} already exists", request.code());
                        return Future.failedFuture(new BusinessServiceException(
                                ErrorCodeEnum.CONFLICT,
                                "Airline code already exists: " + request.code()
                        ));
                    }

                    final Airline entity = AirlineMapper.toEntity(request);
                    log.debug("Saving new airline entity: {}", entity);

                    return airlineRepository.save(entity)
                            .onSuccess(saved -> log.info("Airline created successfully: id={}", saved.getId()))
                            .onFailure(err -> log.error("Airline creation failed code={}", request.code(), err));
                });
    }


    public Future<List<Airline>> getAll() {
        log.debug("Fetching all airlines");
        return airlineRepository.findAll()
                .onSuccess(list -> log.info("Fetched {} airlines", list.size()))
                .onFailure(err -> log.error("Failed to fetch airlines", err));
    }

    public Future<Airline> getById(final Long id) {
        log.debug("Fetching airline by id={}", id);
        return airlineRepository.findById(id)
                .onSuccess(a -> log.info("Fetched airline: id={}", a.getId()))
                .onFailure(err -> log.error("Failed to fetch airline id={}", id, err));
    }
}
