package com.airline.booking.demo.feature.flights.repository;

import com.airline.booking.demo.feature.flights.repository.entity.Flight;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import java.util.List;

public interface FlightRepository {

    Future<Flight> findById(Long id);

    Future<List<Flight>> findByRoute(String from, String to);

    Future<Flight> save(Flight flight);

    Future<Flight> findByIdForUpdate(SqlConnection conn, Long id);

    Future<Flight> save(SqlConnection conn, Flight flight);
}
