package com.airline.booking.demo.feature.bookings.repository.impl

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.feature.bookings.repository.entity.Booking
import io.vertx.core.Future
import io.vertx.sqlclient.*
import spock.lang.Specification
import spock.lang.Subject

import java.time.OffsetDateTime
import java.util.function.Consumer

class BookingRepositoryImplSpec extends Specification {

    Pool pool = Mock(Pool)
    SqlConnection connection = Mock(SqlConnection)
    Row row = Mock(Row)

    @Subject
    BookingRepositoryImpl repository = new BookingRepositoryImpl(pool)

    def setup() {
        pool.getConnection() >> Future.succeededFuture(connection)
        connection.close() >> Future.succeededFuture()

        // Default row mapping shared across tests
        row.getLong("id") >> 1L
        row.getString("booking_reference") >> "REF-123"
        row.getLong("passenger_id") >> 10L
        row.getLong("flight_id") >> 20L
        row.getString("seat_number") >> "12A"
        row.getString("status") >> "CONFIRMED"
        row.get(BigDecimal.class, "total_amount") >> new BigDecimal("100.00")

        row.get(_ as Class, _ as String) >> { Class clazz, String col ->
            if (clazz == BigDecimal) return new BigDecimal("100.00")
            return OffsetDateTime.now()
        }
        row.getValue(_ as String) >> OffsetDateTime.now()
    }

    def "findById should return booking when record exists"() {
        given:
        def selectQuery = Mock(PreparedQuery)
        def rs = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_ID) >> selectQuery
        selectQuery.execute(_ as Tuple) >> Future.succeededFuture(rs)

        rs.iterator() >> it
        it.hasNext() >>> [true, false]
        it.next() >> row

        when:
        def result = repository.findById(1L)

        then:
        result.succeeded()
        result.result().id == 1L
    }

    def "findByPassengerId should map multiple rows to a list"() {
        given:
        def selectQuery = Mock(PreparedQuery)
        def rs = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_PASSENGER) >> selectQuery
        selectQuery.execute(_ as Tuple) >> Future.succeededFuture(rs)

        rs.iterator() >> it
        it.hasNext() >>> [true, true, false]
        it.next() >>> [row, row]

        rs.forEach(_ as Consumer) >> { consumer ->
            consumer.accept(row)
            consumer.accept(row)
        }

        when:
        def result = repository.findByPassengerId(connection, 10L)

        then:
        result.succeeded()
        result.result().size() == 2
        result.result()[0].id == 1L
    }

    def "update should execute update and then return updated booking"() {
        given:
        def booking = new Booking(
                id: 99L,
                status: "CANCELLED",
                seatNumber: "22B",
                totalAmount: new BigDecimal("150.00")
        )

        def updateQuery = Mock(PreparedQuery)
        def selectQuery = Mock(PreparedQuery)
        def rs = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(BookingRepositoryImpl.SQL_UPDATE) >> updateQuery
        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_ID) >> selectQuery

        updateQuery.execute(_ as Tuple) >> Future.succeededFuture(Mock(RowSet))

        selectQuery.execute(_ as Tuple) >> Future.succeededFuture(rs)
        rs.iterator() >> it
        it.hasNext() >>> [true, false]
        it.next() >> row

        when:
        def future = repository.save(connection, booking)

        then:
        future.succeeded()
        future.result().id == 1L
        future.result().status == "CONFIRMED"
    }

    def "save should perform insert when ID is null"() {
        given:
        def booking = new Booking(
                bookingReference: "REF-321",
                passengerId: 33L,
                flightId: 44L,
                seatNumber: "14C",
                status: "CONFIRMED",
                totalAmount: new BigDecimal("200.00")
        )

        pool.getConnection() >> Future.succeededFuture(connection)

        def insertPQ = Mock(PreparedQuery)
        def insertRS = Mock(RowSet)

        connection.preparedQuery({ sql ->
            sql?.trim()?.toLowerCase()?.startsWith("insert into bookings")
        } as String) >> insertPQ

        insertPQ.execute(_ as Tuple) >> Future.succeededFuture(insertRS)

        def lastValQuery = Mock(Query)
        def lastValRS = Mock(RowSet)
        def lastValIter = Mock(RowIterator)
        def lastValRow = Mock(Row)

        connection.query({ sql ->
            sql?.toLowerCase()?.contains("lastval")
        } as String) >> lastValQuery

        lastValQuery.execute() >> Future.succeededFuture(lastValRS)

        lastValRS.iterator() >> lastValIter
        lastValIter.hasNext() >> true
        lastValIter.next() >> lastValRow
        lastValRow.getLong("id") >> 777L

        def selectPQ = Mock(PreparedQuery)
        def selectRS = Mock(RowSet)
        def selectIter = Mock(RowIterator)
        def selectRow = Mock(Row)

        connection.preparedQuery({ sql ->
            sql?.toLowerCase()?.contains("from bookings") &&
                    sql?.toLowerCase()?.contains("where id")
        } as String) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(selectRS)
        selectRS.iterator() >> selectIter
        selectIter.hasNext() >>> [true, false]
        selectIter.next() >> selectRow

        selectRow.getLong("id") >> 777L
        selectRow.getString("booking_reference") >> "REF-321"
        selectRow.getLong("passenger_id") >> 33L
        selectRow.getLong("flight_id") >> 44L
        selectRow.getString("seat_number") >> "14C"
        selectRow.getString("status") >> "CONFIRMED"
        selectRow.get(BigDecimal.class, "total_amount") >> new BigDecimal("200.00")
        selectRow.get(_ as Class, _ as String) >> OffsetDateTime.now()
        selectRow.getValue(_ as String) >> OffsetDateTime.now()

        when:
        def future = repository.save(connection, booking)

        then:
        future.succeeded()
        future.result().id == 777L
        future.result().bookingReference == "REF-321"
    }

    def "save(Booking) should get connection, delegate to save(conn, booking), and close connection (update path)"() {
        given:
        def booking = new Booking(
                id: 1L,
                status: "CANCELLED",
                seatNumber: "15A",
                totalAmount: new BigDecimal("150.00")
        )

        pool.getConnection() >> Future.succeededFuture(connection)

        def updatePQ = Mock(PreparedQuery)
        def selectPQ = Mock(PreparedQuery)
        def rs = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(BookingRepositoryImpl.SQL_UPDATE) >> updatePQ
        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_ID) >> selectPQ

        updatePQ.execute(_ as Tuple) >> Future.succeededFuture(Mock(RowSet)) // update OK

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(rs)
        rs.iterator() >> it
        it.hasNext() >>> [true, false]
        it.next() >> row

        when:
        def fut = repository.save(booking)

        then:
        fut.succeeded()
        fut.result().id == 1L

        and: "connection must be closed"
        1 * connection.close()
    }

    def "save(Booking) should get connection, delegate to save(conn, booking), and close connection (insert path)"() {
        given:
        def booking = new Booking(
                bookingReference: "R-555",
                passengerId: 9L,
                flightId: 8L,
                seatNumber: "18A",
                status: "CONFIRMED",
                totalAmount: new BigDecimal("300.00")
        )

        pool.getConnection() >> Future.succeededFuture(connection)

        def insertPQ = Mock(PreparedQuery)
        connection.preparedQuery({ sql ->
            sql?.trim()?.toLowerCase()?.startsWith("insert into bookings")
        } as String) >> insertPQ
        insertPQ.execute(_ as Tuple) >> Future.succeededFuture(Mock(RowSet))

        def lastValQuery = Mock(Query)
        def lastValRS = Mock(RowSet)
        def lastValIter = Mock(RowIterator)
        def lastValRow = Mock(Row)

        connection.query({ sql ->
            sql?.toLowerCase()?.contains("lastval")
        } as String) >> lastValQuery

        lastValQuery.execute() >> Future.succeededFuture(lastValRS)
        lastValRS.iterator() >> lastValIter
        lastValIter.hasNext() >> true
        lastValIter.next() >> lastValRow
        lastValRow.getLong("id") >> 555L

        def selectPQ = Mock(PreparedQuery)
        def selectRS = Mock(RowSet)
        def selectIter = Mock(RowIterator)
        def selectRow = Mock(Row)

        connection.preparedQuery({ sql ->
            sql?.toLowerCase()?.contains("from bookings") && sql?.toLowerCase()?.contains("where id")
        } as String) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(selectRS)
        selectRS.iterator() >> selectIter
        selectIter.hasNext() >>> [true, false]
        selectIter.next() >> selectRow

        selectRow.getLong("id") >> 555L
        selectRow.getString("booking_reference") >> "R-555"
        selectRow.getLong("passenger_id") >> 9L
        selectRow.getLong("flight_id") >> 8L
        selectRow.getString("seat_number") >> "18A"
        selectRow.getString("status") >> "CONFIRMED"
        selectRow.get(BigDecimal.class, "total_amount") >> new BigDecimal("300.00")
        selectRow.get(_ as Class, _ as String) >> OffsetDateTime.now()
        selectRow.getValue(_ as String) >> OffsetDateTime.now()

        when:
        def fut = repository.save(booking)

        then:
        fut.succeeded()
        fut.result().id == 555L

        and: "connection must be closed"
        1 * connection.close()
    }

    def "insert should fail and map error via PgErrorMapper"() {
        given:
        def booking = new Booking(
                bookingReference: "ERR-1",
                passengerId: 99L,
                flightId: 88L,
                seatNumber: "20C",
                status: "CONFIRMED",
                totalAmount: new BigDecimal("400.00")
        )

        pool.getConnection() >> Future.succeededFuture(connection)

        def insertPQ = Mock(PreparedQuery)

        connection.preparedQuery({ sql ->
            sql?.toLowerCase()?.startsWith("insert into bookings")
        } as String) >> insertPQ

        // FORCE FAILURE HERE
        insertPQ.execute(_ as Tuple) >> Future.failedFuture(new RuntimeException("DB insert failure"))

        when:
        def fut = repository.save(connection, booking)

        then:
        fut.failed()

        and: "Error message is mapped"
        fut.cause().message.contains("Failed to insert booking")
    }

    def "findById should fail with ENTITY_NOT_FOUND when no row exists"() {
        given:
        def selectPQ = Mock(PreparedQuery)
        def emptyRS = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_ID) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(emptyRS)

        emptyRS.iterator() >> it
        it.hasNext() >> false

        when:
        def fut = repository.findById(connection, 999L) // nonexistent

        then:
        fut.failed()
        fut.cause() instanceof BusinessServiceException
        fut.cause().message.contains("Booking not found with id: 999")
    }

    def "findById should map error via recover when database execute fails"() {
        given:
        def selectPQ = Mock(PreparedQuery)

        connection.preparedQuery(BookingRepositoryImpl.SQL_SELECT_BY_ID) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.failedFuture(new RuntimeException("DB failure"))

        when:
        def fut = repository.findById(connection, 100L)

        then:
        fut.failed()
        fut.cause().message.contains("Failed to query booking")
    }


}
