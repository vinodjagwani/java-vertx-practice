package com.airline.booking.demo.feature.airlines.repository.impl;

import static com.airline.booking.demo.common.utils.AirlineBookingUtil.convertSqlDateTimeToOffset;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.PgErrorMapper;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository;
import com.airline.booking.demo.feature.airlines.repository.entity.Airline;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.stream.StreamSupport;

@Singleton
public class AirlineRepositoryImpl implements AirlineRepository {

    private static final String SQL_INSERT = """
                INSERT INTO airlines (code, name, country) VALUES (?, ?, ?)
            """;


    private static final String SQL_SELECT_BY_ID = """
                SELECT id, code, name, country, created_at, updated_at
                FROM airlines WHERE id = ?
            """;

    private static final String SQL_SELECT_BY_CODE = """
                SELECT id, code, name, country, created_at, updated_at
                FROM airlines WHERE code = ?
            """;

    private static final String SQL_SELECT_ALL = """
                SELECT id, code, name, country, created_at, updated_at
                FROM airlines ORDER BY name ASC
            """;

    private static final String SQL_UPDATE = """
                UPDATE airlines SET
                    name = ?, country = ?, updated_at = CURRENT_TIMESTAMP
                WHERE code = ?
            """;

    private final Pool pool;

    @Inject
    public AirlineRepositoryImpl(final Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Airline> findById(final Long id) {
        return pool.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(new BusinessServiceException(
                                ErrorCodeEnum.ENTITY_NOT_FOUND, "Airline not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query airline")));
    }

    @Override
    public Future<Airline> findByCode(final String code) {
        return pool.preparedQuery(SQL_SELECT_BY_CODE)
                .execute(Tuple.of(code))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(new BusinessServiceException(
                                ErrorCodeEnum.ENTITY_NOT_FOUND, "Airline not found with code: " + code));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                })
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query airline")));
    }

    @Override
    public Future<Airline> findByCodeOptional(final String code) {
        return pool.preparedQuery(SQL_SELECT_BY_CODE)
                .execute(Tuple.of(code))
                .map(rows -> rows.iterator().hasNext() ? map(rows.iterator().next()) : null)
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to query airline")));
    }

    @Override
    public Future<List<Airline>> findAll() {
        return pool.query(SQL_SELECT_ALL).execute()
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(this::map)
                        .toList())
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to fetch airlines")));
    }

    @Override
    public Future<Airline> save(final Airline airline) {
        return pool.getConnection()
                .compose(conn -> save(conn, airline).onComplete(ar -> conn.close()));
    }

    @Override
    public Future<Airline> save(final SqlConnection conn, final Airline airline) {
        return airline.getId() == null ? insert(conn, airline) : update(conn, airline);
    }

    private Future<Airline> insert(final SqlConnection conn, final Airline airline) {
        return conn
                .preparedQuery(SQL_INSERT)
                .execute(Tuple.of(airline.getCode(), airline.getName(), airline.getCountry()))
                .compose(v -> conn
                        .query("SELECT lastval() AS id")
                        .execute()
                )
                .compose(rows -> {
                    final Long id = rows.iterator().next().getLong("id");
                    return findById(conn, id);
                })
                .recover(err -> Future.failedFuture(
                        PgErrorMapper.map(err, "Failed to insert airline")));
    }

    private Future<Airline> findById(final SqlConnection conn, final Long id) {
        return conn.preparedQuery(SQL_SELECT_BY_ID)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(
                                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND,
                                        "Airline not found with id: " + id));
                    }
                    return Future.succeededFuture(map(rows.iterator().next()));
                });
    }


    private Future<Airline> update(final SqlClient client, final Airline airline) {
        return client.preparedQuery(SQL_UPDATE)
                .execute(Tuple.of(airline.getName(), airline.getCountry(), airline.getCode()))
                .compose(v -> findByCode(airline.getCode()))
                .recover(err -> Future.failedFuture(PgErrorMapper.map(err, "Failed to update airline")));
    }

    private Airline map(final Row row) {
        final Airline airline = new Airline();
        airline.setId(row.getLong("id"));
        airline.setCode(row.getString("code"));
        airline.setName(row.getString("name"));
        airline.setCountry(row.getString("country"));
        airline.setCreatedAt(convertSqlDateTimeToOffset(row, "created_at"));
        airline.setUpdatedAt(convertSqlDateTimeToOffset(row, "updated_at"));
        return airline;
    }
}
