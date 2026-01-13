package com.airline.booking.demo.feature.bookings.repository.impl;

import static com.airline.booking.demo.common.utils.AirlineBookingUtil.convertSqlDateTimeToOffset;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.PgErrorMapper;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.bookings.repository.BookingRepository;
import com.airline.booking.demo.feature.bookings.repository.entity.Booking;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class BookingRepositoryImpl implements BookingRepository {

    private static final String SQL_SELECT_BY_ID = """
                SELECT id, booking_reference, passenger_id, flight_id, seat_number,
                       status, total_amount, booking_date, created_at, updated_at
                FROM bookings WHERE id = ?
            """;

    private static final String SQL_SELECT_BY_PASSENGER = """
                SELECT id, booking_reference, passenger_id, flight_id, seat_number,
                       status, total_amount, booking_date, created_at, updated_at
                FROM bookings WHERE passenger_id = ?
            """;

    private static final String SQL_INSERT =
            "INSERT INTO bookings (booking_reference, passenger_id, flight_id, seat_number, status, total_amount) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE = """
                UPDATE bookings SET
                    status = ?, seat_number = ?, total_amount = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
            """;

    private final Pool pool;

    @Inject
    public BookingRepositoryImpl(final Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Booking> save(final Booking booking) {
        return pool.getConnection()
                .compose(conn -> save(conn, booking).onComplete(ar -> conn.close()));
    }

    @Override
    public Future<Booking> save(final SqlConnection conn, final Booking booking) {
        return booking.getId() == null ? insert(conn, booking) : update(conn, booking);
    }

    @Override
    public Future<Booking> findById(final Long id) {
        return pool.getConnection()
                .compose(conn -> findById(conn, id).onComplete(ar -> conn.close()));
    }

    @Override
    public Future<Booking> findById(final SqlConnection conn, final Long id) {
        return conn.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(new BusinessServiceException(
                                ErrorCodeEnum.ENTITY_NOT_FOUND, "Booking not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query booking")));
    }

    @Override
    public Future<List<Booking>> findByPassengerId(SqlConnection conn, Long passengerId) {
        return conn.preparedQuery(SQL_SELECT_BY_PASSENGER)
                .execute(Tuple.of(passengerId))
                .map(rows -> {
                    final List<Booking> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(map(row));
                    }
                    return list;
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query bookings")));
    }

    private Future<Booking> insert(final SqlConnection conn, final Booking booking) {
        return conn
                .preparedQuery(SQL_INSERT)
                .execute(Tuple.of(
                        booking.getBookingReference(), booking.getPassengerId(), booking.getFlightId(),
                        booking.getSeatNumber(), booking.getStatus(), booking.getTotalAmount()
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
                        PgErrorMapper.map(err, "Failed to insert booking")));
    }

    private Future<Booking> update(final SqlConnection conn, final Booking booking) {
        return conn.preparedQuery(SQL_UPDATE)
                .execute(Tuple.of(
                        booking.getStatus(),
                        booking.getSeatNumber(),
                        booking.getTotalAmount(),
                        booking.getId()
                ))
                .compose(v -> findById(conn, booking.getId()))
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to update booking")));
    }

    private Booking map(final Row row) {
        final Booking booking = new Booking();
        booking.setId(row.getLong("id"));
        booking.setBookingReference(row.getString("booking_reference"));
        booking.setPassengerId(row.getLong("passenger_id"));
        booking.setFlightId(row.getLong("flight_id"));
        booking.setSeatNumber(row.getString("seat_number"));
        booking.setStatus(row.getString("status"));
        booking.setTotalAmount(row.get(BigDecimal.class, "total_amount"));
        booking.setBookingDate(convertSqlDateTimeToOffset(row, "booking_date"));
        booking.setCreatedAt(convertSqlDateTimeToOffset(row, "created_at"));
        booking.setUpdatedAt(convertSqlDateTimeToOffset(row, "updated_at"));
        return booking;
    }
}
