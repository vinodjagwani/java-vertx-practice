package com.airline.booking.demo.feature.airlines.repository;

import com.airline.booking.demo.feature.airlines.repository.entity.Airline;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import java.util.List;

public interface AirlineRepository {

    Future<Airline> findById(Long id);

    Future<Airline> findByCode(String code);

    Future<List<Airline>> findAll();

    Future<Airline> save(Airline airline);

    Future<Airline> findByCodeOptional(String code);

    Future<Airline> save(SqlConnection conn, Airline airline);
}
