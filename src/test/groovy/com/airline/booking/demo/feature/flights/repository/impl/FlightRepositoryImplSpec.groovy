package com.airline.booking.demo.feature.flights.repository.impl

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.feature.flights.repository.entity.Flight
import io.vertx.core.Future
import io.vertx.sqlclient.*
import spock.lang.Specification
import spock.lang.Subject

import java.time.OffsetDateTime

class FlightRepositoryImplSpec extends Specification {

    Pool pool = Mock(Pool)
    SqlConnection connection = Mock(SqlConnection)

    PreparedQuery<RowSet<Row>> preparedQuery = Mock(PreparedQuery)
    RowSet<Row> rowSet = Mock(RowSet)
    RowIterator<Row> rowIterator = Mock(RowIterator)
    Row row = Mock(Row)

    @Subject
    FlightRepositoryImpl repository = new FlightRepositoryImpl(pool)

    def setup() {
        pool.preparedQuery(_ as String) >> preparedQuery
        pool.getConnection() >> Future.succeededFuture(connection)

        connection.close() >> Future.succeededFuture()

        preparedQuery.execute(_ as Tuple) >> Future.succeededFuture(rowSet)

        row.getLong("id") >> 100L
        row.getString("flight_number") >> "VN123"
        row.getLong("airline_id") >> 1L
        row.getString("departure_airport") >> "SGN"
        row.getString("arrival_airport") >> "BKK"
        row.getInteger("available_seats") >> 50
        row.getInteger("total_seats") >> 200
        row.getString("status") >> "SCHEDULED"

        row.get(BigDecimal.class, "price") >> new BigDecimal("250.00")
        row.get(_ as Class, _ as String) >> { Class clazz, String col ->
            if (clazz == BigDecimal) return new BigDecimal("250.00")
            return OffsetDateTime.now()
        }
        row.getValue(_ as String) >> OffsetDateTime.now()
    }

    def "findById should return flight when record exists"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.findById(100L)

        then:
        result.succeeded()
        result.result().id == 100L
        result.result().flightNumber == "VN123"
    }

    def "findByRoute should map multiple rows using forEach"() {
        given: "A RowSet that triggers the consumer twice"
        rowSet.forEach(_) >> { args ->
            def consumer = args[0]
            consumer.accept(row)
            consumer.accept(row)
        }

        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, true, false]
        rowIterator.next() >> row

        when: "Finding by route"
        def result = repository.findByRoute("SGN", "BKK")

        then: "The future succeeds and contains a list of 2 flights"
        result.succeeded()
        result.result().size() == 2
        result.result()[0].departureAirport == "SGN"
    }

    def "findByIdForUpdate should succeed when called with a connection"() {
        given:
        connection.preparedQuery(_ as String) >> preparedQuery

        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.findByIdForUpdate(connection, 100L)

        then:
        result.succeeded()
        result.result().id == 100L
    }

    def "save should perform update when ID is present"() {
        given: "An existing flight object"
        Flight flight = new Flight(id: 100L, flightNumber: "VN123")

        connection.preparedQuery(_ as String) >> preparedQuery

        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.save(flight)

        then:
        result.succeeded()
        result.result().id == 100L
    }

    def "save should perform insert when ID is null"() {
        given:
        Flight flight = new Flight(
                flightNumber: "VN123",
                airlineId: 1L,
                departureAirport: "SGN",
                arrivalAirport: "BKK",
                availableSeats: 50,
                totalSeats: 200,
                price: new BigDecimal("250.00"),
                status: "SCHEDULED"
        )

        pool.getConnection() >> Future.succeededFuture(connection)

        def insertPQ = Mock(PreparedQuery)
        def insertRS = Mock(RowSet)

        connection.preparedQuery({ String sql ->
            sql?.toLowerCase()?.startsWith("insert into flights")
        } as String) >> insertPQ

        insertPQ.execute(_ as Tuple) >> Future.succeededFuture(insertRS)

        def lastValQuery = Mock(Query)
        def lastValRS = Mock(RowSet)
        def lastValIter = Mock(RowIterator)
        def lastValRow = Mock(Row)

        connection.query(_ as String) >> { String sql ->
            assert sql.toLowerCase().contains("lastval")
            return lastValQuery
        }

        lastValQuery.execute() >> Future.succeededFuture(lastValRS)

        lastValRS.iterator() >> lastValIter
        lastValIter.hasNext() >> true
        lastValIter.next() >> lastValRow
        lastValRow.getLong("id") >> 777L

        def selectPQ = Mock(PreparedQuery)
        def selectRS = Mock(RowSet)
        def selectIter = Mock(RowIterator)
        def selectRow = Mock(Row)

        connection.preparedQuery({ String sql ->
            sql?.toLowerCase()?.contains("from flights") && sql?.toLowerCase()?.contains("where id")
        } as String) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(selectRS)

        selectRS.iterator() >> selectIter
        selectIter.hasNext() >>> [true, false]
        selectIter.next() >> selectRow

        selectRow.getLong("id") >> 777L
        selectRow.getString("flight_number") >> "VN123"
        selectRow.getLong("airline_id") >> 1L
        selectRow.getString("departure_airport") >> "SGN"
        selectRow.getString("arrival_airport") >> "BKK"
        selectRow.getInteger("available_seats") >> 50
        selectRow.getInteger("total_seats") >> 200
        selectRow.getString("status") >> "SCHEDULED"
        selectRow.get(BigDecimal.class, "price") >> new BigDecimal("250.00")
        selectRow.get(_ as Class, _ as String) >> { Class clazz, String col ->
            if (clazz == BigDecimal) return new BigDecimal("250.00")
            return OffsetDateTime.now()
        }
        selectRow.getValue(_ as String) >> OffsetDateTime.now()

        when:
        def result = repository.save(flight)

        then:
        result.succeeded()
        result.result().id == 777L
        result.result().flightNumber == "VN123"
    }


    def "findById should map DB errors via recover()"() {
        given:
        def selectPQ = Mock(PreparedQuery)

        pool.preparedQuery(FlightRepositoryImpl.SQL_SELECT_BY_ID) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.failedFuture(new RuntimeException("DB failure"))

        when:
        def fut = repository.findById(100L)

        then:
        fut.failed()
        fut.cause().message.contains("Failed to query flight")
    }

    def "findByIdForUpdate should fail with ENTITY_NOT_FOUND when no flight exists"() {
        given:
        def selectPQ = Mock(PreparedQuery)
        def emptyRS = Mock(RowSet)
        def it = Mock(RowIterator)

        connection.preparedQuery(FlightRepositoryImpl.SQL_SELECT_FOR_UPDATE) >> selectPQ

        selectPQ.execute(_ as Tuple) >> Future.succeededFuture(emptyRS)

        emptyRS.iterator() >> it
        it.hasNext() >> false

        when:
        def fut = repository.findByIdForUpdate(connection, 999L)

        then:
        fut.failed()
        fut.cause() instanceof BusinessServiceException
        fut.cause().message.contains("Flight not found with id: 999")
    }

}
