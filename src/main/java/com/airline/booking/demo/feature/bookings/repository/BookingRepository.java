package com.airline.booking.demo.feature.bookings.repository;

import com.airline.booking.demo.feature.bookings.repository.entity.Booking;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import java.util.List;

public interface BookingRepository {

    Future<Booking> save(Booking booking);

    Future<Booking> save(SqlConnection conn, Booking booking);

    Future<Booking> findById(Long id);

    Future<Booking> findById(SqlConnection conn, Long id);

    Future<List<Booking>> findByPassengerId(SqlConnection conn, Long passengerId);
}
