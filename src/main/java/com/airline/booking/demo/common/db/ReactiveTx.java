package com.airline.booking.demo.common.db;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReactiveTx {

    private static final Logger log = LoggerFactory.getLogger(ReactiveTx.class);

    private final Pool pool;

    @Inject
    public ReactiveTx(final Pool pool) {
        this.pool = pool;
    }

    public <T> Future<T> withTx(final Function<SqlConnection, Future<T>> work) {
        return pool
                .getConnection()
                .compose(conn ->
                        conn.begin()
                                .compose(tx -> runInTx(conn, tx, work))
                                .onComplete(ar -> conn.close())
                )
                .recover(err -> {
                    if (err instanceof BusinessServiceException) {
                        return Future.failedFuture(err);
                    }
                    log.error("Transaction failed", err);
                    return Future.failedFuture(new BusinessServiceException(
                            ErrorCodeEnum.DATABASE_ERROR,
                            "Transaction failed: " + err.getMessage()
                    ));
                });
    }

    private <T> Future<T> runInTx(
            final SqlConnection conn,
            final Transaction tx,
            final Function<SqlConnection, Future<T>> work
    ) {
        return work.apply(conn)
                .compose(result ->
                        tx.commit().map(v -> result)
                )
                .recover(err ->
                        tx.rollback()
                                .onFailure(rollbackErr -> log.warn("Rollback failed", rollbackErr))
                                .transform(v -> Future.failedFuture(err))
                );
    }
}
