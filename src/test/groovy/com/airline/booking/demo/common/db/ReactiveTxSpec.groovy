package com.airline.booking.demo.common.db

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ReactiveTxSpec extends Specification {

    Pool pool = Mock()
    SqlConnection conn = Mock()
    Transaction tx = Mock()

    ReactiveTx reactiveTx = new ReactiveTx(pool)

    private static <T> T await(final Future<T> fut) {
        return fut.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS)
    }

    def "commits on success and returns result"() {
        given: "Successful connection and transaction start"
        pool.getConnection() >> Future.succeededFuture(conn)
        conn.begin() >> Future.succeededFuture(tx)

        tx.commit() >> Future.succeededFuture()
        conn.close() >> Future.succeededFuture()

        when: "Executing work that succeeds"
        def result = await(reactiveTx.withTx { c ->
            assert c == conn
            Future.succeededFuture("ok")
        })

        then: "Transaction is committed and connection closed"
        result == "ok"
        1 * tx.commit() >> Future.succeededFuture()
        1 * conn.close() >> Future.succeededFuture()
        0 * tx.rollback()
    }

    def "propagates BusinessServiceException and triggers rollback"() {
        given: "An existing business exception"
        def bse = new BusinessServiceException(ErrorCodeEnum.DATABASE_ERROR, "business-fail")

        pool.getConnection() >> Future.succeededFuture(conn)
        conn.begin() >> Future.succeededFuture(tx)
        tx.rollback() >> Future.succeededFuture()
        conn.close() >> Future.succeededFuture()

        when: "The work fails with a BusinessServiceException"
        await(reactiveTx.withTx { c -> Future.failedFuture(bse) })

        then: "Exception is thrown, rolled back, and closed"
        def ex = thrown(Exception)
        def cause = ex.cause ?: ex
        cause.is(bse)

        1 * tx.rollback() >> Future.succeededFuture()
        1 * conn.close() >> Future.succeededFuture()
        0 * tx.commit()
    }

    def "wraps unexpected exceptions in BusinessServiceException"() {
        given: "A generic runtime failure"
        pool.getConnection() >> Future.succeededFuture(conn)
        conn.begin() >> Future.succeededFuture(tx)
        tx.rollback() >> Future.succeededFuture()
        conn.close() >> Future.succeededFuture()

        when: "Work fails with a standard RuntimeException"
        await(reactiveTx.withTx { c -> Future.failedFuture(new RuntimeException("low-level-error")) })

        then: "It is wrapped in a DATABASE_ERROR BusinessServiceException"
        def ex = thrown(Exception)
        def cause = ex.cause ?: ex
        cause instanceof BusinessServiceException
        cause.errorEnum == ErrorCodeEnum.DATABASE_ERROR
        cause.message.contains("low-level-error")

        1 * tx.rollback() >> Future.succeededFuture()
        1 * conn.close() >> Future.succeededFuture()
    }

    def "handles commit failure as a database error"() {
        given: "Work succeeds but commit fails"
        pool.getConnection() >> Future.succeededFuture(conn)
        conn.begin() >> Future.succeededFuture(tx)
        tx.commit() >> Future.failedFuture(new RuntimeException("commit-failed"))
        conn.close() >> Future.succeededFuture()

        when:
        await(reactiveTx.withTx { c -> Future.succeededFuture("ok") })

        then: "The commit error is caught and wrapped"
        def ex = thrown(Exception)
        def cause = ex.cause ?: ex
        cause instanceof BusinessServiceException
        1 * conn.close() >> Future.succeededFuture()
    }

    def "ensures connection is closed even if rollback fails"() {
        given: "Work fails and then rollback itself fails"
        pool.getConnection() >> Future.succeededFuture(conn)
        conn.begin() >> Future.succeededFuture(tx)
        tx.rollback() >> Future.failedFuture(new RuntimeException("rollback-failed"))
        conn.close() >> Future.succeededFuture()

        when:
        await(reactiveTx.withTx { c -> Future.failedFuture(new RuntimeException("work-failed")) })

        then: "Original work failure is preserved and connection is still closed"
        def ex = thrown(Exception)
        1 * conn.close() >> Future.succeededFuture()
    }
}
