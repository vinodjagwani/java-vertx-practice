package com.airline.booking.demo.feature.passengers.repository.impl

import com.airline.booking.demo.feature.passengers.repository.entity.Passenger
import io.vertx.core.Future
import io.vertx.sqlclient.*
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate
import java.time.OffsetDateTime

class PassengerRepositoryImplSpec extends Specification {

    Pool pool = Mock(Pool)
    SqlConnection connection = Mock(SqlConnection)
    PreparedQuery<RowSet<Row>> preparedQuery = Mock(PreparedQuery)
    RowSet<Row> rowSet = Mock(RowSet)
    RowIterator<Row> rowIterator = Mock(RowIterator)
    Row row = Mock(Row)

    @Subject
    PassengerRepositoryImpl repository = new PassengerRepositoryImpl(pool)

    def setup() {
        pool.preparedQuery(_ as String) >> preparedQuery
        pool.getConnection() >> Future.succeededFuture(connection)

        connection.preparedQuery(_ as String) >> preparedQuery
        connection.close() >> Future.succeededFuture()

        preparedQuery.execute(_ as Tuple) >> Future.succeededFuture(rowSet)

        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        row.getLong("id") >> 100L
        row.getString("first_name") >> "John"
        row.getString("last_name") >> "Doe"
        row.getString("email") >> "john@example.com"
        row.getString("phone") >> "12345"
        row.getString("passport_number") >> "A123456"
        row.getLocalDate("date_of_birth") >> LocalDate.of(1990, 1, 1)

        row.getValue(_ as String) >> OffsetDateTime.now()
    }

    def "findById should return passenger when record exists"() {
        when:
        def result = repository.findById(100L)

        then:
        result.succeeded()
        result.result().id == 100L
        result.result().email == "john@example.com"
    }

    def "save should perform insert and return generated passenger when ID is null"() {
        given: "new passenger"
        Passenger p = new Passenger(firstName: "New")

        connection.query(_ as String) >> Mock(Query) {
            execute() >> Future.succeededFuture(rowSet)
        }

        when:
        def result = repository.save(p)

        then:
        result.succeeded()
        result.result().id == 100L
        1 * connection.close() >> Future.succeededFuture()
    }

    def "save should perform update when ID is present"() {
        given:
        Passenger p = new Passenger(id: 100L, firstName: "Update")

        when:
        def result = repository.save(p)

        then:
        result.succeeded()
        result.result().id == 100L
        1 * connection.close() >> Future.succeededFuture()
    }
}
