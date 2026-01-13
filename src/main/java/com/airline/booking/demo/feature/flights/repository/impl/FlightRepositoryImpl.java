package com.airline.booking.demo.feature.flights.repository.impl;

import static com.airline.booking.demo.common.utils.AirlineBookingUtil.convertSqlDateTimeToOffset;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.PgErrorMapper;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.flights.repository.FlightRepository;
import com.airline.booking.demo.feature.flights.repository.entity.Flight;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FlightRepositoryImpl implements FlightRepository {

    private static final String SQL_SELECT_BY_ID = "SELECT * FROM flights WHERE id = ?";
    private static final String SQL_SELECT_ROUTE = "SELECT * FROM flights WHERE departure_airport=? AND arrival_airport=?";
    private static final String SQL_SELECT_FOR_UPDATE = "SELECT * FROM flights WHERE id=? FOR UPDATE";

    private static final String SQL_INSERT =
            "INSERT INTO flights (" +
                    "flight_number, airline_id, departure_airport, arrival_airport, " +
                    "departure_time, arrival_time, available_seats, total_seats, price, status" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE = """
                UPDATE flights SET
                    flight_number=?, airline_id=?, departure_airport=?, arrival_airport=?,
                    departure_time=?, arrival_time=?, available_seats=?, total_seats=?,
                    price=?, status=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
            """;

    private final Pool pool;

    @Inject
    public FlightRepositoryImpl(final Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Flight> findById(final Long id) {
        return pool.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Flight not found with id: " + id)
                        );
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query flight")));
    }

    @Override
    public Future<List<Flight>> findByRoute(final String from, final String to) {
        return pool.preparedQuery(SQL_SELECT_ROUTE)
                .execute(Tuple.of(from, to))
                .map(rows -> {
                    final List<Flight> list = new ArrayList<>();
                    rows.forEach(r -> list.add(map(r)));
                    return list;
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query flights")));
    }

    @Override
    public Future<Flight> findByIdForUpdate(final SqlConnection conn, final Long id) {
        return conn.preparedQuery(SQL_SELECT_FOR_UPDATE)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Flight not found with id: " + id)
                        );
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                });
    }

    @Override
    public Future<Flight> save(final Flight flight) {
        return pool.getConnection()
                .compose(conn -> save(conn, flight).onComplete(ar -> conn.close()));
    }

    @Override
    public Future<Flight> save(final SqlConnection conn, final Flight flight) {
        return flight.getId() == null ? insert(conn, flight) : update(conn, flight);
    }

    private Future<Flight> insert(final SqlConnection conn, final Flight flight) {
        return conn.preparedQuery(SQL_INSERT)
                .execute(Tuple.of(
                        flight.getFlightNumber(), flight.getAirlineId(), flight.getDepartureAirport(),
                        flight.getArrivalAirport(),
                        flight.getDepartureTime(), flight.getArrivalTime(), flight.getAvailableSeats(),
                        flight.getTotalSeats(), flight.getPrice(), flight.getStatus()
                ))
                .compose(v -> conn
                        .query("SELECT lastval() AS id")
                        .execute()
                )
                .compose(rows -> {
                    final Long id = rows.iterator().next().getLong("id");
                    return findById(conn, id);
                })
                .recover(err -> Future.failedFuture(
                        PgErrorMapper.map(err, "Failed to insert flight")));
    }

    private Future<Flight> findById(final SqlConnection conn, final Long id) {
        return conn.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Flight not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                });
    }

    private Future<Flight> update(final SqlConnection conn, final Flight flight) {
        return conn.preparedQuery(SQL_UPDATE)
                .execute(Tuple.of(
                        flight.getFlightNumber(), flight.getAirlineId(), flight.getDepartureAirport(),
                        flight.getArrivalAirport(),
                        flight.getDepartureTime(), flight.getArrivalTime(), flight.getAvailableSeats(),
                        flight.getTotalSeats(), flight.getPrice(), flight.getStatus(),
                        flight.getId()
                ))
                .compose(v -> findById(flight.getId()))
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to update flight")));
    }

    private Flight map(final Row row) {
        final Flight flight = new Flight();
        flight.setId(row.getLong("id"));
        flight.setFlightNumber(row.getString("flight_number"));
        flight.setAirlineId(row.getLong("airline_id"));
        flight.setDepartureAirport(row.getString("departure_airport"));
        flight.setArrivalAirport(row.getString("arrival_airport"));
        flight.setDepartureTime(convertSqlDateTimeToOffset(row, "departure_time"));
        flight.setArrivalTime(convertSqlDateTimeToOffset(row, "arrival_time"));
        flight.setAvailableSeats(row.getInteger("available_seats"));
        flight.setTotalSeats(row.getInteger("total_seats"));
        flight.setPrice(row.getBigDecimal("price"));
        flight.setStatus(row.getString("status"));
        flight.setCreatedAt(convertSqlDateTimeToOffset(row, "created_at"));
        flight.setUpdatedAt(convertSqlDateTimeToOffset(row, "updated_at"));
        return flight;
    }
}
