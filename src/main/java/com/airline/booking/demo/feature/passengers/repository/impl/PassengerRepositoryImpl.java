package com.airline.booking.demo.feature.passengers.repository.impl;

import static com.airline.booking.demo.common.utils.AirlineBookingUtil.convertSqlDateTimeToOffset;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.PgErrorMapper;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository;
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

@Singleton
public class PassengerRepositoryImpl implements PassengerRepository {

    private static final String SQL_SELECT_BY_ID =
            "SELECT * FROM passengers WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO passengers (first_name, last_name, email, phone, passport_number, date_of_birth) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE passengers SET first_name=?, last_name=?, email=?, phone=?, passport_number=?, date_of_birth=?, " +
                    "updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private final Pool pool;

    @Inject
    public PassengerRepositoryImpl(final Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Passenger> findById(final Long id) {
        return pool.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Passenger not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query passenger")));
    }


    @Override
    public Future<Passenger> save(final Passenger passenger) {
        return pool.getConnection()
                .compose(conn -> save(conn, passenger)
                        .onComplete(ar -> conn.close()));
    }

    @Override
    public Future<Passenger> save(final SqlConnection conn, final Passenger passenger) {
        return passenger.getId() == null ? insert(conn, passenger) : update(conn, passenger);
    }

    private Future<Passenger> insert(final SqlConnection conn, final Passenger passenger) {
        return conn.preparedQuery(SQL_INSERT)
                .execute(Tuple.of(
                        passenger.getFirstName(), passenger.getLastName(), passenger.getEmail(),
                        passenger.getPhone(), passenger.getPassportNumber(), passenger.getDateOfBirth()
                ))
                .compose(v -> conn.query("SELECT lastval() AS id").execute())
                .compose(rows -> {
                    final Long id = rows.iterator().next().getLong("id");
                    return findById(conn, id);
                })
                .recover(err -> Future.failedFuture(
                        PgErrorMapper.map(err, "Failed to insert passenger")));
    }

    private Future<Passenger> findById(final SqlConnection conn, final Long id) {
        return conn.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Passenger not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                });
    }

    private Future<Passenger> update(final SqlConnection conn, final Passenger passenger) {
        return conn.preparedQuery(SQL_UPDATE)
                .execute(Tuple.of(
                        passenger.getFirstName(), passenger.getLastName(), passenger.getEmail(),
                        passenger.getPhone(), passenger.getPassportNumber(), passenger.getDateOfBirth(),
                        passenger.getId()
                ))
                .compose(v -> findById(conn, passenger.getId()))
                .recover(err -> Future.failedFuture(
                        PgErrorMapper.map(err, "Failed to update passenger")));
    }

    private Passenger map(final Row row) {
        Passenger passenger = new Passenger();
        passenger.setId(row.getLong("id"));
        passenger.setFirstName(row.getString("first_name"));
        passenger.setLastName(row.getString("last_name"));
        passenger.setEmail(row.getString("email"));
        passenger.setPhone(row.getString("phone"));
        passenger.setPassportNumber(row.getString("passport_number"));
        passenger.setDateOfBirth(row.getLocalDate("date_of_birth"));
        passenger.setCreatedAt(convertSqlDateTimeToOffset(row, "created_at"));
        passenger.setUpdatedAt(convertSqlDateTimeToOffset(row, "updated_at"));
        return passenger;
    }
}
