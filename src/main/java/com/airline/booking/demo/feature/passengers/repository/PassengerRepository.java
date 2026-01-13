package com.airline.booking.demo.feature.passengers.repository;

import com.airline.booking.demo.feature.passengers.repository.entity.Passenger;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;

public interface PassengerRepository {

    Future<Passenger> save(Passenger p);

    Future<Passenger> save(SqlConnection conn, Passenger p);

    Future<Passenger> findById(Long id);
}
